package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.apache.pdfbox.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.text.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class PdfService {
  private final String regularPath, boldPath;

  @Value("${app.aptos-font-directory:.fonts}")
  String aptosDirectory = ".fonts";

  static final List<String> FONTS =
      List.of("Calibri", "Arial", "Aptos", "Cambria", "Georgia", "Times New Roman");

  PdfService(@Value("${app.pdf-font}") String regular, @Value("${app.pdf-bold-font}") String bold) {
    regularPath = regular;
    boldPath = bold;
  }

  record Extraction(
      String text,
      int pages,
      List<Map<String, Object>> typography,
      String datePlacement,
      List<Map<String, Object>> links,
      List<Map<String, Object>> lines,
      List<Map<String, Object>> pageSizes,
      List<String> pageImages) {}

  List<String> uploadPreview(byte[] bytes) {
    if (bytes.length < 5 || bytes.length > 10 * 1024 * 1024
        || !new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
      throw new ResponseStatusException(BAD_REQUEST, "Please upload a valid PDF of 10 MB or less.");
    try (var doc = Loader.loadPDF(bytes)) {
      if (doc.isEncrypted())
        throw new ResponseStatusException(BAD_REQUEST, "Please upload an unlocked PDF.");
      if (doc.getNumberOfPages() < 1 || doc.getNumberOfPages() > 10)
        throw new ResponseStatusException(BAD_REQUEST, "Please upload a resume of 1 to 10 pages.");
      for (var page : doc.getPages()) {
        var box = page.getCropBox();
        if (!Float.isFinite(box.getWidth()) || !Float.isFinite(box.getHeight())
            || box.getWidth() < 100 || box.getHeight() < 100
            || box.getWidth() > 1500 || box.getHeight() > 1500)
          throw new ResponseStatusException(BAD_REQUEST, "This PDF has an unsupported page size.");
      }
      var renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
      renderer.setSubsamplingAllowed(true);
      List<String> images = new ArrayList<>();
      for (int index = 0; index < doc.getNumberOfPages(); index++) {
        var box = doc.getPage(index).getCropBox();
        float scale = 1100f / Math.max(box.getWidth(), box.getHeight());
        var image = renderer.renderImage(index, scale);
        try (var out = new ByteArrayOutputStream()) {
          javax.imageio.ImageIO.write(image, "png", out);
          images.add("data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray()));
        } finally {
          image.flush();
        }
      }
      return images;
    } catch (ResponseStatusException e) {
      throw e;
    } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
      throw new ResponseStatusException(BAD_REQUEST, "Please upload an unlocked PDF.");
    } catch (IOException | IllegalArgumentException e) {
      throw new ResponseStatusException(BAD_REQUEST, "This PDF could not be previewed. Please choose another PDF.");
    }
  }

  Extraction extract(byte[] bytes) {
    if (bytes.length < 5
        || !new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
      throw new ResponseStatusException(BAD_REQUEST, "Please upload a valid PDF.");
    try (var doc = Loader.loadPDF(bytes)) {
      if (doc.isEncrypted())
        throw new ResponseStatusException(BAD_REQUEST, "Please upload an unlocked PDF.");
      if (doc.getNumberOfPages() > 10)
        throw new ResponseStatusException(
            BAD_REQUEST, "Please upload a resume of 10 pages or fewer.");
      var info = new LinkedHashMap<String, Integer>();
      List<Map<String, Object>> lines = new ArrayList<>();
      List<Map<String, Object>> pageSizes = new ArrayList<>();
      for (int index = 0; index < doc.getNumberOfPages(); index++) {
        var box = doc.getPage(index).getCropBox();
        if (box.getWidth() > 1500
            || box.getHeight() > 1500
            || box.getWidth() < 100
            || box.getHeight() < 100)
          throw new ResponseStatusException(
              BAD_REQUEST, "Please export the resume at a standard page size.");
        pageSizes.add(
            Map.of(
                "page",
                index + 1,
                "widthPt",
                box.getWidth(),
                "heightPt",
                box.getHeight(),
                "rotation",
                doc.getPage(index).getRotation()));
      }
      int[] dateLayouts = new int[2];
      PDFTextStripper stripper =
          new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition t) {
              String key =
                  t.getFont().getName() + "|" + Math.round(t.getFontSizeInPt() * 10) / 10.0;
              info.merge(key, 1, Integer::sum);
              super.processTextPosition(t);
            }

            @Override
            protected void writeString(String line, List<TextPosition> positions)
                throws IOException {
              if (!positions.isEmpty() && !line.isBlank()) {
                if (lines.size() >= 1500)
                  throw new ResponseStatusException(
                      BAD_REQUEST,
                      "This PDF has too many text fragments. Please re-export a simpler resume.");
                float left = Float.MAX_VALUE, right = 0, top = Float.MAX_VALUE, baseline = 0;
                List<Map<String, Object>> runs = new ArrayList<>();
                String previous = "";
                for (TextPosition position : positions) {
                  left = Math.min(left, position.getXDirAdj());
                  right = Math.max(right, position.getXDirAdj() + position.getWidthDirAdj());
                  top = Math.min(top, position.getYDirAdj() - position.getHeightDir());
                  baseline = Math.max(baseline, position.getYDirAdj());
                  String key = position.getFont().getName() + "|" + position.getFontSizeInPt();
                  if (!key.equals(previous)) {
                    runs.add(
                        Map.of(
                            "font",
                            position.getFont().getName(),
                            "sizePt",
                            position.getFontSizeInPt()));
                    previous = key;
                  }
                }
                lines.add(
                    Map.of(
                        "id",
                        lines.size() + 1,
                        "page",
                        getCurrentPageNo(),
                        "text",
                        line,
                        "leftPt",
                        left,
                        "rightPt",
                        right,
                        "topPt",
                        top,
                        "baselinePt",
                        baseline,
                        "fonts",
                        runs));
              }
              if (!line.strip().startsWith("•")
                  && !line.contains("http")
                  && line.matches(
                      "(?i).*\\b(?:19|20)\\d{2}(?:\\s*[-–—]\\s*(?:(?:[A-Za-z]+\\s+)?(?:19|20)\\d{2}|Present|Current))?\\s*$")
                  && !positions.isEmpty()) {
                float pageWidth = doc.getPage(getCurrentPageNo() - 1).getMediaBox().getWidth();
                boolean right = positions.get(0).getXDirAdj() > pageWidth * .6f;
                for (int i = 1; i < positions.size(); i++) {
                  var before = positions.get(i - 1);
                  var next = positions.get(i);
                  if (next.getXDirAdj() > pageWidth * .6f
                      && next.getXDirAdj() - before.getXDirAdj() - before.getWidthDirAdj() > 18)
                    right = true;
                }
                dateLayouts[right ? 1 : 0]++;
              }
              super.writeString(line, positions);
            }
          };
      stripper.setSortByPosition(true);
      String text = stripper.getText(doc);
      if (text.strip().length() < 50)
        throw new ResponseStatusException(
            UNPROCESSABLE_ENTITY,
            "This PDF has little readable text. Please export a text-based PDF; scanned PDFs need"
                + " OCR, which is not enabled yet.");
      if (text.length() > 40000)
        throw new ResponseStatusException(
            BAD_REQUEST,
            "This resume contains too much text to review. Please use a shorter file.");
      var metrics =
          info.entrySet().stream()
              .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
              .limit(20)
              .map(
                  e ->
                      Map.<String, Object>of("fontAndSize", e.getKey(), "characters", e.getValue()))
              .toList();
      List<Map<String, Object>> links = new ArrayList<>();
      for (int pageIndex = 0; pageIndex < doc.getNumberOfPages(); pageIndex++) {
        var page = doc.getPage(pageIndex);
        int pageNumber = pageIndex + 1;
        for (var annotation : page.getAnnotations())
          if (annotation instanceof PDAnnotationLink link
              && link.getAction() instanceof PDActionURI uri
              && safeUrl(uri.getURI())
              && link.getRectangle() != null) {
            var rect = link.getRectangle();
            float linkTop = page.getMediaBox().getHeight() - rect.getUpperRightY();
            float linkBottom = page.getMediaBox().getHeight() - rect.getLowerLeftY();
            String context = lines.stream()
                .filter(line -> ((Number) line.get("page")).intValue() == pageNumber)
                .filter(line -> {
                  float baseline = ((Number) line.get("baselinePt")).floatValue();
                  return baseline >= linkTop - 32 && baseline <= linkBottom + 4;
                })
                .sorted(Comparator.comparingDouble(line -> -((Number) line.get("baselinePt")).doubleValue()))
                .limit(3)
                .map(line -> line.get("text").toString())
                .collect(java.util.stream.Collectors.joining(" | "));
            var area = new PDFTextStripperByArea();
            area.addRegion(
                "label",
                new java.awt.geom.Rectangle2D.Float(
                    rect.getLowerLeftX() - 1,
                    page.getMediaBox().getHeight() - rect.getUpperRightY() - 1,
                    rect.getWidth() + 2,
                    rect.getHeight() + 2));
            area.extractRegions(page);
            links.add(
                Map.of(
                    "page",
                    pageIndex + 1,
                    "label",
                    area.getTextForRegion("label").strip(),
                    "context",
                    context,
                    "url",
                    uri.getURI()));
          }
      }
      List<String> images = new ArrayList<>();
      var renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
      renderer.setSubsamplingAllowed(true);
      for (int index = 0; index < Math.min(doc.getNumberOfPages(), 2); index++) {
        var output = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(renderer.renderImageWithDPI(index, 100), "png", output);
        images.add(
            "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray()));
      }
      return new Extraction(
          text,
          doc.getNumberOfPages(),
          metrics,
          dateLayouts[1] > dateLayouts[0] ? "right" : "inline",
          links,
          lines,
          pageSizes,
          images);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (IOException e) {
      throw new ResponseStatusException(
          BAD_REQUEST, "The PDF could not be read. Please re-export it and try again.");
    }
  }

  static void validate(JsonNode r, String level) {
    if (r == null || !r.isObject() || r.toString().length() > 50000)
      throw new ResponseStatusException(BAD_REQUEST, "Please provide a reasonably sized resume document.");

  }

  private static void validateEducation(String education) {
    Pattern dated = Pattern.compile(
        "^(.+?)\\s*\\|\\s*(Graduated|Expected)\\s+"
            + "(January|February|March|April|May|June|July|August|September|October|November|December)"
            + "\\s+((?:19|20)\\d{2})$", Pattern.CASE_INSENSITIVE);
    Pattern undated = Pattern.compile("^(.+?)\\s*\\|\\s*(Graduated|In progress|Not completed)$",
        Pattern.CASE_INSENSITIVE);
    for (String block : education.split("\\n\\s*\\n")) {
      String entry = block.strip().split("\\R", 2)[0];
      if (entry.isBlank()) continue;
      var match = dated.matcher(entry);
      if (match.matches()) {
        int year = Integer.parseInt(match.group(4));
        int month = java.time.Month.valueOf(match.group(3).toUpperCase(java.util.Locale.ROOT)).getValue();
        var date = java.time.YearMonth.of(year, month);
        var current = java.time.YearMonth.now();
        if (match.group(2).equalsIgnoreCase("Graduated") && date.isAfter(current))
          throw new ResponseStatusException(BAD_REQUEST, "A graduation date cannot be in the future.");
        if (match.group(2).equalsIgnoreCase("Expected") && date.isBefore(current))
          throw new ResponseStatusException(BAD_REQUEST, "An expected graduation date must be this month or later.");
      } else if (!undated.matcher(entry).matches()
          && entry.matches("(?i)^.+?\\s*\\|\\s*(?:Graduated|Expected)\\b.*$")) {
        throw new ResponseStatusException(BAD_REQUEST,
            "Use a valid education status and a complete month and year when you include a date.");
      }
    }
  }

  static boolean safeUrl(String u) {
    try {
      var x = java.net.URI.create(u);
      return List.of("http", "https").contains(x.getScheme()) && x.getHost() != null;
    } catch (Exception e) {
      return false;
    }
  }

  byte[] create(JsonNode resume, String level) {
    return create(resume, level, "Calibri", "inline");
  }

  byte[] create(JsonNode resume, String level, String requestedFont, String requestedDates) {
    return create(resume, level, requestedFont, requestedDates, "blue");
  }

  byte[] create(JsonNode resume, String level, String requestedFont, String requestedDates, String requestedLinkColor) {
    validate(resume, level);
    resume = ResumeCleanup.normalize(resume);
    String linkColor = requestedLinkColor == null ? "blue" : requestedLinkColor;
    if (!List.of("blue", "dark").contains(linkColor))
      throw new ResponseStatusException(BAD_REQUEST, "Choose original blue or dark blue for links.");
    byte[] fullSize = null;
    for (float bodySize : new float[] {ResumeTypography.forLevel(level).body()}) {
      byte[] candidate = render(resume, level, requestedFont, requestedDates, linkColor, bodySize);
      if (fullSize == null) fullSize = candidate;
      try (var document = org.apache.pdfbox.Loader.loadPDF(candidate)) {
        if (document.getNumberOfPages() == 1) return candidate;
      } catch (IOException e) {
        throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "The resume PDF could not be prepared.");
      }
    }
    return fullSize;
  }

  private static boolean datedExperienceLine(String line) {
    return !line.matches("^\\s*[•*\\-]\\s+.*")
        && line.matches(".*\\b(?:19|20)\\d{2}\\b.*");
  }

  static String formattedExperience(String source) {
    List<List<String>> groups = new ArrayList<>();
    for (String block : source.split("\\R\\s*\\R")) {
      List<String> lines = Arrays.stream(block.split("\\R")).map(String::strip)
          .filter(line -> !line.isBlank()).toList();
      if (lines.isEmpty()) continue;
      boolean separateCompany = lines.size() > 1 && !datedExperienceLine(lines.get(0))
          && datedExperienceLine(lines.get(1));
      if (separateCompany && !groups.isEmpty()) {
        List<String> previous = groups.get(groups.size() - 1);
        if (previous.size() > 1 && previous.get(0).equalsIgnoreCase(lines.get(0))
            && datedExperienceLine(previous.get(1))) {
          previous.addAll(lines.subList(1, lines.size()));
          continue;
        }
      }
      groups.add(new ArrayList<>(lines));
    }
    List<String> output = new ArrayList<>();
    for (List<String> lines : groups) {
      boolean separateCompany = lines.size() > 1 && !datedExperienceLine(lines.get(0))
          && datedExperienceLine(lines.get(1));
      long roles = lines.stream().filter(PdfService::datedExperienceLine).count();
      if (separateCompany && roles == 1) {
        List<String> combined = new ArrayList<>();
        combined.add(lines.get(0) + " — " + lines.get(1));
        combined.addAll(lines.subList(2, lines.size()));
        output.add(String.join("\n", combined));
      } else output.add(String.join("\n", lines));
    }
    return String.join("\n\n", output);
  }

  static String formattedProjects(String source) {
    List<String> output = new ArrayList<>();
    for (String block : source.split("\\R\\s*\\R")) {
      if (block.isBlank()) continue;
      List<String> lines = new ArrayList<>(Arrays.asList(block.split("\\R")));
      if (lines.size() > 1 && lines.get(1).matches("(?i)^\\s*(?:tech(?:nologies)?|stack|languages?)\\s*:.*")) {
        String stack = lines.remove(1).replaceFirst("(?i)^\\s*(?:tech(?:nologies)?|stack|languages?)\\s*:\\s*", "").strip();
        if (!stack.isBlank()) lines.set(0, lines.get(0).strip() + " — " + stack);
      }
      if (!lines.isEmpty() && !lines.get(0).matches("^\\s*[•*\\-]\\s+.*"))
        lines.set(0, "• " + lines.get(0).strip());
      output.add(String.join("\n", lines));
    }
    return String.join("\n\n", output);
  }

  private byte[] render(JsonNode resume, String level, String requestedFont, String requestedDates, String linkColor, float bodySize) {
    ResumeTypography typography = ResumeTypography.forLevel(level);
    String font = requestedFont == null ? typography.font() : requestedFont;
    String dates = requestedDates == null ? "inline" : requestedDates;
    if (!FONTS.contains(font))
      throw new ResponseStatusException(BAD_REQUEST, "Choose one of the six accepted fonts.");
    if (!List.of("inline", "right").contains(dates))
      throw new ResponseStatusException(
          BAD_REQUEST, "Choose dates beside the entry or aligned to the right.");
    Path directory = Path.of(regularPath).getParent();
    String[] files =
        switch (font) {
          case "Arial" -> new String[] {"arial.ttf", "arialbd.ttf"};
          case "Cambria" -> new String[] {"cambria.ttc", "cambriab.ttf"};
          case "Georgia" -> new String[] {"georgia.ttf", "georgiab.ttf"};
          case "Times New Roman" -> new String[] {"times.ttf", "timesbd.ttf"};
          case "Aptos" -> new String[] {"Aptos.ttf", "Aptos-Bold.ttf"};
          default -> new String[] {"calibri.ttf", "calibrib.ttf"};
        };
    if (font.equals("Aptos")) directory = Path.of(aptosDirectory);
    Path regularFile = font.equals("Calibri") ? Path.of(regularPath) : directory.resolve(files[0]);
    Path boldFile = font.equals("Calibri") ? Path.of(boldPath) : directory.resolve(files[1]);
    String italicName = switch (font) {
      case "Arial" -> "ariali.ttf";
      case "Cambria" -> "cambriai.ttf";
      case "Georgia" -> "georgiai.ttf";
      case "Times New Roman" -> "timesi.ttf";
      case "Aptos" -> "Aptos-Italic.ttf";
      default -> "calibrii.ttf";
    };
    Path italicFile = directory.resolve(italicName);
    if (!Files.isRegularFile(regularFile) || !Files.isRegularFile(boldFile)) {
      String[] hosted = switch (font) {
        case "Cambria" -> new String[] {"crosextra/Caladea-Regular.ttf", "crosextra/Caladea-Bold.ttf"};
        case "Georgia" -> new String[] {"dejavu/DejaVuSerif.ttf", "dejavu/DejaVuSerif-Bold.ttf"};
        case "Times New Roman" -> new String[] {"liberation2/LiberationSerif-Regular.ttf", "liberation2/LiberationSerif-Bold.ttf"};
        case "Arial", "Aptos" -> new String[] {"liberation2/LiberationSans-Regular.ttf", "liberation2/LiberationSans-Bold.ttf"};
        default -> new String[] {"crosextra/Carlito-Regular.ttf", "crosextra/Carlito-Bold.ttf"};
      };
      regularFile = Path.of("/usr/share/fonts/truetype", hosted[0]);
      boldFile = Path.of("/usr/share/fonts/truetype", hosted[1]);
      italicFile = Path.of("/usr/share/fonts/truetype", hosted[0].replace("Regular", "Italic"));
      if ((!Files.isRegularFile(regularFile) || !Files.isRegularFile(boldFile))
          && hosted[0].startsWith("liberation2/")) {
        regularFile = Path.of("/usr/share/fonts/truetype", hosted[0].replace("liberation2/", "liberation/"));
        boldFile = Path.of("/usr/share/fonts/truetype", hosted[1].replace("liberation2/", "liberation/"));
        italicFile = Path.of("/usr/share/fonts/truetype", hosted[0].replace("liberation2/", "liberation/").replace("Regular", "Italic"));
      }
    }
    if (!Files.isRegularFile(regularFile) || !Files.isRegularFile(boldFile))
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE, "The selected " + font + " font needs configuring on this server.");
    try (var doc = new PDDocument();
        var out = new ByteArrayOutputStream();
        var collection =
            regularFile.toString().toLowerCase(Locale.ROOT).endsWith(".ttc")
                ? new org.apache.fontbox.ttf.TrueTypeCollection(regularFile.toFile())
                : null) {
      PDPage page = new PDPage(PDRectangle.LETTER);
      doc.addPage(page);
      PDFont
          regular =
              collection == null
                  ? PDType0Font.load(doc, regularFile.toFile())
                  : PDType0Font.load(doc, collection.getFontByName("Cambria"), true),
          bold = PDType0Font.load(doc, boldFile.toFile()),
          italic = Files.isRegularFile(italicFile) ? PDType0Font.load(doc, italicFile.toFile()) : regular;
      try (Writer w = new Writer(doc, page, regular, bold, italic, typography,
            linkColor.equals("dark") ? new java.awt.Color(0x23, 0x47, 0x6A) : new java.awt.Color(0, 0, 0xEE))) {
        w.center(resume.path("contact").path("name").asText(), typography.name(), bold);
        w.y -= 4;
        List<String> contact = new ArrayList<>();
        JsonNode c = resume.path("contact");
        if (!c.path("location").asText().isBlank()) contact.add(c.path("location").asText());
        if (!c.path("phone").asText().isBlank()) contact.add(c.path("phone").asText());
        if (!c.path("email").asText().isBlank()) contact.add(c.path("email").asText() + "@@mailto:" + c.path("email").asText());
        for (String k : List.of("linkedin", "github", "portfolio"))
          if (!c.path(k).asText().isBlank())
            contact.add(
                (k.equals("linkedin") ? "LinkedIn" : k.equals("github") ? "GitHub" : "Portfolio")
                    + "@@"
                    + c.path(k).asText());
        w.contacts(contact);
        w.y -= 6;
        w.paragraph(resume.path("summary").asText(), regular, bodySize, typography.leading());
        String[][] sections = {
          {"experience", "EXPERIENCE"},
          {"clinicalRotations", "CLINICAL ROTATIONS"},
          {"clinicalTraining", "CLINICAL TRAINING"},
          {"proceduresSpecialty", "PROCEDURES & SPECIALTY EXPERIENCE"},
          {"residencyFellowship", "RESIDENCY / FELLOWSHIP"},
          {"licensure", "LICENSURE"}, {"boardCertification", "BOARD CERTIFICATION"},
          {"research", "RESEARCH"}, {"publications", "PUBLICATIONS"},
          {"teaching", "TEACHING"}, {"presentations", "PRESENTATIONS"},
          {"skills", ResumeProfiles.skillsHeading(level)},
          {"projects", ResumeProfiles.projectsHeading(level)},
          {"education", "EDUCATION"},
          {"certifications", ResumeProfiles.certificationsHeading(level)}
        };
        for (String[] s : sections) {
          s[1] = ResumeProfiles.sectionHeading(s[0], s[1], level, resume);
          String text = resume.path(s[0]).asText();
          if (s[0].equals("experience")) text = formattedExperience(text);
          if (s[0].equals("projects")) text = formattedProjects(text);
          if (text.isBlank()) continue;
          w.y -= 6;
          w.heading(s[1]);
          w.y -= 3;
          boolean projectHeading = true;
          for (String line : text.split("\n", -1)) {
            if (line.isBlank()) {
              if (!s[0].equals("education")) w.y -= 3;
              projectHeading = true;
              continue;
            }
            w.entry(line, dates, s[0], s[0].equals("projects") && projectHeading);
            projectHeading = false;
            w.y -= s[0].equals("skills") || s[0].equals("projects") ? 2 : 0;
          }
        }

      }
      doc.getDocumentInformation().setTitle("Professional Resume");
      doc.save(out);
      return out.toByteArray();
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY,
          "The PDF could not be generated. Check for unsupported characters and try again.");
    }
  }

  byte[] preview(JsonNode resume, String level, String font, String dates) {
    return preview(resume, level, font, dates, "blue");
  }

  byte[] preview(JsonNode resume, String level, String font, String dates, String linkColor) {
    try (var doc = Loader.loadPDF(create(resume, level, font, dates, linkColor));
        var out = new ByteArrayOutputStream()) {
      javax.imageio.ImageIO.write(
          new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0, 110), "png", out);
      return out.toByteArray();
    } catch (ResponseStatusException e) {
      throw e;
    } catch (IOException e) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "The preview could not be created.");
    }
  }

  static class Writer implements AutoCloseable {
    final PDDocument document;
    PDPageContentStream stream;
    PDPage page;
    final PDFont regular, bold, italic;
    final float bodySize;
    final ResumeTypography typography;
    final java.awt.Color linkColor;
    float y;
    final float left, width;

    Writer(PDDocument doc, PDPage p, PDFont r, PDFont b, PDFont i, ResumeTypography type, java.awt.Color color) throws IOException {
      document = doc;
      stream = new PDPageContentStream(doc, p);
      page = p;
      regular = r;
      bold = b;
      italic = i;
      typography = type;
      bodySize = type.body();
      left = type.side();
      width = 612 - 2 * left;
      y = 792 - type.vertical();
      linkColor = color;
    }

    void advance(float leading) throws IOException {
      if (y - leading < typography.vertical()) {
        stream.close();
        page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        stream = new PDPageContentStream(document, page);
        y = 792 - typography.vertical();
      }
      y -= leading;
    }

    public void close() throws IOException { stream.close(); }

    float width(String s, PDFont f, float size) throws IOException {
      return f.getStringWidth(s.replace('—', '–').replace(" - ", " – ")) / 1000 * size;
    }

    void text(String s, float x, float baseline, PDFont f, float size) throws IOException {
      stream.beginText();
      stream.setFont(f, size);
      stream.newLineAtOffset(x, baseline);
      stream.showText(s.replace('—', '–').replace(" - ", " – "));
      stream.endText();
    }

    void center(String s, float size, PDFont f) throws IOException {
      if (width(s, f, size) > width)
        throw new ResponseStatusException(
            UNPROCESSABLE_ENTITY,
            "The name is too wide for this template; please review the contact section.");
      y -= size;
      text(s, left + (width - width(s, f, size)) / 2, y, f, size);
    }

    void contacts(List<String> items) throws IOException {
      float size = typography.contact(), separator = width(" · ", regular, typography.contact());
      List<List<String>> lines = new ArrayList<>();
      List<String> line = new ArrayList<>();
      float used = 0;
      for (String item : items) {
        float next = width(item.split("@@")[0], regular, size);
        if (next > width)
          throw new ResponseStatusException(
              UNPROCESSABLE_ENTITY, "A contact detail is too long. Please review the header.");
        if (used + next + (line.isEmpty() ? 0 : separator) > width && !line.isEmpty()) {
          lines.add(line);
          line = new ArrayList<>();
          used = 0;
        }
        used += next + (line.isEmpty() ? 0 : separator);
        line.add(item);
      }
      if (!line.isEmpty()) lines.add(line);
      for (var row : lines) {
        float total = separator * (row.size() - 1);
        for (String item : row) total += width(item.split("@@")[0], regular, size);
        float x = left + (width - total) / 2;
        y -= 12;
        for (int i = 0; i < row.size(); i++) {
          if (i > 0) {
            text(" · ", x, y, regular, size);
            x += separator;
          }
          String[] parts = row.get(i).split("@@", 2);
          float tw = width(parts[0], regular, size);
          if (parts.length == 2) stream.setNonStrokingColor(linkColor);
          text(parts[0], x, y, regular, size);
          if (parts.length == 2) stream.setNonStrokingColor(0, 0, 0);
          if (parts.length == 2) {
            link(parts[1], x, y, tw, size);
            stream.setStrokingColor(linkColor);
            stream.setLineWidth(.35f);
            stream.moveTo(x, y - 1.5f);
            stream.lineTo(x + tw, y - 1.5f);
            stream.stroke();
            stream.setStrokingColor(0, 0, 0);
          }
          x += tw;
        }
      }
    }

    void heading(String value) throws IOException {
      paragraph(value, bold, typography.heading(), typography.leading());
      stream.setLineWidth(.5f);
      stream.moveTo(left, y - 1.5f);
      stream.lineTo(left + width(value, bold, typography.heading()), y - 1.5f);
      stream.stroke();
    }

    void entry(String line, String dates, String section, boolean projectHeading) throws IOException {
      line = line.replace("\t", " | ").strip();
      float size = bodySize, leading = typography.leading();
      boolean bullet = line.startsWith("•") || line.startsWith("- ");
      int split = line.lastIndexOf(" | ");
      boolean dated = split > 0 && !bullet
          && line.substring(split + 3).matches(".*\\b(?:19|20)\\d{2}.*")
          && !line.substring(split + 3).contains("http");

      if (section.equals("projects") && !dated) {
        if (line.matches("(?i)^Organization:.*")) {
          linkParagraph(line.replaceFirst("(?i)^Organization:\\s*", ""), italic, size, leading, false, false, true, false);
          return;
        }
        linkParagraph(line, regular, size, leading, projectHeading, false, true, projectHeading);
        return;
      }
      if (section.equals("certifications")) {
        var credential = CredentialFormatting.parse(line);
        List<String> fragments = new ArrayList<>();
        List<PDFont> fonts = new ArrayList<>();
        fragments.add(credential.name() + (credential.issuer().isBlank() ? "" : ",")); fonts.add(regular);
        if (!credential.issuer().isBlank()) { fragments.add(credential.issuer()); fonts.add(italic); }
        if (!credential.date().isBlank()) {
          fragments.add("|"); fonts.add(regular);
          fragments.add(credential.date()); fonts.add(bold);
        }
        float x = left;
        advance(leading);
        for (int i = 0; i < fragments.size(); i++) {
          PDFont f = fonts.get(i);
          for (String word : fragments.get(i).split("\\s+")) {
            float wordWidth = width(word, f, size);
            if (wordWidth > width) throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "A certification or training entry contains a word too long to fit.");
            if (x + wordWidth > left + width) { advance(leading); x = left; }
            text(word, x, y, f, size);
            x += wordWidth + width(" ", f, size);
          }
        }
        return;
      }
      if (section.equals("education")) {
        String title = dated ? line.substring(0, split) : line;
        String date = dated ? line.substring(split + 3) : "";
        String[] parts = title.split("\\s+[—–-]\\s+", 2);
        List<String> fragments = new ArrayList<>();
        List<PDFont> fonts = new ArrayList<>();
        fragments.add(parts[0]); fonts.add(regular);
        if (parts.length > 1) {
          fragments.add(" —"); fonts.add(regular);
          fragments.add(parts[1]); fonts.add(italic);
        }
        if (dated && !dates.equals("right")) {
          fragments.add("| " + date); fonts.add(regular);
        }
        float x = left;
        advance(leading);
        for (int i = 0; i < fragments.size(); i++) {
          PDFont f = fonts.get(i);
          for (String word : fragments.get(i).split("\\s+")) {
            float wordWidth = width(word, f, size);
            if (wordWidth > width) throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "An education entry contains a word too long to fit.");
            if (x + wordWidth > left + width) { advance(leading); x = left; }
            text(word, x, y, f, size);
            x += wordWidth + width(" ", f, size);
          }
        }
        if (dated && dates.equals("right")) {
          float dateWidth = width(date, regular, size);
          if (x + dateWidth > left + width) advance(leading);
          text(date, left + width - dateWidth, y, regular, size);
        }
        return;
      }
      if (section.equals("experience") && !bullet && (dated || line.contains(" — "))) {
        String title = dated ? line.substring(0, split) : line;
        String date = dated ? line.substring(split + 3) : "";
        int separator = title.indexOf(" — ");
        advance(leading);
        float x = left;
        String[] fragments = separator < 0 ? new String[]{title}
            : new String[]{title.substring(0, separator), title.substring(separator)};
        for (int part = 0; part < fragments.length; part++) {
          PDFont face = separator >= 0 && part == 0 ? bold : italic;
          for (String word : fragments[part].strip().split("\\s+")) {
            float wordWidth = width(word, face, size);
            if (wordWidth > width) throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "An experience heading contains a word too long to fit.");
            if (x + wordWidth > left + width) { advance(leading); x = left; }
            text(word, x, y, face, size);
            x += wordWidth + width(" ", face, size);
          }
        }
        if (!date.isBlank()) {
          float dateWidth = width(date, regular, size);
          if (dates.equals("right")) {
            if (x + dateWidth + 14 > left + width) advance(leading);
            text(date, left + width - dateWidth, y, regular, size);
          } else {
            String suffix = " | " + date;
            if (x + width(suffix, regular, size) > left + width) { advance(leading); x = left; suffix = date; }
            text(suffix, x, y, regular, size);
          }
        }
        return;
      }
      if (dates.equals("right") && dated) {
        String title = line.substring(0, split), date = line.substring(split + 3);
        PDFont titleFont = section.equals("experience") && !title.contains(" — ") ? italic : bold;
        float tw = width(title, titleFont, size), dw = width(date, regular, size);
        if (tw + dw + 14 > width) {
          paragraph(title, titleFont, size, leading);
          advance(leading);
        } else {
          advance(leading);
          if (section.equals("experience") && title.contains(" — ")) {
            int separator = title.indexOf(" — ");
            String employer = title.substring(0, separator);
            text(employer, left, y, bold, size);
            text(title.substring(separator), left + width(employer, bold, size), y, italic, size);
          } else text(title, left, y, titleFont, size);
        }
        text(date, left + width - dw, y, regular, size);
      } else {
        boolean title = !bullet && (section.equals("experience") || section.equals("certifications"))
            && !(section.equals("experience") && dated);
        linkParagraph(line, title ? bold : regular, size, leading,
            bullet, section.equals("skills"), false, false);
      }
    }

    void paragraph(String s, PDFont f, float size, float leading) throws IOException {
      StringBuilder line = new StringBuilder();
      for (String word : s.split("\\s+")) {
        if (width(word, f, size) > width)
          throw new ResponseStatusException(
              UNPROCESSABLE_ENTITY, "A long unbroken word does not fit. Please edit that section.");
        String next = line.isEmpty() ? word : line + " " + word;
        if (width(next, f, size) > width) {
          advance(leading);
          text(line.toString(), left, y, f, size);
          line = new StringBuilder(word);
        } else line = new StringBuilder(next);
      }
      if (!line.isEmpty()) {
        advance(leading);
        text(line.toString(), left, y, f, size);
      }
    }

    void linkParagraph(String s, PDFont f, float size, float leading, boolean bullet, boolean skill, boolean project, boolean projectHeading) throws IOException {
      if (bullet) s = s.replaceFirst("^[•-]\\s*", "");
      Matcher labeled = Pattern.compile("\\[([^\\]\\r\\n]+)\\]\\((https?://[^\\s)]+)\\)").matcher(s);
      List<String> urls = new ArrayList<>();
      List<String> labels = new ArrayList<>();
      StringBuffer buf = new StringBuffer();
      while (labeled.find()) {
        urls.add(labeled.group(2));
        labels.add(labeled.group(1));
        labeled.appendReplacement(buf, "LINKTOKEN" + (urls.size() - 1));
      }
      labeled.appendTail(buf);
      Matcher m = Pattern.compile("https?://\\S+").matcher(buf.toString());
      buf = new StringBuffer();
      while (m.find()) {
        urls.add(m.group());
        labels.add((m.group().contains("github") || m.group().contains("gitlab")
            || m.group().contains("bitbucket")) ? "GitHub Repo" : "View Project");
        m.appendReplacement(buf, "LINKTOKEN" + (urls.size() - 1));
      }
      m.appendTail(buf);
      String text = buf.toString();
      float start = left + (bullet || project ? 18 : 0);
      float x = start;
      String[] words = text.split("\\s+");
      advance(leading);
      if (bullet) text("•", left + 9, y, regular, size);
      boolean category = (skill && text.contains(":")) || projectHeading;
      for (int i = 0; i < words.length; i++) {
        String word = words[i];
        Matcher linkToken = Pattern.compile("LINKTOKEN(\\d+)").matcher(word);
        boolean label = linkToken.matches();
        int linkIndex = label ? Integer.parseInt(linkToken.group(1)) : -1;
        if (label) word = labels.get(linkIndex);
        if (projectHeading && (word.equals("|") || word.equals("—") || word.equals("–"))) category = false;
        PDFont wordFont = category ? bold : f;
        float tw = width(word, wordFont, size);
        if (tw > width - (start - left))
          throw new ResponseStatusException(
              UNPROCESSABLE_ENTITY, "A long word does not fit. Please edit the section.");
        if (x + tw > left + width) {
          x = start;
          advance(leading);
        }
        if (label) stream.setNonStrokingColor(linkColor);
        text(word, x, y, wordFont, size);
        if (label) stream.setNonStrokingColor(0, 0, 0);
        if (word.endsWith(":") || (project && word.equals("|"))) category = false;
        if (label) {
          link(urls.get(linkIndex), x, y, tw, size);
          stream.setStrokingColor(linkColor);
          stream.setLineWidth(.35f);
          stream.moveTo(x, y - 1.5f); stream.lineTo(x + tw, y - 1.5f); stream.stroke();
          stream.setStrokingColor(0, 0, 0);
        }
        x += tw + width(" ", f, size);
      }
    }

    void link(String url, float x, float y, float w, float size) throws IOException {
      if (!safeUrl(url) && !url.matches("mailto:[^\\s<>@]+@[^\\s<>@]+\\.[^\\s<>@]+")) return;
      var link = new PDAnnotationLink();
      link.setRectangle(new PDRectangle(x, y - 2, w, size + 3));
      var border = new PDBorderStyleDictionary();
      border.setWidth(0);
      link.setBorderStyle(border);
      var action = new PDActionURI();
      action.setURI(url);
      link.setAction(action);
      page.getAnnotations().add(link);
    }
  }
}
