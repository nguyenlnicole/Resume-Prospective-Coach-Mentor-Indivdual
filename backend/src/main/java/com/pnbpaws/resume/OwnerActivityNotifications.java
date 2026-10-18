package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.Executor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
class OwnerActivityNotifications {
  private static final Logger log = LoggerFactory.getLogger(OwnerActivityNotifications.class);
  private final MailService mail;
  private final String ownerInbox;
  private final Executor executor;

  OwnerActivityNotifications(MailService mail,
      @Qualifier("ownerNotificationExecutor") Executor executor,
      @Value("${app.review-email}") String ownerInbox) {
    this.mail = mail;
    this.executor = executor;
    this.ownerInbox = ownerInbox;
  }

  static String rating(JsonNode score) {
    if (!score.isObject() || !score.hasNonNull("display")) return "Unavailable";
    String display = score.path("display").asText() + "/10";
    if (score.path("unverified").asInt() == 0) return display;
    return display + " (provisional; possible range "
        + score.path("minimum").asText() + "–" + score.path("maximum").asText() + "/10)";
  }

  void reviewed(String email, String coachEmail, String level, String formatRating,
      String overallRating) {
    notifyOwner("Resume rated — Resume Prospective",
        "Someone completed a resume review.\n\n"
            + "User email: " + email + "\n"
            + coachLine(coachEmail)
            + "Career level: " + level + "\n"
            + "Format rating: " + formatRating + "\n"
            + "Overall rating: " + overallRating + "\n\n"
            + "No resume file or content is attached.", email);
  }

  void rerated(String email, String coachEmail, String level, String formatRating,
      String overallRating) {
    notifyOwner("Finished resume re-rated — Resume Prospective",
        "Someone rated a finished resume before choosing a version.\n\n"
            + "User email: " + email + "\n"
            + coachLine(coachEmail)
            + "Career level: " + level + "\n"
            + "Format rating: " + formatRating + "\n"
            + "Overall rating: " + overallRating + "\n\n"
            + "No resume file or content is attached.", email);
  }

  void builderStarted(String email, String coachEmail, String level, String formatRating,
      String overallRating) {
    notifyOwner("Resume builder started — Resume Prospective",
        "Someone started the resume builder.\n\n"
            + "User email: " + email + "\n"
            + coachLine(coachEmail)
            + "Career level: " + level + "\n"
            + "Format rating: " + formatRating + "\n"
            + "Overall rating: " + overallRating + "\n\n"
            + "No resume file or content is attached.", email);
  }

  void signedIn(String email, String edition) {
    notifyOwner("Resume app sign-in — Resume Prospective",
        edition + " signed in.\n\nEmail: " + email + "\n", email);
  }

  void coachAccessRequested(String email, String approvalLink) {
    if (!canNotify(email)) return;
    queue(() -> mail.textWithAction(ownerInbox, "New coach access request — Resume Prospective",
        "A coach requested access.\n\nCoach email: " + email
            + "\n\nApprove 30-day coach access: " + approvalLink
            + "\n\nThe link expires in seven days. Access is assigned only after you confirm.",
        "Approve Coach or Mentor", approvalLink));
  }

  void betaAccessRequested(String email, String approvalLink) {
    if (!canNotify(email)) return;
    queue(() -> mail.textWithAction(ownerInbox, "New individual access request — Resume Prospective",
        "A user requested access: " + email + "\n\nApprove individual access: " + approvalLink
            + "\n\nThis link expires in seven days. No access is granted until you approve.",
        "Approve Access", approvalLink));
  }

  void betaOriginal(String email, String filename, byte[] bytes, byte[] feedback) {
    if (!canNotify(email) || bytes == null || bytes.length == 0) return;
    String safeName = filename == null ? "uploaded-resume" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    String type = safeName.toLowerCase().endsWith(".pdf") ? "application/pdf"
        : safeName.toLowerCase().endsWith(".docx") ? HelpDocuments.DOCX_TYPE
        : safeName.toLowerCase().endsWith(".doc") ? "application/msword"
        : safeName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
    queue(() -> mail.sendAttachments(ownerInbox, "Resume and AI feedback — Resume Prospective",
        "User: " + email + "\nAttached are the uploaded resume and AI feedback PDF.", null,
        List.of(new MailService.Attachment(safeName, bytes.clone(), type),
            new MailService.Attachment("PNB-Resume-Feedback.pdf", feedback.clone(), "application/pdf"))));
  }

  void coachReview(String email, String coachEmail, String filename, byte[] original,
      byte[] feedback) {
    if (!canNotify(email) || !AdminService.validEmail(coachEmail)
        || original == null || original.length == 0 || feedback == null || feedback.length == 0) return;
    String safeName = filename == null ? "uploaded-resume" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    String type = safeName.toLowerCase().endsWith(".pdf") ? "application/pdf"
        : safeName.toLowerCase().endsWith(".docx") ? HelpDocuments.DOCX_TYPE
        : safeName.toLowerCase().endsWith(".doc") ? "application/msword"
        : safeName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
    queue(() -> mail.sendAttachments(ownerInbox, "Coach resume and AI feedback — Resume Prospective",
        "Resume user: " + email + "\nCoach: " + coachEmail
            + "\nAttached are the uploaded resume and the AI feedback PDF.", null,
        List.of(new MailService.Attachment(safeName, original.clone(), type),
            new MailService.Attachment("PNB-Resume-Feedback.pdf", feedback.clone(), "application/pdf"))));
  }

  void betaResume(String email, String stage, JsonNode resume, String level, String font,
      String dates, String linkColor, PdfService pdf, DocxService docx) {
    if (!canNotify(email) || resume == null || !resume.isObject()) return;
    JsonNode copy = resume.deepCopy();
    queue(() -> {
      String name = ResumeFilename.base(copy) + "-" + stage;
      mail.sendAttachments(ownerInbox, "Resume " + stage + " resume — Resume Prospective",
          "User: " + email + "\nAttached are the " + stage + " resume copies. If an export could not be created, a complete text copy is attached.", null,
          resumeAttachments(name, copy, level, font, dates, linkColor, pdf, docx));
    });
  }

  void coachResume(String email, String coachEmail, String stage, JsonNode resume, String level,
      String font, String dates, String linkColor, PdfService pdf, DocxService docx) {
    if (!canNotify(email) || !AdminService.validEmail(coachEmail)
        || resume == null || !resume.isObject()) return;
    JsonNode copy = resume.deepCopy();
    queue(() -> {
      String name = ResumeFilename.base(copy) + "-" + stage;
      mail.sendAttachments(ownerInbox, "Coach " + stage + " resume — Resume Prospective",
          "Resume user: " + email + "\nCoach: " + coachEmail
              + "\nAttached are the " + stage + " resume copies. If an export could not be created, a complete text copy is attached.", null,
          resumeAttachments(name, copy, level, font, dates, linkColor, pdf, docx));
    });
  }

  private List<MailService.Attachment> resumeAttachments(String name, JsonNode resume,
      String level, String font, String dates, String linkColor, PdfService pdf, DocxService docx) {
    var attachments = new java.util.ArrayList<MailService.Attachment>();
    var unavailable = new java.util.ArrayList<String>();
    try { attachments.add(new MailService.Attachment(name + ".pdf",
        pdf.create(resume, level, font, dates, linkColor), "application/pdf")); }
    catch (RuntimeException e) { unavailable.add("PDF"); }
    try { attachments.add(new MailService.Attachment(name + ".docx",
        docx.create(resume, level, font, dates, linkColor), HelpDocuments.DOCX_TYPE)); }
    catch (RuntimeException e) { unavailable.add("Word"); }
    if (!unavailable.isEmpty()) {
      StringBuilder text = new StringBuilder("Resume copy\n\n")
          .append(String.join(" and ", unavailable))
          .append(" export was unavailable. The complete submitted text follows.\n\n");
      resume.path("contact").fields().forEachRemaining(field -> text.append(field.getKey())
          .append(": ").append(field.getValue().asText()).append("\n"));
      for (String section : List.of("summary", "experience", "skills", "projects", "education", "certifications"))
        text.append("\n").append(section.toUpperCase(java.util.Locale.ROOT)).append("\n")
            .append(resume.path(section).asText()).append("\n");
      attachments.add(new MailService.Attachment(name + ".txt",
          text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), "text/plain"));
      log.warn("Owner resume copy includes text fallback because {} export was unavailable", String.join(" and ", unavailable));
    }
    return attachments;
  }

  void betaFeedback(String email, int stars, int aiStars, String comment) {
    if (!canNotify(email)) return;
    notifyOwner("User rated the app — Resume Prospective",
        "User: " + email + "\nApp rating: " + stars + "/5\n"
            + "AI feedback rating: " + aiStars + "/5\n"
            + (comment.isBlank() ? "" : "Comments: " + comment + "\n"), email);
  }

  void coachAiFeedback(String coachEmail, String resumeEmail, int aiStars, String comment) {
    if (!canNotify(coachEmail)) return;
    notifyOwner("Coach rated AI feedback — Resume Prospective",
        "Coach: " + coachEmail + "\nResume email: " + resumeEmail
            + "\nAI feedback rating: " + aiStars + "/5\nComments: " + comment + "\n", coachEmail);
  }

  private boolean canNotify(String email) {
    return mail.enabled && AdminService.validEmail(email) && AdminService.validEmail(ownerInbox);
  }

  private void queue(Runnable task) {
    try {
      executor.execute(() -> {
        try { task.run(); }
        catch (RuntimeException e) { log.warn("Owner activity email could not be sent: {}", e.getMessage()); }
      });
    } catch (RuntimeException e) {
      log.warn("Owner activity email could not be queued: {}", e.getMessage());
    }
  }

  private static String coachLine(String coachEmail) {
    return AdminService.validEmail(coachEmail) ? "Coach email: " + coachEmail + "\n" : "";
  }

  private void notifyOwner(String subject, String body, String email) {
    if (!canNotify(email))
      return;
    queue(() -> mail.text(ownerInbox, subject, body));
  }
}

@Configuration
class OwnerNotificationConfig {
  @Bean("ownerNotificationExecutor")
  Executor ownerNotificationExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("owner-notification-");
    executor.initialize();
    return executor;
  }
}
