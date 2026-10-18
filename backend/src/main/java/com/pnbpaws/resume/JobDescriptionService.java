package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class JobDescriptionService {
  private final AiService ai;
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(8))
      .followRedirects(HttpClient.Redirect.NEVER).build();

  JobDescriptionService(AiService ai) {
    this.ai = ai;
  }

  String fromFile(String name, byte[] bytes) {
    if (bytes.length == 0 || bytes.length > 5_000_000)
      throw new ResponseStatusException(BAD_REQUEST, "Choose a job posting file under 5 MB.");
    String lower = Objects.toString(name, "").toLowerCase(Locale.ROOT);
    try {
      String text;
      if (lower.endsWith(".pdf") && starts(bytes, "%PDF-")) {
        try (var document = Loader.loadPDF(bytes)) {
          if (document.isEncrypted() || document.getNumberOfPages() == 0 || document.getNumberOfPages() > 5)
            throw new ResponseStatusException(BAD_REQUEST, "Choose an unlocked job posting PDF of five pages or fewer.");
          text = new PDFTextStripper().getText(document).strip();
          if (text.isBlank()) {
            var renderer = new PDFRenderer(document);
            StringJoiner pages = new StringJoiner("\n");
            for (int i = 0; i < Math.min(document.getNumberOfPages(), 3); i++) {
              BufferedImage page = renderer.renderImageWithDPI(i, 120);
              var out = new ByteArrayOutputStream();
              ImageIO.write(page, "png", out);
              pages.add(ai.readJobImage(dataUrl("image/png", out.toByteArray())));
            }
            text = pages.toString();
          }
        }
      } else if (lower.endsWith(".docx") && starts(bytes, "PK\u0003\u0004")) {
        try (var document = new XWPFDocument(new ByteArrayInputStream(bytes));
             var extractor = new XWPFWordExtractor(document)) {
          text = extractor.getText();
        }
      } else if (lower.endsWith(".doc") && starts(bytes, "\u00d0\u00cf\u0011\u00e0")) {
        try (var document = new HWPFDocument(new ByteArrayInputStream(bytes));
             var extractor = new WordExtractor(document)) {
          text = extractor.getText();
        }
      } else if ((lower.endsWith(".png") && starts(bytes, "\u0089PNG")) ||
          (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) && bytes.length > 3 &&
              (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216) {
        String mime = lower.endsWith(".png") ? "image/png" : "image/jpeg";
        if (ImageIO.read(new ByteArrayInputStream(bytes)) == null)
          throw new ResponseStatusException(BAD_REQUEST, "Choose a readable screenshot.");
        text = ai.readJobImage(dataUrl(mime, bytes));
      } else {
        throw new ResponseStatusException(BAD_REQUEST, "Upload a PDF, Word document, PNG, or JPG job listing.");
      }
      return clean(text);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(BAD_REQUEST, "This job posting could not be read. Paste its text instead.");
    }
  }

  String fromUrl(String value) {
    try {
      URI uri = URI.create(Objects.toString(value, "").strip());
      if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null ||
          uri.getHost() == null || uri.getPort() != -1)
        throw new ResponseStatusException(BAD_REQUEST, "Use a public HTTPS job posting link.");
      String host = uri.getHost();
      if (host.equalsIgnoreCase("localhost") || host.endsWith(".local") || host.endsWith(".internal"))
        throw new ResponseStatusException(BAD_REQUEST, "Use a public job posting link.");
      for (InetAddress address : InetAddress.getAllByName(host))
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() ||
            address.isSiteLocalAddress() || address.isLinkLocalAddress() ||
            address.isMulticastAddress() || !publicAddress(address))
          throw new ResponseStatusException(BAD_REQUEST, "Use a public job posting link.");
      HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12))
          .header("User-Agent", "Mozilla/5.0 (compatible; ResumeProspective/1.0)")
          .GET().build();
      HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream body = response.body()) {
        if (response.statusCode() != 200)
          throw new ResponseStatusException(BAD_REQUEST, "That link could not be opened. Paste the description or upload a screenshot.");
        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        if (!contentType.contains("text/html") && !contentType.contains("text/plain"))
          throw new ResponseStatusException(BAD_REQUEST, "That link is not a text page. Upload its PDF or screenshot.");
        byte[] bytes = body.readNBytes(500_001);
        if (bytes.length > 500_000)
          throw new ResponseStatusException(BAD_REQUEST, "That page is too large. Paste the description or upload a screenshot.");
        String page = new String(bytes, StandardCharsets.UTF_8);
        String metadata = host.toLowerCase(Locale.ROOT).endsWith(".myworkdayjobs.com") ? jobMetadata(page) : "";
        if (!metadata.isBlank()) return clean(metadata);
        page = page.replaceAll("(?is)<(script|style|nav|footer|header)[^>]*>.*?</\\1>", " ")
            .replaceAll("(?i)</(p|div|li|h[1-6]|br|section)>", "\n")
            .replaceAll("<[^>]+>", " ");
        page = org.springframework.web.util.HtmlUtils.htmlUnescape(page);
        return clean(page);
      }
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(BAD_REQUEST, "That link could not be read. Paste the description or upload a screenshot.");
    }
  }

  static String jobMetadata(String page) {
    String title = "", description = "";
    var tags = java.util.regex.Pattern.compile("(?is)<meta\\s+[^>]+>").matcher(page);
    while (tags.find()) {
      Map<String, String> attributes = new HashMap<>();
      var values = java.util.regex.Pattern.compile("(?is)([\\w:-]+)\\s*=\\s*([\"'])(.*?)\\2").matcher(tags.group());
      while (values.find()) attributes.put(values.group(1).toLowerCase(Locale.ROOT),
          org.springframework.web.util.HtmlUtils.htmlUnescape(values.group(3)));
      String property = attributes.getOrDefault("property", "");
      if (property.equalsIgnoreCase("og:title")) title = attributes.getOrDefault("content", "");
      if (property.equalsIgnoreCase("og:description")) description = attributes.getOrDefault("content", "");
    }

    if (description.length() < 500) return "";
    return (title + "\n\n" + description).strip();
  }

  private static boolean publicAddress(InetAddress address) {
    byte[] a = address.getAddress();
    if (a.length == 4) {
      int first = a[0] & 255, second = a[1] & 255;
      return first != 0 && first != 10 && first != 127 && first < 224 &&
          !(first == 100 && second >= 64 && second <= 127) &&
          !(first == 169 && second == 254) && !(first == 172 && second >= 16 && second <= 31) &&
          !(first == 192 && second == 168) && !(first == 192 && second == 0) &&
          !(first == 198 && (second == 18 || second == 19));
    }
    return !address.isSiteLocalAddress() && !address.isLinkLocalAddress() &&
        !address.isLoopbackAddress() && !address.isAnyLocalAddress() &&
        (a[0] & 255) != 0xfc && (a[0] & 255) != 0xfd;
  }

  private static boolean starts(byte[] bytes, String marker) {
    byte[] prefix = marker.getBytes(StandardCharsets.ISO_8859_1);
    return bytes.length >= prefix.length && Arrays.equals(Arrays.copyOf(bytes, prefix.length), prefix);
  }

  private static String dataUrl(String mime, byte[] bytes) {
    return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
  }

  private static String clean(String text) {
    String value = Objects.toString(text, "").replace('\u0000', ' ').strip();
    if (value.isBlank())
      throw new ResponseStatusException(BAD_REQUEST, "No readable job description was found. Paste the text instead.");
    return value.substring(0, Math.min(value.length(), 12_000));
  }
}
