package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class OwnerActivityNotificationsTest {
  record Notice(String to, String subject, String body) {}

  @Test void sendsSeparateOwnerEmailsForRatingAndBuilderStart() {
    List<Notice> sent = new ArrayList<>();
    MailService mail = new MailService(null, true, "sender@example.com") {
      @Override void text(String to, String subject, String body) {
        sent.add(new Notice(to, subject, body));
      }
    };
    var notifications = new OwnerActivityNotifications(mail, Runnable::run, "resume.prospective@pnbpaws.com");

    notifications.reviewed("person@example.com", "coach@example.com", "Senior", "8/10", "7/10");
    notifications.builderStarted("person@example.com", "coach@example.com", "Senior", "8/10", "7/10");

    assertEquals(2, sent.size());
    assertEquals("resume.prospective@pnbpaws.com", sent.get(0).to());
    assertTrue(sent.get(0).subject().contains("rated"));
    assertTrue(sent.get(0).body().contains("person@example.com"));
    assertTrue(sent.get(0).body().contains("Coach email: coach@example.com"));
    assertTrue(sent.get(0).body().contains("Format rating: 8/10"));
    assertTrue(sent.get(0).body().contains("Overall rating: 7/10"));
    assertTrue(sent.get(1).subject().contains("builder started"));
    assertFalse(sent.get(1).body().contains("attachment"));
  }

  @Test void provisionalScoreIncludesItsPossibleRange() throws Exception {
    var score = new ObjectMapper().readTree("""
        {"display":"6.5","minimum":6.5,"maximum":8.5,"unverified":2}
        """);
    assertEquals("6.5/10 (provisional; possible range 6.5–8.5/10)",
        OwnerActivityNotifications.rating(score));
  }

  @Test void signInNoticeIdentifiesTheUserAndEdition() {
    List<Notice> sent = new ArrayList<>();
    MailService mail = new MailService(null, true, "sender@example.com") {
      @Override void text(String to, String subject, String body) {
        sent.add(new Notice(to, subject, body));
      }
    };
    var notifications = new OwnerActivityNotifications(mail, Runnable::run, "resume.prospective@pnbpaws.com");
    notifications.signedIn("tester@example.com", "User");
    assertEquals(1, sent.size());
    assertEquals("resume.prospective@pnbpaws.com", sent.getFirst().to());
    assertTrue(sent.getFirst().body().contains("User signed in"));
    assertTrue(sent.getFirst().body().contains("tester@example.com"));
  }

  @Test void betaAndCoachReviewsSendDocumentsToTheOwner() throws Exception {
    List<List<MailService.Attachment>> copies = new ArrayList<>();
    List<Notice> feedback = new ArrayList<>();
    MailService mail = new MailService(null, true, "sender@example.com") {
      @Override void sendAttachments(String to, String subject, String body, String replyTo,
          List<Attachment> attachments) {
        assertEquals("resume.prospective@pnbpaws.com", to);
        copies.add(attachments);
      }
      @Override void text(String to, String subject, String body) {
        feedback.add(new Notice(to, subject, body));
      }
    };
    var notifications = new OwnerActivityNotifications(mail, Runnable::run, "resume.prospective@pnbpaws.com");
    notifications.betaOriginal("tester@example.com", "original.pdf", new byte[]{1, 2}, new byte[]{9});
    assertEquals("original.pdf", copies.getFirst().getFirst().filename());
    assertEquals("application/pdf", copies.getFirst().getFirst().type());
    assertEquals("PNB-Resume-Feedback.pdf", copies.getFirst().get(1).filename());
    notifications.coachReview("candidate@example.com", "coach@example.com", "resume.docx",
        new byte[]{5}, new byte[]{6});
    assertEquals("resume.docx", copies.get(1).getFirst().filename());
    assertEquals("PNB-Resume-Feedback.pdf", copies.get(1).get(1).filename());
    JsonNode resume = new ObjectMapper().readTree("{\"contact\":{\"name\":\"Test Person\"}}");
    PdfService pdf = mock(PdfService.class);
    DocxService docx = mock(DocxService.class);
    when(pdf.create(resume, "Senior", "Calibri", "right", "blue")).thenReturn(new byte[]{3});
    when(docx.create(resume, "Senior", "Calibri", "right", "blue")).thenReturn(new byte[]{4});
    notifications.betaResume("tester@example.com", "draft", resume, "Senior", "Calibri", "right", "blue", pdf, docx);
    assertEquals(2, copies.get(2).size());
    assertTrue(copies.get(2).getFirst().filename().endsWith("-draft.pdf"));
    notifications.coachResume("candidate@example.com", "coach@example.com", "final", resume,
        "Senior", "Calibri", "right", "blue", pdf, docx);
    assertEquals(2, copies.get(3).size());
    assertTrue(copies.get(3).getFirst().filename().endsWith("-final.pdf"));
    notifications.betaFeedback("tester@example.com", 0, 5, "Needs clearer steps");
    notifications.coachAiFeedback("coach@example.com", "resume@example.com", 4, "Useful feedback");
    assertTrue(feedback.get(0).body().contains("App rating: 0/5"));
    assertTrue(feedback.get(0).body().contains("AI feedback rating: 5/5"));
    assertTrue(feedback.get(1).body().contains("Coach: coach@example.com"));
    assertTrue(feedback.get(1).body().contains("Resume email: resume@example.com"));
  }

  @Test void failedPdfStillDeliversWordAndCompleteResumeText() throws Exception {

    List<MailService.Attachment> sent = new ArrayList<>();
    MailService enabled = new MailService(null, true, "sender@example.com") {
      @Override void sendAttachments(String to, String subject, String body, String replyTo,
          List<Attachment> attachments) { sent.addAll(attachments); }
    };
    var resume = new ObjectMapper().readTree("{\"contact\":{\"name\":\"Test Person\"},\"experience\":\"All original contributions\"}");
    PdfService pdf = mock(PdfService.class);
    DocxService docx = mock(DocxService.class);
    when(pdf.create(resume, "Junior", "Calibri", "right", "blue"))
        .thenThrow(new IllegalArgumentException("Too long"));
    when(docx.create(resume, "Junior", "Calibri", "right", "blue")).thenReturn(new byte[]{4});
    new OwnerActivityNotifications(enabled, Runnable::run, "owner@example.com")
        .betaResume("person@example.com", "draft", resume, "Junior", "Calibri", "right", "blue", pdf, docx);
    assertEquals(2, sent.size());
    assertTrue(sent.stream().anyMatch(a -> a.filename().endsWith(".docx")));
    String text = new String(sent.stream().filter(a -> a.filename().endsWith(".txt")).findFirst().orElseThrow().bytes(),
        java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(text.contains("All original contributions"));
    assertTrue(text.contains("Test Person"));
    assertTrue(text.contains("PDF export was unavailable"));
  }

  @Test void mailFailureDoesNotFailTheReviewOrBuilderStart() {
    MailService mail = new MailService(null, true, "sender@example.com") {
      @Override void text(String to, String subject, String body) {
        throw new IllegalStateException("mail unavailable");
      }
    };
    var notifications = new OwnerActivityNotifications(mail, Runnable::run, "resume.prospective@pnbpaws.com");
    assertDoesNotThrow(() -> notifications.reviewed("person@example.com", "", "Senior", "8/10", "7/10"));
    assertDoesNotThrow(() -> notifications.builderStarted("person@example.com", "", "Senior", "8/10", "7/10"));
  }
}
