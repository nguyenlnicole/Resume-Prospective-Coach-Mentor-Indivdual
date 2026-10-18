package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class ResumeUploadService {
  private static final int MAX_BYTES = 10 * 1024 * 1024;
  private final PdfService pdf;
  private final AiService ai;

  ResumeUploadService(PdfService pdf, AiService ai) {
    this.pdf = pdf;
    this.ai = ai;
  }

  Map<String, Object> preview(String name, byte[] bytes) {
    String type = type(name, bytes);
    if (type.equals("pdf")) {
      var pages = pdf.uploadPreview(bytes);
      return Map.of("pageCount", pages.size(), "pages", pages, "previewType", "pages");
    }
    if (type.equals("docx") || type.equals("doc")) {
      String text = wordText(type, bytes);
      return Map.of("pageCount", 0, "pages", List.of(), "previewType", "text",
          "textPreview", text.substring(0, Math.min(text.length(), 15_000)));
    }
    String image = imageData(bytes);
    return Map.of("pageCount", 1, "pages", List.of(image), "previewType", "pages");
  }

  PdfService.Extraction extract(String name, byte[] bytes) {
    String type = type(name, bytes);
    if (type.equals("pdf")) {
      try {
        return pdf.extract(bytes);
      } catch (ResponseStatusException e) {
        if (!e.getStatusCode().equals(UNPROCESSABLE_ENTITY)) throw e;
        var pages = pdf.uploadPreview(bytes);
        String text = ai.readResumeImages(pages);
        if (text.length() < 50)
          throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
              "This scanned PDF does not contain enough readable resume text.");
        return new PdfService.Extraction(text, pages.size(), List.of(), "inline",
            List.of(), List.of(), List.of(), pages.subList(0, Math.min(2, pages.size())));
      }
    }
    if (type.equals("docx") || type.equals("doc"))
      return new PdfService.Extraction(wordText(type, bytes), 0, List.of(), "inline",
          type.equals("docx") ? docxLinks(bytes) : List.of(), List.of(), List.of(), List.of());
    String image = imageData(bytes);
    String text = ai.readResumeImage(image);
    if (text.length() < 50)
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
          "This image does not contain enough readable resume text. Try a sharper image or a PDF.");
    return new PdfService.Extraction(text, 1, List.of(), "inline",
        List.of(), List.of(), List.of(), List.of(image));
  }

  private static String type(String name, byte[] bytes) {
    if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES)
      throw new ResponseStatusException(BAD_REQUEST, "Choose a resume file of 10 MB or less.");
    String lower = Objects.toString(name, "").toLowerCase(Locale.ROOT);
    if (lower.endsWith(".pdf") && starts(bytes, "%PDF-")) return "pdf";
    if (lower.endsWith(".docx") && starts(bytes, "PK\u0003\u0004")) return "docx";
    if (lower.endsWith(".doc") && starts(bytes, "\u00d0\u00cf\u0011\u00e0")) return "doc";
    if (lower.endsWith(".png") && starts(bytes, "\u0089PNG\r\n\u001a\n")) return "png";
    if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        && bytes.length > 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216)
      return "jpg";
    throw new ResponseStatusException(BAD_REQUEST,
        "Upload a PDF, Word document (.docx or .doc), PNG, or JPG resume.");
  }

  private static boolean starts(byte[] bytes, String marker) {
    byte[] prefix = marker.getBytes(StandardCharsets.ISO_8859_1);
    return bytes.length >= prefix.length && Arrays.equals(Arrays.copyOf(bytes, prefix.length), prefix);
  }

  private static String wordText(String type, byte[] bytes) {
    try {
      String text;
      if (type.equals("docx")) {
        try (var document = new XWPFDocument(new ByteArrayInputStream(bytes));
             var extractor = new XWPFWordExtractor(document)) {
          extractor.setFetchHyperlinks(true);
          text = extractor.getText();
        }
      } else {
        try (var document = new HWPFDocument(new ByteArrayInputStream(bytes));
             var extractor = new WordExtractor(document)) {
          text = extractor.getText();
        }
      }
      text = text.replace('\u0000', ' ').strip();
      if (text.length() < 50)
        throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
            "This Word document does not contain enough readable resume text.");
      if (text.length() > 40_000)
        throw new ResponseStatusException(BAD_REQUEST, "Please use a shorter resume document.");
      return text;
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(BAD_REQUEST,
          "This Word document could not be read. Save it again or upload a PDF.");
    }
  }

  private static List<Map<String, Object>> docxLinks(byte[] bytes) {
    try (var document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
      List<Map<String, Object>> links = new ArrayList<>();
      collectDocxLinks(document, document.getBodyElements(), links);
      return links;
    } catch (Exception e) {
      return List.of();
    }
  }

  private static void collectDocxLinks(XWPFDocument document, List<IBodyElement> elements,
      List<Map<String, Object>> links) {
    for (IBodyElement element : elements) {
      if (element instanceof XWPFParagraph paragraph) {
        String context = Objects.toString(paragraph.getText(), "");
        for (XWPFRun run : paragraph.getRuns()) {
          if (!(run instanceof XWPFHyperlinkRun hyperlinkRun)) continue;
          XWPFHyperlink hyperlink = hyperlinkRun.getHyperlink(document);
          if (hyperlink == null || !PdfService.safeUrl(hyperlink.getURL())) continue;
          links.add(Map.of("label", Objects.toString(hyperlinkRun.text(), ""), "context", context,
              "url", hyperlink.getURL()));
        }
      } else if (element instanceof XWPFTable table) {
        for (XWPFTableRow row : table.getRows())
          for (XWPFTableCell cell : row.getTableCells())
            collectDocxLinks(document, cell.getBodyElements(), links);
      }
    }
  }

  private static String imageData(byte[] bytes) {
    try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      if (stream == null)
        throw new ResponseStatusException(BAD_REQUEST, "This resume image could not be read.");
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext())
        throw new ResponseStatusException(BAD_REQUEST, "This resume image could not be read.");
      ImageReader reader = readers.next();
      try {
        reader.setInput(stream);
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width < 300 || height < 300 || width > 6000 || height > 6000
            || (long) width * height > 20_000_000)
          throw new ResponseStatusException(BAD_REQUEST,
              "Choose a clear resume image between 300 and 6000 pixels per side.");
        BufferedImage source = reader.read(0);
        double scale = Math.min(1.0, 2000.0 / Math.max(width, height));
        int outWidth = Math.max(1, (int) Math.round(width * scale));
        int outHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage output = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
          graphics.setColor(Color.WHITE);
          graphics.fillRect(0, 0, outWidth, outHeight);
          graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
              RenderingHints.VALUE_INTERPOLATION_BICUBIC);
          graphics.drawImage(source, 0, 0, outWidth, outHeight, null);
        } finally {
          graphics.dispose();
          source.flush();
        }
        try (var out = new ByteArrayOutputStream()) {
          ImageIO.write(output, "png", out);
          return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } finally {
          output.flush();
        }
      } finally {
        reader.dispose();
      }
    } catch (ResponseStatusException e) {
      throw e;
    } catch (IOException | IllegalArgumentException e) {
      throw new ResponseStatusException(BAD_REQUEST, "This resume image could not be read.");
    }
  }
}
