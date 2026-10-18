package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class JobDescriptionServiceTest {
  @Test
  void readsWordJobListing() throws Exception {
    byte[] bytes;
    try (var document = new XWPFDocument(); var out = new ByteArrayOutputStream()) {
      document.createParagraph().createRun().setText("Software Engineer");
      document.createParagraph().createRun().setText(
          "Build Java services, write tests, and collaborate with product teams.");
      document.write(out);
      bytes = out.toByteArray();
    }
    String text = new JobDescriptionService(mock(AiService.class)).fromFile("job.docx", bytes);
    assertTrue(text.contains("Software Engineer"));
    assertTrue(text.contains("Build Java services"));
  }
}
