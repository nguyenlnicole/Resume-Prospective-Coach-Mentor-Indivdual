package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class DocxServiceTest {
  @Test void medicalExportIncludesSeparateRotationsAndLicensure() throws Exception {
    var resume = (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper()
        .readTree(getClass().getResourceAsStream("/approved-reference.json"));
    resume.put("clinicalRotations", "Clinical Rotations\nExample Hospital | Pediatrics | 2025\nSupervised student rotation, 80 hours.");
    byte[] bytes = new DocxService().create(resume, "Medical Field", "Arial", "right", "blue");
    String document = "";
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      for (var part = zip.getNextEntry(); part != null; part = zip.getNextEntry())
        if (part.getName().equals("word/document.xml")) document = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertTrue(document.contains("CLINICAL ROTATIONS"));
    assertTrue(document.contains("Supervised student rotation, 80 hours."));
    assertTrue(document.contains("LICENSURE AND CERTIFICATIONS"));
  }
  @Test
  void wordExportKeepsReferenceLayoutAndClickableLinks() throws Exception {
    var resume = new ObjectMapper().readTree(getClass().getResourceAsStream("/approved-reference.json"));
    byte[] bytes = new DocxService().create(resume, "Senior", "Calibri", "right", "dark");
    assertEquals('P', bytes[0]);
    assertEquals('K', bytes[1]);
    String document = "", relationships = "";
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      for (var part = zip.getNextEntry(); part != null; part = zip.getNextEntry()) {
        String value = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
        if (part.getName().equals("word/document.xml")) document = value;
        if (part.getName().equals("word/_rels/document.xml.rels")) relationships = value;
      }
    }
    assertTrue(document.contains("Cedar Example Labs"));
    assertTrue(document.contains("Lead Engineer"));
    assertTrue(document.contains("Meadow Example Studio"));
    assertTrue(document.contains("<w:tab/>"));
    assertTrue(document.contains("Java, Spring Boot, React"));
    assertTrue(relationships.contains("https://github.com/example/schedule"));
    assertTrue(relationships.contains("https://www.linkedin.com/in/example"));
  }
}
