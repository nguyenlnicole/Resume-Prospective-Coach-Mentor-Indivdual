package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

class CoachApprovalLinksTest {
  @Test void requestLinkApprovesOnceAndEmailsCodeOnlyAfterApproval() {
    var db = new JdbcTemplate(new DriverManagerDataSource(
        "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    var mail = new MailService(null, true, "owner@example.com") {
      String code;
      @Override void text(String to, String subject, String body) {
        assertTrue(java.util.Set.of("coach@example.com", "tester@example.com").contains(to));
        var match = java.util.regex.Pattern.compile("[0-9]{6}").matcher(body);
        assertTrue(match.find()); code = match.group();
      }
    };
    var admin = new AdminService(db, mail, "owner@example.com",
        AdminService.hash("testing-password-42"), "review@example.com", "https://example.com");
    var clock = new CoachAccessTest.MutableClock();
    var access = new CoachAccess(db, mail, admin, clock);
    var passes = new BetaPasses(db, access, clock, "https://example.com");
    var links = new CoachApprovalLinks(db, clock, access, passes);
    assertTrue(access.requestAccess("coach@example.com"));
    String token = links.create("coach@example.com");
    assertEquals("coach@example.com", links.details(token).get("email"));
    assertThrows(ResponseStatusException.class, () -> access.requireCodeEligibility("coach@example.com"));
    assertEquals("coach@example.com", links.approve(token));
    assertDoesNotThrow(() -> access.requireCodeEligibility("coach@example.com"));
    assertThrows(ResponseStatusException.class, () -> links.approve(token));
    assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM coach_code WHERE email=?", Integer.class, "coach@example.com"));

    String betaToken = links.create("tester@example.com", "beta");
    assertEquals("beta", links.details(betaToken).get("edition"));
    assertFalse(passes.eligibleForSignIn("tester@example.com"));
    links.approve(betaToken);
    assertTrue(passes.eligibleForSignIn("tester@example.com"));
    assertFalse(access.isApproved("tester@example.com"));
    assertThrows(ResponseStatusException.class, () -> links.approve(betaToken));
    assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM coach_code WHERE email='tester@example.com'", Integer.class));

    assertTrue(access.requestAccess("other@example.com"));
    String expired = links.create("other@example.com");
    clock.now += Duration.ofDays(7).toMillis();
    assertThrows(ResponseStatusException.class, () -> links.approve(expired));
  }
}
