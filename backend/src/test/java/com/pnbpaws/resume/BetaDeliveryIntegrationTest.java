package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:beta-delivery;DB_CLOSE_DELAY=-1",
    "app.admin-email=owner@example.com", "app.admin-password-hash=", "app.openai-key=", "app.openai-model=",
    "app.mail-enabled=true", "app.review-email=nicole@example.com", "app.public-url=https://betarp.pnbpaws.com"
})
@AutoConfigureMockMvc
class BetaDeliveryIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired CoachAccess access;
  @Autowired BetaPasses passes;
  @Autowired AdminService admin;
  @Autowired CaptureMail mail;

  @BeforeEach void resetCapturedMail() {
    mail.code = null; mail.to = null; mail.body = null; mail.replyTo = null; mail.attachments = null;
  }

  @Test void betaPassAllowsHelpWithWordAndLinkButNeverAdminAccess() throws Exception {
    var owner = new MockHttpSession(); owner.setAttribute("adminVersion", admin.version());
    var issued = (List<?>)passes.issue(owner, 1).get("issued");
    String code = ((Map<?,?>)issued.getFirst()).get("code").toString();
    access.sendCode("tester@example.com");
    String token = access.verify("tester@example.com", mail.code);
    var session = new MockHttpSession(); session.setAttribute("coachEmail", "tester@example.com"); session.setAttribute("coachToken", token);
    passes.redeem(session, code);
    mvc.perform(get("/api/admin/beta-passes").session(session)).andExpect(status().isForbidden());
    var csrf = mvc.perform(get("/api/csrf").session(session)).andReturn().getResponse().getContentAsString();
    String csrfToken = mapper.readTree(csrf).path("token").asText();
    var resume = mapper.readTree(getClass().getResourceAsStream("/approved-reference.json"));
    var payload = mapper.createObjectNode(); payload.set("resume", resume);
    payload.put("level", "Senior").put("font", "Calibri").put("dates", "right").put("linkColor", "dark")
        .put("email", "tester@example.com").put("kind", "beta-human").put("consent", false);
    mvc.perform(post("/api/delivery").session(session).header("X-CSRF-TOKEN", csrfToken)
        .contentType(MediaType.APPLICATION_JSON).content(payload.toString())).andExpect(status().isBadRequest());
    assertNull(mail.attachments);
    payload.put("consent", true);
    payload.put("email", "someone-else@example.com");
    mvc.perform(post("/api/delivery").session(session).header("X-CSRF-TOKEN", csrfToken)
        .contentType(MediaType.APPLICATION_JSON).content(payload.toString())).andExpect(status().isForbidden());
    payload.put("email", "tester@example.com");
    mvc.perform(post("/api/delivery").session(session).header("X-CSRF-TOKEN", csrfToken)
        .contentType(MediaType.APPLICATION_JSON).content(payload.toString())).andExpect(status().isOk());
    assertEquals("nicole@example.com", mail.to); assertEquals("tester@example.com", mail.replyTo);
    assertEquals(Set.of("application/pdf", HelpDocuments.DOCX_TYPE), new HashSet<>(mail.attachments.stream().map(MailService.Attachment::type).toList()));
    assertTrue(mail.body.contains("expires in 7 days"));
    var match = java.util.regex.Pattern.compile("https://betarp.pnbpaws.com(/api/help-documents/[A-Za-z0-9_-]+)").matcher(mail.body);
    assertTrue(match.find());
    mvc.perform(get(match.group(1))).andExpect(status().isOk()).andExpect(content().contentType(HelpDocuments.DOCX_TYPE))
        .andExpect(header().string("Cache-Control", "no-store"));
    payload.put("kind", "email");
    mvc.perform(post("/api/delivery").session(session).header("X-CSRF-TOKEN", csrfToken)
        .contentType(MediaType.APPLICATION_JSON).content(payload.toString())).andExpect(status().isOk());
    assertEquals("tester@example.com", mail.to); assertEquals(1, mail.attachments.size());
    assertEquals("application/pdf", mail.attachments.getFirst().type());
  }

  @Test void coachCanEmailTheReviewedResumeOwnerButNotAnotherAddress() throws Exception {
    access.setStatus("coach-delivery@example.com", "approved");
    access.sendCode("coach-delivery@example.com");
    String token = access.verify("coach-delivery@example.com", mail.code);
    var session = new MockHttpSession();
    session.setAttribute("coachEmail", "coach-delivery@example.com");
    session.setAttribute("coachToken", token);
    session.setAttribute("reviewUserEmail", "candidate@example.com");
    var csrf = mvc.perform(get("/api/csrf").session(session)).andReturn().getResponse().getContentAsString();
    String csrfToken = mapper.readTree(csrf).path("token").asText();
    var payload = mapper.createObjectNode();
    payload.set("resume", mapper.readTree(getClass().getResourceAsStream("/approved-reference.json")));
    payload.put("level", "Senior").put("font", "Calibri").put("dates", "right").put("linkColor", "dark")
        .put("kind", "email").put("consent", true).put("email", "unrelated@example.com");
    mvc.perform(post("/api/delivery").session(session).header("X-CSRF-TOKEN", csrfToken)
        .contentType(MediaType.APPLICATION_JSON).content(payload.toString())).andExpect(status().isForbidden());
    payload.put("email", "candidate@example.com");
    mvc.perform(post("/api/delivery").session(session).header("X-CSRF-TOKEN", csrfToken)
        .contentType(MediaType.APPLICATION_JSON).content(payload.toString())).andExpect(status().isOk());
    assertEquals("candidate@example.com", mail.to);
    assertEquals("application/pdf", mail.attachments.getFirst().type());
  }

  @Test void betaCanRateWithoutBuildingAndCoachRatesOnlyAiFeedback() throws Exception {
    var owner = new MockHttpSession(); owner.setAttribute("adminVersion", admin.version());
    var issued = (List<?>)passes.issue(owner, 1).get("issued");
    String passCode = ((Map<?,?>)issued.getFirst()).get("code").toString();
    access.sendCode("rating-beta@example.com");
    String betaToken = access.verify("rating-beta@example.com", mail.code);
    var beta = new MockHttpSession();
    beta.setAttribute("coachEmail", "rating-beta@example.com");
    beta.setAttribute("coachToken", betaToken);
    passes.redeem(beta, passCode);
    beta.setAttribute("reviewUserEmail", "rating-beta@example.com");
    beta.setAttribute("reviewedLevel", "Senior");
    String betaCsrf = mapper.readTree(mvc.perform(get("/api/csrf").session(beta))
        .andReturn().getResponse().getContentAsString()).path("token").asText();
    var resumeCopy = mapper.createObjectNode();
    resumeCopy.set("resume", mapper.readTree(getClass().getResourceAsStream("/approved-reference.json")));
    resumeCopy.put("level", "Senior").put("font", "Calibri").put("dates", "right")
        .put("linkColor", "blue").put("stage", "draft");
    mvc.perform(post("/api/beta/resume-copy").session(beta).header("X-CSRF-TOKEN", betaCsrf)
        .contentType(MediaType.APPLICATION_JSON).content(resumeCopy.toString()))
        .andExpect(status().isOk());
    mvc.perform(post("/api/beta/app-rating").session(beta).header("X-CSRF-TOKEN", betaCsrf)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"stars\":0,\"aiStars\":5,\"comment\":\"Clear review\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/beta/app-rating").session(beta).header("X-CSRF-TOKEN", betaCsrf)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"stars\":2,\"aiStars\":3,\"comment\":\"Again\"}"))
        .andExpect(status().isConflict());
    mvc.perform(post("/api/coach/ai-rating").session(beta).header("X-CSRF-TOKEN", betaCsrf)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"aiStars\":5,\"comment\":\"Clear review\"}"))
        .andExpect(status().isForbidden());

    access.setStatus("rating-coach@example.com", "approved");
    access.sendCode("rating-coach@example.com");
    String coachToken = access.verify("rating-coach@example.com", mail.code);
    var coach = new MockHttpSession();
    coach.setAttribute("coachEmail", "rating-coach@example.com");
    coach.setAttribute("coachToken", coachToken);
    coach.setAttribute("reviewUserEmail", "candidate@example.com");
    coach.setAttribute("reviewedLevel", "Senior");
    String coachCsrf = mapper.readTree(mvc.perform(get("/api/csrf").session(coach))
        .andReturn().getResponse().getContentAsString()).path("token").asText();
    mvc.perform(post("/api/beta/resume-copy").session(coach).header("X-CSRF-TOKEN", coachCsrf)
        .contentType(MediaType.APPLICATION_JSON).content(resumeCopy.toString()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/review/owner-copy").session(coach).header("X-CSRF-TOKEN", coachCsrf)
        .contentType(MediaType.APPLICATION_JSON).content(resumeCopy.toString()))
        .andExpect(status().isOk());
    mvc.perform(post("/api/review/owner-copy").session(beta).header("X-CSRF-TOKEN", betaCsrf)
        .contentType(MediaType.APPLICATION_JSON).content(resumeCopy.toString()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/coach/ai-rating").session(coach).header("X-CSRF-TOKEN", coachCsrf)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"aiStars\":0,\"comment\":\"More specific examples, please\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/beta/app-rating").session(coach).header("X-CSRF-TOKEN", coachCsrf)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"stars\":5,\"aiStars\":5,\"comment\":\"Nice\"}"))
        .andExpect(status().isForbidden());
  }

  @TestConfiguration static class Config {
    @Bean @Primary CaptureMail captureMail() { return new CaptureMail(); }
  }
  static class CaptureMail extends MailService {
    String code, to, body, replyTo;
    List<Attachment> attachments;
    CaptureMail() { super(null, true, "owner@example.com"); }
    @Override void text(String to, String subject, String body) {
      var match = java.util.regex.Pattern.compile("[0-9]{6}").matcher(body);
      if (match.find()) code = match.group();
    }
    @Override void sendAttachments(String to, String subject, String body, String replyTo, List<Attachment> attachments) {
      this.to = to; this.body = body; this.replyTo = replyTo; this.attachments = attachments;
    }
  }
}
