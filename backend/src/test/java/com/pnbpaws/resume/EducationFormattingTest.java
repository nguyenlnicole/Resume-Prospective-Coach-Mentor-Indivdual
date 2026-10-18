package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

class EducationFormattingTest {
  static final String SOURCE = "Example University — Bachelor's in Software Engineering\nAccelerated software engineering program focused on programming.\n\nTraining Provider — Technical mentorship\nCompleted technical mentorship focused on development.\nBootcamp Provider — Full-stack coding bootcamp\nAttended a 14-week bootcamp.\nExample College\nAssociate of Science\nMay 2024";
  static final String EXPECTED = "Example University — Bachelor's in Software Engineering\n\nTraining Provider — Technical mentorship\n\nBootcamp Provider — Full-stack coding bootcamp\n\nExample College — Associate of Science | May 2024";

  @Test void automaticallyRemovesDescriptionsAndPreservesCredentialsAndOptionalDates() {
    assertEquals(EXPECTED, ResumeCleanup.compactEducation(SOURCE));
    assertEquals(EXPECTED, ResumeCleanup.compactEducation(EXPECTED));
    assertEquals("Example College — B.S.", ResumeCleanup.compactEducation("Example College — B.S.\nRelevant coursework: databases\n• Built training applications"));
  }

  @Test void graduationDateAppearsOnceAndUsesRangeEnd() {
    assertEquals("Example University — B.S. | May 2022", ResumeCleanup.compactEducation("Example University — B.S. | Aug. 2020–May 2022"));
    assertEquals("Example University — B.S. | May 2022", ResumeCleanup.compactEducation("Example University — B.S. | Aug. 2020–May 2022 | Graduated May 2022"));
    assertEquals("Example College — A.S. | May 2020", ResumeCleanup.compactEducation("Example College — A.S.\nJan. 2018–May 2020"));
    assertEquals("Example University — B.S. | 2020–Present", ResumeCleanup.compactEducation("Example University — B.S. | 2020–Present"));
  }

  @Test void removesGraduatedLabelButKeepsTheDate() {
    assertEquals("Example University — B.S. | May 2022", ResumeCleanup.compactEducation("Example University — B.S. | Graduated May 2022"));
    assertEquals("Example University — B.S.", ResumeCleanup.compactEducation("Example University — B.S. | Graduated"));
  }

  @Test void exportedPdfAndWordKeepOnlyHeadingsWithItalicDegrees() throws Exception {
    ObjectNode resume = (ObjectNode) new ObjectMapper().readTree(getClass().getResourceAsStream("/approved-reference.json"));
    resume.put("education", SOURCE);
    resume.put("projects", "");
    PdfService pdf = new PdfService("C:/Windows/Fonts/calibri.ttf", "C:/Windows/Fonts/calibrib.ttf");
    for (String placement : new String[]{"inline", "right"}) {
      byte[] bytes = pdf.create(resume, "Senior", "Calibri", placement);
      var extraction = pdf.extract(bytes);
      assertEquals(1, extraction.pages());
      assertTrue(extraction.text().contains("Bachelor's in"));
      assertTrue(extraction.text().contains("Engineering"));
      assertTrue(extraction.text().contains("Technical mentorship"));
      assertTrue(extraction.text().contains("May 2024"));
      assertFalse(extraction.text().contains("Completed technical"));
      assertFalse(extraction.text().contains("program focused"));
      assertFalse(extraction.text().contains("Attended a"));
      try (var document = Loader.loadPDF(bytes)) {
        String content = new String(document.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
        assertFalse(content.isBlank());
        boolean italic = false;
        for (var name : document.getPage(0).getResources().getFontNames()) {
          String font = document.getPage(0).getResources().getFont(name).getName().toLowerCase();
          italic |= font.contains("italic") || font.contains("oblique");
        }
        assertTrue(italic, "PDF embeds an italic font for credential names");
        String preview = System.getProperty("education.preview.output");
        if (preview != null && placement.equals("inline"))
          javax.imageio.ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 110), "png", Path.of(preview).toFile());
      }
      String xml = "";
      try (var zip = new ZipInputStream(new ByteArrayInputStream(new DocxService().create(resume, "Senior", "Calibri", placement, "blue")))) {
        for (var part = zip.getNextEntry(); part != null; part = zip.getNextEntry())
          if (part.getName().equals("word/document.xml")) xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
      }
      assertTrue(xml.contains("Bachelor's in Software Engineering"));
      assertTrue(xml.contains("<w:i/></w:rPr><w:t xml:space=\"preserve\">Bachelor's in Software Engineering"));
      assertFalse(xml.contains("Completed technical"));
      assertFalse(xml.contains("program focused"));
    }
    assertEquals(SOURCE, resume.path("education").asText(), "Do not mutate the original uploaded data");
  }
}
