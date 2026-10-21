package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
class ApiController {
  @org.springframework.beans.factory.annotation.Autowired RegisteredAccounts accounts;
  private final AdminService admin;
  private final MailService mail;
  private final PdfService pdf;
  private final FeedbackPdfService feedbackPdf;
  private final ResumeUploadService uploads;
  private final DocxService docx;
  private final AiService ai;
  private final JobDescriptionService jobs;
  private final HelpDocuments documents;
  private final CoachAccess access;
  private final OwnerActivityNotifications ownerNotifications;
  private final CoachReviewHistory coachHistory;

  ApiController(AdminService a, MailService m, PdfService p, FeedbackPdfService feedbackPdf, ResumeUploadService uploads,
      DocxService d, AiService i, JobDescriptionService j, HelpDocuments documents,
      CoachAccess access, OwnerActivityNotifications ownerNotifications, CoachReviewHistory coachHistory) {
    admin = a;
    mail = m;
    pdf = p;
    this.feedbackPdf = feedbackPdf;
    this.uploads = uploads;
    docx = d;
    ai = i;
    jobs = j;
    this.documents = documents;
    this.access = access;
    this.ownerNotifications = ownerNotifications;
    this.coachHistory = coachHistory;
  }

  static final List<String> LEVELS = List.of("Entry / Junior", "Mid-level", "Senior", "Management", "General", "Normal", "Medical Field", "Educator");

  static void level(String s) {
    if (!LEVELS.contains(s))
      throw new ResponseStatusException(BAD_REQUEST, "Select a valid career level.");
  }

  @GetMapping("/csrf")
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken());
  }

  @GetMapping("/status")
  Map<String, Object> status() {
    return Map.of(
        "aiReady",
        ai.enabled(),
        "emailReady",
        mail.enabled,
        "adminReady",
        admin.configured(),
        "scoringFinalized",
        true,
        "scoringVersion",
        ResumeScoring.VERSION);
  }

  record Login(String email, String code) {}

  record Email(String email) {}


  record Recipients(List<String> recipients) {}

  @PostMapping("/admin/code")
  Map<String, Boolean> code(@RequestBody Email input, HttpServletRequest req) {
    admin.limit("admin-code-ip:" + req.getRemoteAddr(), 10);
    admin.sendCode(input.email());
    return Map.of("ok", true);
  }

  @PostMapping("/admin/verify")
  Map<String, Object> login(@RequestBody Login input, HttpServletRequest req) {
    admin.limit("admin-verify:" + req.getRemoteAddr(), 15);
    Integer v = admin.verifyCode(input.email(), input.code());
    if (v == null) throw new ResponseStatusException(UNAUTHORIZED, "This code is invalid or expired. Request a new code if needed.");
    req.getSession();
    req.changeSessionId();
    req.getSession().setMaxInactiveInterval(30 * 60);
    req.getSession().removeAttribute("coachEmail");
    req.getSession().removeAttribute("coachToken");
    req.getSession().removeAttribute("reviewedLevel");
    req.getSession().setAttribute("adminVersion", v);
    return adminInfo();
  }

  @GetMapping("/admin/me")
  Map<String, Object> me(HttpSession s) {
    requireAdmin(s);
    return adminInfo();
  }

  Map<String, Object> adminInfo() {
    return Map.of(
        "email", admin.email(), "recipients", admin.recipients(), "emailReady", mail.enabled);
  }

  void requireAdmin(HttpSession session) {
    if (!admin.configured()
        || !Objects.equals(session.getAttribute("adminVersion"), admin.version()))
      throw new ResponseStatusException(UNAUTHORIZED, "Please sign in to continue.");
  }

  @PostMapping("/admin/logout")
  Map<String, Boolean> logout(HttpSession session) {
    session.invalidate();
    return Map.of("ok", true);
  }

  @PostMapping("/admin/recipients")
  Map<String, Object> recipients(@RequestBody Recipients in, HttpSession session) {
    requireAdmin(session);
    if (in.recipients() == null)
      throw new ResponseStatusException(BAD_REQUEST, "Provide email recipients.");
    admin.recipients(in.recipients());
    return adminInfo();
  }

  @PostMapping(value = "/resume/upload-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<Map<String, Object>> uploadPreview(
      @RequestParam MultipartFile file, HttpServletRequest req) throws Exception {
    admin.limit("upload-preview:" + req.getRemoteAddr(), 30);
    var preview = uploads.preview(file.getOriginalFilename(), file.getBytes());
    return ResponseEntity.ok().header("Cache-Control", "no-store")
        .body(preview);
  }

  @PostMapping(value = "/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  JsonNode review(
      @RequestParam MultipartFile file,
      @RequestParam String level,
      @RequestParam String email,
      @RequestParam boolean consent,
      HttpServletRequest req)
      throws Exception {
    return reviewBytes(file.getBytes(), file.getOriginalFilename(), level, email, consent, req);
  }

  private JsonNode reviewBytes(byte[] uploadedBytes, String filename, String level, String email, boolean consent, HttpServletRequest req) throws Exception {
    if (!consent)
      throw new ResponseStatusException(BAD_REQUEST, "Confirm external resume processing before submitting.");
    level(level);
    if (!AdminService.validEmail(email))
      throw new ResponseStatusException(BAD_REQUEST, "Enter a valid resume user email.");
    admin.limit("review:" + req.getRemoteAddr(), 6);
    JsonNode result = accounts.runTrial(req.getSession(), "review", level, email.trim(), () -> ai.review(uploads.extract(filename, uploadedBytes), level));
    req.getSession().setAttribute("reviewedLevel", level);
    req.getSession().setAttribute("reviewUserEmail", email.trim());
    req.getSession().setAttribute("reviewResult", result);
    req.getSession().removeAttribute("savedReviewId");
    Map<String, Object> account = access.state(req.getSession());
    String coachEmail = "coach".equals(account.get("role"))
        && !"beta-pass".equals(account.get("accessKind"))
        ? Objects.toString(account.get("email"), "") : "";
    req.getSession().setAttribute("reviewCoachEmail", coachEmail);
    req.getSession().removeAttribute("coachHistoryId");
    if (!coachEmail.isBlank()) {
      String firstName = result.path("resume").path("contact").path("name").asText("");
      byte[] feedback = feedbackPdf.create(result, level);
      String id = coachHistory.saveReview(coachEmail, email.trim(), firstName,
          filename, uploadedBytes, feedback);
      req.getSession().setAttribute("coachHistoryId", id);
      ((com.fasterxml.jackson.databind.node.ObjectNode) result).put("historyId", id);
      ownerNotifications.coachReview(email.trim(), coachEmail, filename,
          uploadedBytes, feedback);
    }
    req.getSession().removeAttribute("builderStartNotified");
    req.getSession().removeAttribute("betaCopy:draft");
    req.getSession().removeAttribute("betaCopy:final");
    req.getSession().removeAttribute("coachCopy:draft");
    req.getSession().removeAttribute("coachCopy:final");
    req.getSession().removeAttribute("betaAppRated");
    req.getSession().removeAttribute("coachAiRated");
    String formatRating = OwnerActivityNotifications.rating(result.path("formatRating"));
    String overallRating = OwnerActivityNotifications.rating(result.path("overallRating"));
    req.getSession().setAttribute("reviewFormatRating", formatRating);
    req.getSession().setAttribute("reviewOverallRating", overallRating);
    if (!coachEmail.isBlank()) coachHistory.startProgress(coachEmail,
        result.path("historyId").asText(), result, level, email.trim(), req.getSession());
    ownerNotifications.reviewed(email.trim(), coachEmail, level, formatRating, overallRating);
    if ("beta-pass".equals(account.get("accessKind")))
      ownerNotifications.betaOriginal(email.trim(), filename, uploadedBytes,
          feedbackPdf.create(result, level));
    return result;
  }

  record CreateResumeInput(JsonNode resume, String level, String email, boolean consent) {}

  @PostMapping("/resume/create")
  JsonNode createResume(@RequestBody CreateResumeInput input, HttpServletRequest req) throws Exception {
    level(input.level());
    if (!input.consent() || !AdminService.validEmail(input.email()))
      throw new ResponseStatusException(BAD_REQUEST, "Enter a valid email and confirm external processing.");
    if (input.resume() == null || !input.resume().isObject() || input.resume().toString().length() > 25000
        || input.resume().path("contact").path("name").asText().isBlank())
      throw new ResponseStatusException(BAD_REQUEST, "Complete your name and keep your resume details under 25,000 characters.");
    admin.limit("create-resume:" + req.getRemoteAddr(), 6);
    byte[] document = pdf.create(input.resume(), input.level(), "Calibri", "right", "blue");
    return reviewBytes(document, "New-Resume.pdf", input.level(), input.email(), input.consent(), req);
  }

  @PostMapping("/review/email-feedback")
  Map<String, Boolean> emailFeedback(HttpServletRequest req) {
    var session = req.getSession(false);
    if (session == null || !(session.getAttribute("reviewResult") instanceof JsonNode result))
      throw new ResponseStatusException(UNAUTHORIZED, "Review a resume before emailing feedback.");
    String email = Objects.toString(session.getAttribute("reviewUserEmail"), "");
    String level = Objects.toString(session.getAttribute("reviewedLevel"), "");
    if (!AdminService.validEmail(email) || !LEVELS.contains(level))
      throw new ResponseStatusException(UNAUTHORIZED, "Review a resume before emailing feedback.");
    admin.limit("feedback-email:" + req.getRemoteAddr(), 8);
    byte[] report = feedbackPdf.create(result, level);
    mail.send(email, "Your Resume Prospective feedback",
        "Your resume feedback is attached as a PDF. It starts with what is working well, followed by your scores, detailed findings, and practical next steps.",
        report, null, "PNB-Resume-Feedback.pdf");
    return Map.of("ok", true);
  }

  record FinishedReview(JsonNode resume, String level, String font, String dates, String linkColor) {}

  @PostMapping("/review/finished")
  JsonNode reviewFinished(@RequestBody FinishedReview input, HttpServletRequest req) {
    var session = req.getSession();
    buildLevel(input.level(), session.getAttribute("reviewedLevel"));
    if (input.resume() == null || !input.resume().isObject()
        || input.resume().toString().length() > 25_000)
      throw new ResponseStatusException(BAD_REQUEST, "Finish your resume before rating it again.");
    admin.limit("review-finished:" + req.getRemoteAddr(), 6);
    byte[] rendered = pdf.create(input.resume(), input.level(), input.font(), input.dates(), input.linkColor());
    JsonNode result = ai.review(pdf.extract(rendered), input.level());
    String email = Objects.toString(session.getAttribute("reviewUserEmail"), "");
    if (AdminService.validEmail(email)) {
      Map<String, Object> account = access.state(session);
      String coachEmail = "coach".equals(account.get("role"))
          && !"beta-pass".equals(account.get("accessKind"))
          ? Objects.toString(account.get("email"), "") : "";
      ownerNotifications.rerated(email, coachEmail, input.level(),
          OwnerActivityNotifications.rating(result.path("formatRating")),
          OwnerActivityNotifications.rating(result.path("overallRating")));
    }
    return result;
  }

  record BetaResumeCopy(JsonNode resume, String level, String font, String dates,
      String linkColor, String stage) {}

  @PostMapping("/review/owner-copy")
  Map<String, Boolean> coachResumeCopy(@RequestBody BetaResumeCopy input, HttpServletRequest req) {
    var session = req.getSession();
    Map<String, Object> account = access.state(session);
    if (!"coach".equals(account.get("role")) || "beta-pass".equals(account.get("accessKind"))
        || !Boolean.TRUE.equals(account.get("allowed")))
      throw new ResponseStatusException(FORBIDDEN, "This is available only in the coach version.");
    String email = Objects.toString(session.getAttribute("reviewUserEmail"), "");
    if (!AdminService.validEmail(email))
      throw new ResponseStatusException(CONFLICT, "Complete a resume rating first.");
    if (!Objects.equals(input.level(), session.getAttribute("reviewedLevel")))
      throw new ResponseStatusException(CONFLICT, "Finish the current review first.");
    if (!List.of("draft", "final").contains(input.stage()) || input.resume() == null
        || !input.resume().isObject() || input.resume().toString().length() > 25_000)
      throw new ResponseStatusException(BAD_REQUEST, "Choose a valid resume copy.");
    String fingerprint = AdminService.digest(input.resume().toString() + input.stage());
    String key = "coachCopy:" + input.stage();
    if (fingerprint.equals(session.getAttribute(key))) return Map.of("ok", true);
    admin.limit("coach-copy:" + req.getRemoteAddr(), 12);
    ownerNotifications.coachResume(email, Objects.toString(account.get("email"), ""),
        input.stage(), input.resume(), input.level(), input.font(), input.dates(),
        input.linkColor(), pdf, docx);
    session.setAttribute(key, fingerprint);
    return Map.of("ok", true);
  }

  @PostMapping("/beta/resume-copy")
  Map<String, Boolean> betaResumeCopy(@RequestBody BetaResumeCopy input, HttpServletRequest req) {
    var session = req.getSession();
    if (!"beta-pass".equals(access.state(session).get("accessKind")))
      throw new ResponseStatusException(FORBIDDEN, "This is available only in the individual version.");
    String email = Objects.toString(session.getAttribute("reviewUserEmail"), "");
    if (!AdminService.validEmail(email))
      throw new ResponseStatusException(CONFLICT, "Complete a resume rating first.");
    if (!Objects.equals(input.level(), session.getAttribute("reviewedLevel")))
      throw new ResponseStatusException(CONFLICT, "Finish the current review first.");
    if (!List.of("draft", "final").contains(input.stage()) || input.resume() == null
        || !input.resume().isObject() || input.resume().toString().length() > 25_000)
      throw new ResponseStatusException(BAD_REQUEST, "Choose a valid resume copy.");
    String fingerprint = AdminService.digest(input.resume().toString() + input.stage());
    String key = "betaCopy:" + input.stage();
    if (fingerprint.equals(session.getAttribute(key))) return Map.of("ok", true);
    admin.limit("beta-copy:" + req.getRemoteAddr(), 12);
    ownerNotifications.betaResume(email, input.stage(), input.resume(), input.level(),
        input.font(), input.dates(), input.linkColor(), pdf, docx);
    session.setAttribute(key, fingerprint);
    return Map.of("ok", true);
  }

  record BetaAppRating(int stars, int aiStars, String comment) {}

  @PostMapping("/beta/app-rating")
  Map<String, Boolean> betaAppRating(@RequestBody BetaAppRating input, HttpServletRequest req) {
    var session = req.getSession();
    if (!"beta-pass".equals(access.state(session).get("accessKind")))
      throw new ResponseStatusException(FORBIDDEN, "This is available only in the individual version.");
    String email = Objects.toString(session.getAttribute("reviewUserEmail"), "");
    if (!AdminService.validEmail(email))
      throw new ResponseStatusException(CONFLICT, "Complete a rating first.");
    if (session.getAttribute("betaAppRated") != null)
      throw new ResponseStatusException(CONFLICT, "Thanks, your rating was already sent.");
    String comment = Objects.toString(input.comment(), "").trim();
    if (input.stars() < 0 || input.stars() > 5 || input.aiStars() < 0 || input.aiStars() > 5
        || comment.isBlank() || comment.length() > 1000)
      throw new ResponseStatusException(BAD_REQUEST, "Choose 0–5 stars for both and add feedback under 1,000 characters.");
    admin.limit("beta-app-rating:" + req.getRemoteAddr(), 5);
    ownerNotifications.betaFeedback(Objects.toString(access.state(session).get("email"), email),
        input.stars(), input.aiStars(), comment);
    session.setAttribute("betaAppRated", true);
    return Map.of("ok", true);
  }

  record CoachAiRating(int aiStars, String comment) {}

  @PostMapping("/coach/ai-rating")
  Map<String, Boolean> coachAiRating(@RequestBody CoachAiRating input, HttpServletRequest req) {
    var session = req.getSession();
    Map<String, Object> account = access.state(session);
    if (!"coach".equals(account.get("role")) || "beta-pass".equals(account.get("accessKind")))
      throw new ResponseStatusException(FORBIDDEN, "This is available only in the coach version.");
    String resumeEmail = Objects.toString(session.getAttribute("reviewUserEmail"), "");
    if (!AdminService.validEmail(resumeEmail))
      throw new ResponseStatusException(CONFLICT, "Complete a resume rating first.");
    if (session.getAttribute("coachAiRated") != null)
      throw new ResponseStatusException(CONFLICT, "Thanks, your feedback was already sent.");
    String comment = Objects.toString(input.comment(), "").trim();
    if (input.aiStars() < 0 || input.aiStars() > 5 || comment.isBlank() || comment.length() > 1000)
      throw new ResponseStatusException(BAD_REQUEST, "Choose 0–5 stars and add feedback under 1,000 characters.");
    admin.limit("coach-ai-rating:" + req.getRemoteAddr(), 5);
    ownerNotifications.coachAiFeedback(Objects.toString(account.get("email"), ""),
        resumeEmail, input.aiStars(), comment);
    session.setAttribute("coachAiRated", true);
    return Map.of("ok", true);
  }

  @PostMapping("/builder/start")
  Map<String, Boolean> builderStarted(HttpServletRequest req) {
    var session = req.getSession();
    String level = Objects.toString(session.getAttribute("reviewedLevel"), "");
    String email = Objects.toString(session.getAttribute("reviewUserEmail"), "");
    if (level.isBlank() || !AdminService.validEmail(email))
      throw new ResponseStatusException(CONFLICT, "Complete a resume rating before starting the builder.");
    if (!Boolean.TRUE.equals(session.getAttribute("builderStartNotified"))) {
      ownerNotifications.builderStarted(email,
          Objects.toString(session.getAttribute("reviewCoachEmail"), ""), level,
          Objects.toString(session.getAttribute("reviewFormatRating"), "Unavailable"),
          Objects.toString(session.getAttribute("reviewOverallRating"), "Unavailable"));
      session.setAttribute("builderStartNotified", true);
    }
    return Map.of("ok", true);
  }

  @PostMapping(value = "/job-description/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  Map<String, String> jobFile(@RequestParam MultipartFile file, HttpServletRequest req) throws Exception {
    admin.limit("job-description:" + req.getRemoteAddr(), 10);
    return Map.of("text", jobs.fromFile(file.getOriginalFilename(), file.getBytes()));
  }

  record FeedbackDetail(JsonNode resume, JsonNode finding, String level) {}

  @PostMapping("/feedback/details")
  Map<String, String> feedbackDetails(@RequestBody FeedbackDetail input, HttpServletRequest req) {
    level(input.level());
    if (input.resume() == null || !input.resume().isObject() ||
        input.finding() == null || !input.finding().isObject() ||
        input.resume().toString().length() > 25_000 ||
        input.finding().toString().length() > 5_000)
      throw new ResponseStatusException(BAD_REQUEST, "This feedback could not be explained. Start a new review.");
    admin.limit("feedback-details:" + req.getRemoteAddr(), 24);
    return Map.of("details", ai.feedbackDetails(input.resume(), input.finding(), input.level()));
  }

  record PositiveFeedback(JsonNode resume, JsonNode findings, String level) {}

  @PostMapping("/feedback/positive")
  Map<String, String> positiveFeedback(@RequestBody PositiveFeedback input, HttpServletRequest req) {
    level(input.level());
    if (input.resume() == null || !input.resume().isObject()
        || input.findings() == null || !input.findings().isArray()
        || input.findings().isEmpty() || input.findings().size() > 20
        || input.resume().toString().length() > 25_000
        || input.findings().toString().length() > 25_000)
      throw new ResponseStatusException(BAD_REQUEST, "Positive feedback is unavailable for this review.");
    for (JsonNode finding : input.findings())
      if (!"pass".equals(finding.path("status").asText()))
        throw new ResponseStatusException(BAD_REQUEST, "Only passing checks can be included.");
    admin.limit("feedback-positive:" + req.getRemoteAddr(), 12);
    return Map.of("feedback", ai.positiveFeedback(input.resume(), input.findings(), input.level()));
  }

  record RoastFeedback(JsonNode resume, JsonNode findings, String level) {}

  @PostMapping("/feedback/roast")
  Map<String, String> roastFeedback(@RequestBody RoastFeedback input, HttpServletRequest req) {
    level(input.level());
    if (input.resume() == null || !input.resume().isObject()
        || input.findings() == null || !input.findings().isArray()
        || input.findings().size() > 25
        || input.resume().toString().length() > 25_000
        || input.findings().toString().length() > 30_000)
      throw new ResponseStatusException(BAD_REQUEST, "A roast is unavailable for this review.");
    admin.limit("feedback-roast:" + req.getRemoteAddr(), 8);
    return Map.of("roast", ai.roastFeedback(input.resume(), input.findings(), input.level()));
  }

  record GlowUpRoast(JsonNode original, JsonNode finished, String level) {}

  @PostMapping("/feedback/glow-up-roast")
  Map<String, List<String>> glowUpRoast(@RequestBody GlowUpRoast input, HttpServletRequest req) {
    level(input.level());
    if (input.original() == null || !input.original().isObject()
        || input.finished() == null || !input.finished().isObject()
        || input.original().toString().length() > 25_000
        || input.finished().toString().length() > 25_000)
      throw new ResponseStatusException(BAD_REQUEST, "Finish your resume before requesting a glow-up roast.");
    admin.limit("feedback-glow-up:" + req.getRemoteAddr(), 8);
    return Map.of("jokes", ai.glowUpRoast(input.original(), input.finished(), input.level()));
  }

  record JobLink(String url) {}

  @PostMapping("/job-description/link")
  Map<String, String> jobLink(@RequestBody JobLink input, HttpServletRequest req) {
    admin.limit("job-description:" + req.getRemoteAddr(), 10);
    return Map.of("text", jobs.fromUrl(input.url()));
  }

  record CoverLetterInput(JsonNode resume, String listing, String suggestions) {}
  @PostMapping("/cover-letter")
  Map<String,String> coverLetter(@RequestBody CoverLetterInput input, HttpServletRequest req) {
    var session=req.getSession(false);
    if(session==null || session.getAttribute("reviewedLevel")==null || !Boolean.TRUE.equals(access.state(session).get("allowed"))) throw new ResponseStatusException(UNAUTHORIZED,"Review a resume first.");
    if(input.resume()==null || !input.resume().isObject() || input.resume().toString().length()>30000 || input.listing()==null || input.listing().isBlank() || input.listing().length()>12000)
      throw new ResponseStatusException(BAD_REQUEST,"Add your finished resume and a job description.");
    admin.limit("cover-letter:"+req.getRemoteAddr(),6);
    if (input.suggestions()!=null && input.suggestions().length()>2000)
      throw new ResponseStatusException(BAD_REQUEST,"Keep your cover letter suggestions under 2,000 characters.");
    return Map.of("text",ai.coverLetter(input.resume(),input.listing(),input.suggestions()));
  }

  record JobPrepInput(String listing, String targetRole) {}

  @PostMapping("/job-prep")
  JsonNode jobPrep(@RequestBody JobPrepInput input, HttpServletRequest req) {
    if (req.getSession(false) == null || req.getSession(false).getAttribute("reviewedLevel") == null)
      throw new ResponseStatusException(UNAUTHORIZED, "Review a resume before making a prep sheet.");
    admin.limit("job-prep:" + req.getRemoteAddr(), 6);
    return ai.jobPrep(input.listing(), input.targetRole());
  }

  static String buildLevel(String requested, Object reviewed) {
    level(requested);
    if (!requested.equals(reviewed))
      throw new ResponseStatusException(
          CONFLICT,
          "Build with the same career level as your review. Start a new review if you want to"
              + " change levels.");
    return requested;
  }

  @PostMapping("/build")
  JsonNode build(@RequestBody JsonNode data, HttpServletRequest req) throws Exception {
    String selected =
        buildLevel(data.path("level").asText(), req.getSession().getAttribute("reviewedLevel"));
    if (data.toString().length() > 60000)
      throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Please shorten the input.");
    if (data.path("jobDescription").asText().length() > 12000)
      throw new ResponseStatusException(BAD_REQUEST, "Shorten the job description to 12,000 characters.");
    if (data.path("targetRole").asText().length() > 200)
      throw new ResponseStatusException(BAD_REQUEST, "Shorten the target role.");
    if (data.path("roleContext").toString().length() > 8_000)
      throw new ResponseStatusException(BAD_REQUEST, "Shorten the additional experience details.");
    admin.limit("build:" + req.getRemoteAddr(), 10);
    return accounts.runTrial(req.getSession(), "build", selected, "", () -> ai.build(data, selected, SectionRewrites.sessionCache(req.getSession())));
  }

  record ResumeInput(JsonNode resume, String level, String font, String dates, String linkColor) {}

  record AlternativeInput(JsonNode resume, String level, String jobDescription,
      String targetRole, JsonNode roleContext, boolean retry) {}

  @PostMapping("/resume/alternative")
  Map<String, JsonNode> alternative(@RequestBody AlternativeInput input, HttpServletRequest req) {
    level(input.level());
    if (input.resume() == null || !input.resume().isObject()
        || input.resume().toString().length() > 25_000)
      throw new ResponseStatusException(BAD_REQUEST, "Please finish your resume before comparing versions.");
    if (input.jobDescription() != null && input.jobDescription().length() > 12_000)
      throw new ResponseStatusException(BAD_REQUEST, "Shorten the job description to 12,000 characters.");
    if (input.targetRole() != null && input.targetRole().length() > 200)
      throw new ResponseStatusException(BAD_REQUEST, "Shorten the target role.");
    if (input.roleContext() != null && input.roleContext().toString().length() > 8_000)
      throw new ResponseStatusException(BAD_REQUEST, "Shorten the additional experience details.");
    admin.limit("alternative:" + req.getRemoteAddr(), 8);
    return Map.of("resume", ai.alternativeResume(input.resume(), input.level(),
        input.jobDescription(), input.targetRole(), input.roleContext(), input.retry() ? new SectionRewrites.Cache() : SectionRewrites.sessionCache(req.getSession())));
  }

  @PostMapping("/resume/pdf")
  ResponseEntity<byte[]> export(@RequestBody ResumeInput input, HttpServletRequest req) {
    level(input.level());
    admin.limit("pdf:" + req.getRemoteAddr(), 20);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=" + ResumeFilename.base(input.resume()) + ".pdf")
        .header("Cache-Control", "no-store")
        .body(pdf.create(input.resume(), input.level(), input.font(), input.dates(), input.linkColor()));
  }

  @PostMapping("/resume/docx")
  ResponseEntity<byte[]> exportWord(@RequestBody ResumeInput input, HttpServletRequest req) {
    level(input.level());
    admin.limit("docx:" + req.getRemoteAddr(), 20);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .header("Content-Disposition", "attachment; filename=" + ResumeFilename.base(input.resume()) + ".docx")
        .header("Cache-Control", "no-store")
        .body(docx.create(input.resume(), input.level(), input.font(), input.dates(), input.linkColor()));
  }

  @PostMapping("/resume/preview")
  ResponseEntity<byte[]> preview(@RequestBody ResumeInput input, HttpServletRequest req) {
    level(input.level());
    admin.limit("preview:" + req.getRemoteAddr(), 120);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .header("Cache-Control", "no-store")
        .body(pdf.preview(input.resume(), input.level(), input.font(), input.dates(), input.linkColor()));
  }

  record Delivery(
      JsonNode resume,
      String level,
      String font,
      String dates,
      String linkColor,
      String email,
      String kind,
      boolean consent) {}

  @PostMapping("/delivery")
  Map<String, Boolean> deliver(@RequestBody Delivery in, HttpServletRequest req) {
    if ("human".equals(in.kind()) && !Objects.equals(req.getSession().getAttribute("adminVersion"), admin.version()))
      throw new ResponseStatusException(FORBIDDEN, "Human review is available only in the admin edition.");
    if ("beta-human".equals(in.kind()) && !access.isAdmin(req.getSession())
        && !"beta-pass".equals(access.state(req.getSession()).get("accessKind")))
      throw new ResponseStatusException(FORBIDDEN, "Resume help is available in the individual and Admin versions.");
    if (!in.consent())
      throw new ResponseStatusException(BAD_REQUEST, "Confirm sharing before sending.");
    if (!AdminService.validEmail(in.email()))
      throw new ResponseStatusException(BAD_REQUEST, "Enter a valid destination email.");
    Map<String, Object> account = access.state(req.getSession());
    boolean assignedCoach = "coach".equals(account.get("role")) && !"beta-pass".equals(account.get("accessKind"));
    String deliveryEmail = assignedCoach
        ? Objects.toString(req.getSession().getAttribute("reviewUserEmail"), "")
        : Objects.toString(account.get("email"), "");
    if (!in.email().trim().equalsIgnoreCase(deliveryEmail))
      throw new ResponseStatusException(FORBIDDEN, assignedCoach
          ? "Send this resume to the owner's email entered for the review."
          : "Enter the email address you used to sign in.");
    level(in.level());
    if (!List.of("email", "human", "beta-human").contains(in.kind()))
      throw new ResponseStatusException(BAD_REQUEST, "Choose email or human review.");
    if (!mail.enabled)
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE,
          "Email delivery is not connected yet. Please download your PDF instead.");
    admin.limit("delivery:" + req.getRemoteAddr(), 3);
    admin.limit("recipient:" + in.email().toLowerCase(Locale.ROOT), 3);
    byte[] bytes = pdf.create(in.resume(), in.level(), in.font(), in.dates(), in.linkColor());
    if (in.kind().equals("email"))
      mail.send(
          in.email(),
          "Your professional resume",
          "Your completed resume is attached. Thank you for building with Resume Prospective.",
          bytes,
          null,
          ResumeFilename.base(in.resume()) + ".pdf");
    else {
      var recipients = admin.recipients();
      if (recipients.isEmpty()) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "No resume-help recipient is configured yet.");
      String name = ResumeFilename.base(in.resume());
      byte[] word = docx.create(in.resume(), in.level(), in.font(), in.dates(), in.linkColor());
      var link = documents.store(name + ".docx", word);
      int sent = 0;
      try {
        for (String recipient : recipients) {
          mail.sendAttachments(recipient, "Resume help requested — Resume Prospective",
              "A user requested help with their resume. Reply to " + in.email()
                  + ". Their editable Word document and PDF are attached.\n\nWord download (expires in 7 days): "
                  + link.url() + "\n\nThis private link grants access to the resume. Please do not forward it.",
              in.email(), List.of(new MailService.Attachment(name + ".pdf", bytes, "application/pdf"),
                  new MailService.Attachment(name + ".docx", word, HelpDocuments.DOCX_TYPE)));
          sent++;
        }
      } catch (RuntimeException e) {
        if (sent == 0) documents.remove(link.token());
        throw e;
      }
    }
    return Map.of("ok", true);
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, String>> expected(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(
            Map.of(
                "message", Objects.toString(e.getReason(), "The request could not be completed.")));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, String>> unexpected(Exception e) {
    return ResponseEntity.status(BAD_REQUEST)
        .body(
            Map.of(
                "message",
                "The request could not be completed. Please check the information and try again."));
  }
}
