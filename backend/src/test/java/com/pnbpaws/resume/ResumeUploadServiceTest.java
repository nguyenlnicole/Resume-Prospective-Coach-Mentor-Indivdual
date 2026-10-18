package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ResumeUploadServiceTest {
  private final PdfService pdf = mock(PdfService.class);
  private final AiService ai = mock(AiService.class);
  private final ResumeUploadService uploads = new ResumeUploadService(pdf, ai);

  @Test
  void wordDocumentCanBePreviewedAndReviewedWithoutInventingPageCount() throws Exception {
    byte[] bytes;
    try (var document = new XWPFDocument(); var out = new ByteArrayOutputStream()) {
      document.createParagraph().createRun().setText("Taylor Morgan — Customer Service Representative");
      document.createParagraph().createRun().setText(
          "Helped customers resolve account questions, coordinated with teammates, and documented follow-up steps for recurring issues.");
      document.write(out);
      bytes = out.toByteArray();
    }
    var preview = uploads.preview("resume.docx", bytes);
    assertEquals("text", preview.get("previewType"));
    assertEquals(0, preview.get("pageCount"));
    assertTrue(preview.get("textPreview").toString().contains("Customer Service"));
    var extracted = uploads.extract("resume.docx", bytes);
    assertEquals(0, extracted.pages());
    assertTrue(extracted.text().contains("documented follow-up steps"));
  }

  @Test
  void wordDocumentKeepsHiddenProjectLinkAndItsHeading() throws Exception {
    byte[] bytes;
    try (var document = new XWPFDocument(); var out = new ByteArrayOutputStream()) {
      var project = document.createParagraph();
      project.createRun().setText("CareTrack | ");
      project.createHyperlinkRun("https://github.com/example/caretrack").setText("GitHub");
      document.createParagraph().createRun().setText(
          "Built a care tracking application with Java and Spring Boot to help teams manage tasks and review progress across the full workflow.");
      document.write(out);
      bytes = out.toByteArray();
    }
    var extracted = uploads.extract("resume.docx", bytes);
    assertTrue(extracted.links().stream().anyMatch(link ->
        "https://github.com/example/caretrack".equals(link.get("url"))
            && link.get("context").toString().contains("CareTrack")));
  }

  @Test
  void imageResumeIsPreviewedAndItsReadableTextIsReviewed() throws Exception {
    var image = new BufferedImage(600, 800, BufferedImage.TYPE_INT_RGB);
    byte[] bytes;
    try (var out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      bytes = out.toByteArray();
    }
    when(ai.readResumeImage(anyString())).thenReturn(
        "Taylor Morgan — Customer Service Representative. Helped customers solve account questions and documented follow-up steps across the service team.");
    var preview = uploads.preview("resume.png", bytes);
    assertEquals(1, preview.get("pageCount"));
    assertEquals(1, ((java.util.List<?>) preview.get("pages")).size());
    var extracted = uploads.extract("resume.png", bytes);
    assertEquals(1, extracted.pages());
    assertTrue(extracted.text().contains("Taylor Morgan"));
  }

  @Test
  void rejectsRenamedFilesWithUnrelatedContent() {
    assertThrows(ResponseStatusException.class,
        () -> uploads.preview("resume.docx", "not a Word file".getBytes()));
  }
}
