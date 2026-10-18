package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailServiceTest {
  @Test void signInCodesUseConfiguredSmtpSender() throws Exception {
    class CapturingSender extends JavaMailSenderImpl {
      MimeMessage sent;
      @Override public void send(MimeMessage message) { sent = message; }
    }
    var sender = new CapturingSender();
    var mail = new MailService(sender, true, "onboarding@resend.dev", "unused-resend-key", true, "owner@gmail.com");

    mail.text("coach@example.com", "Your sign-in code", "Enter this code: 123456");

    assertNotNull(sender.sent);
    assertEquals("owner@gmail.com", sender.sent.getFrom()[0].toString());
    assertEquals("coach@example.com", sender.sent.getRecipients(jakarta.mail.Message.RecipientType.TO)[0].toString());
    assertEquals("Your sign-in code", sender.sent.getSubject());
  }
  @Test void helpEmailContainsPdfAndEditableWordWithReplyAddress() throws Exception {
    class CapturingSender extends JavaMailSenderImpl {
      MimeMessage sent;
      @Override public void send(MimeMessage message) { sent = message; }
    }
    var sender = new CapturingSender();
    var mail = new MailService(sender, true, "owner@example.com");
    mail.sendAttachments("nicole@example.com", "Resume help", "Word download: https://example.com/private", "tester@example.com",
        java.util.List.of(new MailService.Attachment("resume.pdf", new byte[]{1,2}, "application/pdf"),
            new MailService.Attachment("resume.docx", new byte[]{3,4}, HelpDocuments.DOCX_TYPE)));
    assertEquals("tester@example.com", sender.sent.getReplyTo()[0].toString());
    var multipart = (jakarta.mail.Multipart)sender.sent.getContent();
    var filenames = new java.util.HashSet<String>();
    for (int i=0; i<multipart.getCount(); i++) {
      var part = multipart.getBodyPart(i);
      if (part.getFileName() != null) filenames.add(part.getFileName());
    }
    assertEquals(java.util.Set.of("resume.pdf", "resume.docx"), filenames);
  }

  @Test void resumePdfUsesWorkingSmtpWhenItIsConfiguredForSignIn() throws Exception {
    class CapturingSender extends JavaMailSenderImpl {
      MimeMessage sent;
      @Override public void send(MimeMessage message) { sent = message; }
    }
    var sender = new CapturingSender();
    var mail = new MailService(sender, true, "onboarding@resend.dev", "unused-resend-key", true, "owner@gmail.com");
    mail.send("tester@example.com", "Your resume", "Attached", new byte[]{1,2}, null, "resume.pdf");
    assertNotNull(sender.sent);
    assertEquals("owner@gmail.com", sender.sent.getFrom()[0].toString());
    assertEquals("tester@example.com", sender.sent.getRecipients(jakarta.mail.Message.RecipientType.TO)[0].toString());
    var multipart = (jakarta.mail.Multipart)sender.sent.getContent();
    assertEquals("resume.pdf", multipart.getBodyPart(1).getFileName());
  }

  @Test void disabledMailCannotReportSuccess() {
    var mail = new MailService(null, false, "owner@example.com");
    assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> mail.send("tester@example.com", "Resume", "Attached", new byte[]{1}, null, "resume.pdf"));
  }
}
