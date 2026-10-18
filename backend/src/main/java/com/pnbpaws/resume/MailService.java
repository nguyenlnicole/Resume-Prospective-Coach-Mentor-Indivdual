package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class MailService {
  final boolean enabled;
  private final JavaMailSender sender;
  private final String from;
  private final String resendKey;
  private final boolean authSmtp;
  private final String authFrom;
  private final java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
  MailService(JavaMailSender sender, boolean enabled, String from) { this(sender, enabled, from, "", false, ""); }

  @org.springframework.beans.factory.annotation.Autowired
  MailService(
      JavaMailSender sender,
      @Value("${app.mail-enabled}") boolean enabled,
      @Value("${app.mail-from}") String from,
      @Value("${app.resend-key:}") String resendKey,
      @Value("${app.auth-mail-smtp:false}") boolean authSmtp,
      @Value("${app.auth-mail-from:}") String authFrom) {
    this.sender = sender;
    this.enabled = enabled;
    this.from = from;
    this.resendKey = resendKey;
    this.authSmtp = authSmtp;
    this.authFrom = authFrom;
  }

  void text(String to, String subject, String body) {
    if (authSmtp) {
      if (!enabled || authFrom.isBlank())
        throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Sign-in email is not connected yet.");
      try {
        var message = sender.createMimeMessage();
        var helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(authFrom);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);
        sender.send(message);
        return;
      } catch (Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        throw new ResponseStatusException(BAD_GATEWAY, "The sign-in email could not be sent. Please try again later.");
      }
    }
    send(to, subject, body, null, null, null);
  }

  void textWithAction(String to, String subject, String body, String label, String url) {
    if (!enabled) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Email delivery is not connected yet.");
    String safeBody = body.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\n", "<br>");
    String html = "<div style=\"font-family:Arial,sans-serif;color:#23476a;line-height:1.5\"><p>"
        + safeBody + "</p><p><a href=\"" + url + "\" style=\"display:inline-block;padding:12px 20px;"
        + "border-radius:20px;background:#dceeff;border:2px solid #4f7fac;color:#183b61;"
        + "font-weight:bold;text-decoration:none\">" + label + "</a></p></div>";
    try {
      if (!authSmtp && !resendKey.isBlank()) {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var payload = mapper.createObjectNode();
        payload.put("from", from); payload.putArray("to").add(to);
        payload.put("subject", subject); payload.put("text", body); payload.put("html", html);
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://api.resend.com/emails"))
            .timeout(java.time.Duration.ofSeconds(20)).header("Authorization", "Bearer " + resendKey)
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload.toString())).build();
        var response = http.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || !mapper.readTree(response.body()).hasNonNull("id"))
          throw new IllegalStateException("Mail provider did not accept the message");
      } else {
        if (authSmtp && authFrom.isBlank()) throw new IllegalStateException("SMTP sender is not configured");
        var message = sender.createMimeMessage();
        var helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(authSmtp ? authFrom : from);
        helper.setTo(to); helper.setSubject(subject); helper.setText(body, html);
        sender.send(message);
      }
    } catch (Exception e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new ResponseStatusException(BAD_GATEWAY, "The mail service could not confirm delivery. Please try again later.");
    }
  }

  record Attachment(String filename, byte[] bytes, String type) {}

  void send(String to, String subject, String body, byte[] pdf, String replyTo, String attachmentName) {
    sendAttachments(to, subject, body, replyTo, pdf == null ? java.util.List.of()
        : java.util.List.of(new Attachment(attachmentName, pdf, "application/pdf")));
  }

  void sendAttachments(String to, String subject, String body, String replyTo, java.util.List<Attachment> attachments) {
    if (!enabled)
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE,
          "Email delivery is not connected yet. Please download your PDF instead.");
    try {
      if (!authSmtp && !resendKey.isBlank()) {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var payload = mapper.createObjectNode();
        payload.put("from", from); payload.putArray("to").add(to);
        payload.put("subject", subject); payload.put("text", body);
        if (replyTo != null) payload.put("reply_to", replyTo);
        if (!attachments.isEmpty()) {
          var files = payload.putArray("attachments");
          for (var file : attachments) {
            var attachment = files.addObject();
            attachment.put("filename", file.filename());
            attachment.put("content_type", file.type());
            attachment.put("content", java.util.Base64.getEncoder().encodeToString(file.bytes()));
          }
        }
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://api.resend.com/emails"))
            .timeout(java.time.Duration.ofSeconds(20)).header("Authorization", "Bearer " + resendKey)
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload.toString())).build();
        var response = http.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || !mapper.readTree(response.body()).hasNonNull("id"))
          throw new IllegalStateException("Mail provider did not accept the message");
        return;
      }
      var message = sender.createMimeMessage();
      var helper = new MimeMessageHelper(message, !attachments.isEmpty(), "UTF-8");
      helper.setFrom(authSmtp ? authFrom : from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(body, false);
      if (replyTo != null) helper.setReplyTo(replyTo);
      for (var attachment : attachments)
        helper.addAttachment(attachment.filename(), new ByteArrayResource(attachment.bytes()), attachment.type());
      sender.send(message);
    } catch (Exception e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new ResponseStatusException(
          BAD_GATEWAY, "The mail service could not confirm delivery. Please try again later.");
    }
  }
}
