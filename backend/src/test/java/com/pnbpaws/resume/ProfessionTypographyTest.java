package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class ProfessionTypographyTest {
  @Test void approvedProfessionReferencesUseTheirOwnTypographyInBothExports() throws Exception {
    var mapper = new ObjectMapper();
    var pdf = new PdfService(System.getProperty("pdf.font", "C:/Windows/Fonts/calibri.ttf"), System.getProperty("pdf.bold", "C:/Windows/Fonts/calibrib.ttf"));
    for (String[] item : new String[][] {{"General", "general"}, {"Medical Field", "medical"}, {"Educator", "educator"}}) {
      var resume = mapper.readTree(Path.of("../frontend/src/" + item[1] + "-reference-resume.json").toFile());
      byte[] bytes = pdf.create(resume, item[0], null, "right");
      var extracted = pdf.extract(bytes);
      assertTrue(extracted.typography().stream().anyMatch(t -> t.get("fontAndSize").toString().contains("24")));
      assertTrue(extracted.lines().stream().allMatch(line -> ((Number)line.get("leftPt")).floatValue() >= 53 && ((Number)line.get("rightPt")).floatValue() <= 559));
      byte[] docx = new DocxService().create(resume, item[0], null, "right", "blue");
      String xml = "";
      try (var zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
        for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
          if (entry.getName().equals("word/document.xml")) xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
      }
      assertTrue(xml.contains("w:top=\"1080\" w:right=\"1080\" w:bottom=\"1080\" w:left=\"1080\""));
      assertTrue(xml.contains("w:ascii=\"Arial\""));
      assertTrue(xml.contains("w:sz w:val=\"22\""));
      if (item[0].equals("Educator")) assertTrue(xml.contains("TEACHING CREDENTIALS &amp; CERTIFICATIONS"));
      if (System.getProperty("profession.preview.directory") != null) {
        Path directory = Path.of(System.getProperty("profession.preview.directory"));
        Files.createDirectories(directory);
        Files.write(directory.resolve(item[1] + ".pdf"), bytes);
        Files.write(directory.resolve(item[1] + ".docx"), docx);
      }
    }
  }
}
