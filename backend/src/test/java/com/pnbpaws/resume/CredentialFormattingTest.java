package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class CredentialFormattingTest {
  static final String SOURCE = "Professional Scrum Master (PSM I), _Scrum.org |&#x20;_**January 2024&#x20;**\nAWS Cloud Bootcamp, _Thinkcloudly |&#x20;_**September 2026**";
  @Test void cleansFormattingAndPreservesOptionalFacts() {
    String expected = "Professional Scrum Master (PSM I), Scrum.org | January 2024\nAWS Cloud Bootcamp, Thinkcloudly | September 2026";
    assertEquals(expected, CredentialFormatting.normalize(SOURCE));
    assertEquals(expected, CredentialFormatting.normalize(expected));
    assertEquals("PSM I, Scrum.org | January 2024", CredentialFormatting.normalize("PSM I – Scrum.org | January 2024"));
    assertEquals(new CredentialFormatting("Course", "", ""), CredentialFormatting.parse("Course"));
  }
  @Test void previewFactsSurvivePdfAndWordWithItalicProvidersAndBoldDates() throws Exception {
    ObjectNode resume = (ObjectNode) new ObjectMapper().readTree(getClass().getResourceAsStream("/approved-reference.json"));
    resume.put("certifications", SOURCE);
    resume.put("projects", "");
    for (String placement : List.of("right", "inline")) {
      var pdf = new PdfService("C:/Windows/Fonts/calibri.ttf", "C:/Windows/Fonts/calibrib.ttf");
      try (var document = Loader.loadPDF(pdf.create(resume, "Senior", "Calibri", placement))) {
        Map<String, StringBuilder> byFont = new HashMap<>();
        String text = new PDFTextStripper() {
          @Override protected void processTextPosition(TextPosition position) {
            byFont.computeIfAbsent(position.getFont().getName().toLowerCase(), ignored -> new StringBuilder()).append(position.getUnicode());
            super.processTextPosition(position);
          }
        }.getText(document);
        assertEquals(1, text.split("January 2024", -1).length - 1);
        assertEquals(1, text.split("September 2026", -1).length - 1);
        assertTrue(byFont.entrySet().stream().anyMatch(e -> e.getKey().contains("italic") && e.getValue().toString().contains("Scrum.org")));
        assertTrue(byFont.entrySet().stream().anyMatch(e -> e.getKey().contains("bold") && e.getValue().toString().replace(" ", "").contains("January2024")));
        String preview = System.getProperty("credential.preview.output");
        if (preview != null && placement.equals("inline")) javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(document).renderImageWithDPI(0, 110), "png", new File(preview));
      }
      String xml = "";
      try (var zip = new ZipInputStream(new ByteArrayInputStream(new DocxService().create(resume, "Senior", "Calibri", placement, "blue")))) {
        for (var part = zip.getNextEntry(); part != null; part = zip.getNextEntry())
          if (part.getName().equals("word/document.xml")) xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
      }
      assertTrue(xml.contains("<w:i/></w:rPr><w:t xml:space=\"preserve\">Scrum.org"));
      assertTrue(xml.contains("<w:b/></w:rPr><w:t xml:space=\"preserve\">January 2024"));
      assertTrue(xml.contains("<w:i/></w:rPr><w:t xml:space=\"preserve\">Thinkcloudly"));
      assertEquals(1, xml.split("September 2026", -1).length - 1);
    }
  }
}
