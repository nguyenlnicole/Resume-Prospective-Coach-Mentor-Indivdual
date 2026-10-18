package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:admin-login-integration;DB_CLOSE_DELAY=-1",
    "app.admin-email=owner@example.com", "app.admin-password-hash=", "app.openai-key=",
    "app.openai-model=", "app.mail-enabled=false", "server.servlet.session.cookie.secure=false"
})
@AutoConfigureMockMvc
@Import(AdminLoginIntegrationTest.TestMailConfig.class)
class AdminLoginIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired TestMail mail;

  @Test void emailedCodeSignsInAndAdminAccessSurvivesTheResponse() throws Exception {
    var csrf = mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
    var session = (MockHttpSession) csrf.getRequest().getSession();
    String token = mapper.readTree(csrf.getResponse().getContentAsString()).path("token").asText();
    mvc.perform(post("/api/admin/code").session(session).header("X-CSRF-TOKEN", token)
        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"owner@example.com\"}"))
        .andExpect(status().isOk());
    assertNotNull(mail.code);
    mvc.perform(post("/api/admin/verify").session(session).header("X-CSRF-TOKEN", token)
        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(java.util.Map.of("email", "owner@example.com", "code", mail.code))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("owner@example.com"));
    mvc.perform(get("/api/access").session(session)).andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(true)).andExpect(jsonPath("$.role").value("admin"))
        .andExpect(jsonPath("$.email").value("owner@example.com"));
    mvc.perform(get("/api/admin/me").session(session)).andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("owner@example.com"));
    mvc.perform(post("/api/admin/verify").session(session).header("X-CSRF-TOKEN", token)
        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(java.util.Map.of("email", "owner@example.com", "code", mail.code))))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/coach/code").session(session).header("X-CSRF-TOKEN", token)
        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"owner@example.com\"}"))
        .andExpect(status().isBadRequest());
  }
  @TestConfiguration static class TestMailConfig {
    @Bean @Primary TestMail testMail() { return new TestMail(); }
  }
  static class TestMail extends MailService {
    String code;
    TestMail() { super(null, true, "owner@example.com"); }
    @Override void text(String to, String subject, String body) {
      assertEquals("owner@example.com", to);
      var match = java.util.regex.Pattern.compile("[0-9]{6}").matcher(body);
      assertTrue(match.find()); code = match.group();
    }
  }
}
