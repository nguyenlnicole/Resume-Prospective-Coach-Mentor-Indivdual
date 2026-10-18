package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class DocxService {
  byte[] create(JsonNode resume, String level, String font, String dates, String linkColor) {
    PdfService.validate(resume, level);
    resume = ResumeCleanup.normalize(resume);
    ResumeTypography typography = ResumeTypography.forLevel(level);
    if (font == null) font = typography.font();
    if (dates == null) dates = "inline";
    if (linkColor == null) linkColor = "blue";
    if (!PdfService.FONTS.contains(font) || !List.of("inline", "right").contains(dates)
        || !List.of("blue", "dark").contains(linkColor))
      throw new ResponseStatusException(BAD_REQUEST, "Choose valid resume formatting options.");
    Builder b = new Builder(font, dates, linkColor, typography);
    JsonNode contact = resume.path("contact");
    b.line(b.run(contact.path("name").asText(), true, false, Math.round(typography.name() * 2)), "center", 0, 40);
    StringBuilder details = new StringBuilder();
    for (String key : List.of("location", "phone", "email")) {
      String value = contact.path(key).asText();
      if (!value.isBlank()) {
        if (!details.isEmpty()) details.append(b.run(" · ", false, false, 20));
        details.append(key.equals("email") ? b.link(value, "mailto:" + value) : b.run(value, false, false, 20));
      }
    }
    for (String key : List.of("linkedin", "github", "portfolio")) {
      String url = contact.path(key).asText();
      if (!url.isBlank()) {
        details.append(b.run(" · ", false, false, 20));
        details.append(b.link(key.equals("linkedin") ? "LinkedIn" : key.equals("github") ? "GitHub" : "Portfolio", url));
      }
    }
    b.line(details.toString(), "center", 0, 70);
    b.line(b.run(resume.path("summary").asText(), false, false, 20), "left", 0, 45);
    String[][] sections = {{"experience", "EXPERIENCE"}, {"clinicalRotations", "CLINICAL ROTATIONS"}, {"clinicalTraining", "CLINICAL TRAINING"}, {"skills", ResumeProfiles.skillsHeading(level)},
        {"proceduresSpecialty", "PROCEDURES & SPECIALTY EXPERIENCE"},
        {"residencyFellowship", "RESIDENCY / FELLOWSHIP"},
        {"licensure", "LICENSURE"}, {"boardCertification", "BOARD CERTIFICATION"},
        {"research", "RESEARCH"}, {"publications", "PUBLICATIONS"},
        {"teaching", "TEACHING"}, {"presentations", "PRESENTATIONS"},
        {"projects", ResumeProfiles.projectsHeading(level)}, {"education", "EDUCATION"},
        {"certifications", ResumeProfiles.certificationsHeading(level)}};
    for (String[] section : sections) {
      String value = resume.path(section[0]).asText();
      if (value.isBlank()) continue;
      b.line(b.run(section[1].toUpperCase(), true, false, Math.round(typography.heading() * 2)).replace("</w:rPr>", "<w:u w:val=\"single\"/></w:rPr>"), "left", 120, 40);
      if (section[0].equals("experience")) value = PdfService.formattedExperience(value);
      if (section[0].equals("projects")) value = PdfService.formattedProjects(value);
      for (String row : value.split("\\R")) {
        if (row.isBlank()) continue;
        boolean bullet = row.matches("^\\s*[•*\\-]\\s+.*");
        if (bullet) {
          String text = row.replaceFirst("^\\s*[•*\\-]\\s+", "");
          b.line(b.run("•  ", false, false, 20) + (section[0].equals("projects") ? b.projectHeading(text) : b.linked(text, false)), "bullet", 0, 0);
          continue;
        }
        if (section[0].equals("certifications")) {
          var credential = CredentialFormatting.parse(row);
          String runs = b.run(credential.name(), false, false, 20);
          if (!credential.issuer().isBlank()) runs += b.run(", ", false, false, 20) + b.run(credential.issuer(), false, true, 20);
          if (!credential.date().isBlank()) runs += b.run(" | ", false, false, 20) + b.run(credential.date(), true, false, 20);
          b.line(runs, "left", 35, 0);
          continue;
        }
        int divider = row.lastIndexOf(" | ");
        boolean dated = divider > 0 && row.substring(divider + 3).matches(".*\\b(?:19|20)\\d{2}.*")
            && !row.substring(divider + 3).contains("http");
        String title = dated ? row.substring(0, divider) : row;
        StringBuilder runs = new StringBuilder();
        if (section[0].equals("projects") && title.matches("(?i)^Organization:.*")) {
          runs.append(b.run(title.replaceFirst("(?i)^Organization:\\s*", ""), false, true, 20));
        } else if (section[0].equals("education")) {
          String[] parts = title.split("\\s+[—–-]\\s+", 2);
          runs.append(b.run(parts[0], false, false, 20));
          if (parts.length > 1) runs.append(b.run(" — ", false, false, 20))
              .append(b.run(parts[1], false, true, 20));
        } else if (section[0].equals("experience") && title.contains(" — ")) {
          int dash = title.indexOf(" — ");
          runs.append(b.run(title.substring(0, dash), true, false, 20))
              .append(b.run(title.substring(dash), false, true, 20));
        } else if (section[0].equals("experience") && dated)
          runs.append(b.run(title, false, true, 20));
        else if (section[0].equals("experience"))
          runs.append(b.run(title, true, false, 20));
        else if (section[0].equals("skills") && title.contains(":")) {
          int colon = title.indexOf(':') + 1;
          runs.append(b.run(title.substring(0, colon), true, false, 20))
              .append(b.run(title.substring(colon), false, false, 20));
        }
        else runs.append(b.linked(title, false));
        if (dated) runs.append(b.run(dates.equals("right") ? "\t" : "  ", false, false, 20))
            .append(b.run(row.substring(divider + 3), false, false, 20));
        b.line(runs.toString(), dated && dates.equals("right") ? "date" : section[0].equals("projects") ? "project" : "left", 35, 0);
      }
    }
    return b.finish();
  }

  private static String xml(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;");
  }

  private static void zipEntry(ZipOutputStream zip, String name, String content) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(content.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  private static class Builder {
    final String font, dates, color;
    final ResumeTypography typography;
    final StringBuilder body = new StringBuilder();
    final List<String> links = new ArrayList<>();

    Builder(String font, String dates, String color, ResumeTypography typography) {
      this.typography = typography;
      this.font = font;
      this.dates = dates;
      this.color = color.equals("dark") ? "23476A" : "0000EE";
    }

    String run(String text, boolean bold, boolean italic, int size) {
      if (size == 20) size = Math.round(typography.body() * 2);
      text = text.replace('—', '–').replace(" - ", " – ");
      if (text.equals("\t")) return "<w:r><w:tab/></w:r>";
      return "<w:r><w:rPr><w:rFonts w:ascii=\"" + xml(font) + "\" w:hAnsi=\"" + xml(font)
          + "\"/><w:sz w:val=\"" + size + "\"/>" + (bold ? "<w:b/>" : "")
          + (italic ? "<w:i/>" : "") + "</w:rPr><w:t xml:space=\"preserve\">"
          + xml(text) + "</w:t></w:r>";
    }

    String link(String label, String target) {
      links.add(target);
      return "<w:hyperlink r:id=\"rId" + links.size() + "\"><w:r><w:rPr><w:rFonts w:ascii=\""
          + xml(font) + "\"/><w:sz w:val=\"" + Math.round(typography.body() * 2) + "\"/><w:color w:val=\"" + color
          + "\"/><w:u w:val=\"single\"/></w:rPr><w:t xml:space=\"preserve\">"
          + xml(label) + "</w:t></w:r></w:hyperlink>";
    }

    String projectHeading(String text) {
      int pipe = text.indexOf(" | "), dash = text.indexOf(" — ");
      int end = pipe < 0 ? dash < 0 ? text.length() : dash : dash < 0 ? pipe : Math.min(pipe, dash);
      return linked(text.substring(0, end), false).replace("</w:rPr>", "<w:b/></w:rPr>") + linked(text.substring(end), false);
    }

    String linked(String text, boolean heading) {
      Matcher m = Pattern.compile("\\[([^\\]]+)\\]\\((https?://[^)]+)\\)|https?://[^\\s|)]+")
          .matcher(text);
      StringBuilder result = new StringBuilder();
      int offset = 0;
      while (m.find()) {
        if (m.start() > offset) result.append(run(text.substring(offset, m.start()), heading && offset == 0, false, 20));
        String url = m.group(2) == null ? m.group() : m.group(2);
        result.append(link(m.group(1) == null
            ? (url.contains("github") || url.contains("gitlab") ? "GitHub Repo" : "Demo")
            : m.group(1), url));
        offset = m.end();
      }
      if (offset < text.length()) result.append(run(text.substring(offset), heading && offset == 0, false, 20));
      return result.toString();
    }

    void line(String runs, String alignment, int before, int after) {
      body.append("<w:p><w:pPr><w:spacing w:before=\"").append(before)
          .append("\" w:after=\"").append(after)
          .append("\" w:line=\"").append(Math.round(typography.leading() * 20)).append("\" w:lineRule=\"atLeast\"/>");
      if (alignment.equals("center")) body.append("<w:jc w:val=\"center\"/>");
      if (alignment.equals("date")) body.append("<w:tabs><w:tab w:val=\"right\" w:pos=\"" + Math.round((612 - typography.side() * 2) * 20) + "\"/></w:tabs>");
      if (alignment.equals("project")) body.append("<w:ind w:left=\"360\"/>");
      if (alignment.equals("bullet")) body.append("<w:ind w:left=\"360\" w:hanging=\"180\"/>");
      body.append("</w:pPr>").append(runs).append("</w:p>");
    }

    byte[] finish() {
      body.append("<w:sectPr><w:pgSz w:w=\"12240\" w:h=\"15840\"/>"
          + "<w:pgMar w:top=\"" + Math.round(typography.vertical() * 20) + "\" w:right=\"" + Math.round(typography.side() * 20) + "\" w:bottom=\"" + Math.round(typography.vertical() * 20) + "\" w:left=\"" + Math.round(typography.side() * 20) + "\"/>"
          + "</w:sectPr>");
      String document = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
          + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><w:body>"
          + body + "</w:body></w:document>";
      StringBuilder rels = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
      for (int i = 0; i < links.size(); i++)
        rels.append("<Relationship Id=\"rId").append(i + 1)
            .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\" Target=\"")
            .append(xml(links.get(i))).append("\" TargetMode=\"External\"/>");
      rels.append("</Relationships>");
      try (var out = new ByteArrayOutputStream(); var zip = new ZipOutputStream(out)) {
        zipEntry(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
            + "</Types>");
        zipEntry(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
            + "</Relationships>");
        zipEntry(zip, "word/document.xml", document);
        zipEntry(zip, "word/_rels/document.xml.rels", rels.toString());
        zip.finish();
        return out.toByteArray();
      } catch (IOException e) {
        throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "The Word resume could not be created.");
      }
    }
  }
}
