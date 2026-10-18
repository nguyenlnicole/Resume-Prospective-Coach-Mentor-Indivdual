package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

class CoachAccessTest {
  JdbcTemplate db;
  CoachAccess access;
  MailService mail;
  AdminService admin;
  MutableClock clock;
  String lastCode;
  @BeforeEach void setup() {
    db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    mail = new MailService(null, true, "owner@example.com") {
      @Override void text(String to, String subject, String body) {
        var match = java.util.regex.Pattern.compile("[0-9]{6}").matcher(body);
        assertTrue(match.find()); lastCode = match.group();
      }
    };
    admin = new AdminService(db, mail, "owner@example.com", AdminService.hash("testing-password-42"), "review@example.com", "https://example.com");
    clock = new MutableClock();
    access = new CoachAccess(db, mail, admin, clock);
  }
  MockHttpSession signIn(String email) {
    access.sendCode(email);
    String token = access.verify(email, lastCode);
    assertNotNull(token);
    var session = new MockHttpSession();
    session.setAttribute("coachEmail", email); session.setAttribute("coachToken", token);
    return session;
  }
  @Test void assignmentStartsThirtyDaysAndDoesNotRequirePriorSignup() {
    access.setStatus("coach@example.com", "approved");
    var session = signIn("coach@example.com");
    assertDoesNotThrow(() -> access.requireTools(session));
    assertThrows(ResponseStatusException.class, () -> access.requireAdmin(session));
    assertEquals(clock.millis() + Duration.ofDays(30).toMillis(), expiry(session));
  }
  long expiry(MockHttpSession session) {
    return ((Number)((Map<?,?>)access.state(session).get("trial")).get("expiresAt")).longValue();
  }
  @Test void expirationUsesServerTimeAndLoginDoesNotRestartIt() {
    access.setStatus("coach@example.com", "approved");
    var session = signIn("coach@example.com");
    long end = expiry(session);
    clock.now = end - 1;
    assertDoesNotThrow(() -> access.requireTools(session));
    access.setStatus("coach@example.com", "approved");
    assertEquals(end, expiry(session));
    clock.now = end;
    assertEquals("expired", access.state(session).get("state"));
    assertThrows(ResponseStatusException.class, () -> access.requireTools(session));
    var newLogin = signIn("coach@example.com");
    assertThrows(ResponseStatusException.class, () -> access.requireTools(newLogin));
    assertEquals(end, expiry(newLogin));
  }
  @Test void revocationBlocksExistingSessionAndNewLoginReplacesOldSession() {
    access.setStatus("coach@example.com", "approved");
    var old = signIn("coach@example.com");
    var current = signIn("coach@example.com");
    assertThrows(ResponseStatusException.class, () -> access.requireTools(old));
    assertDoesNotThrow(() -> access.requireTools(current));
    access.setStatus("coach@example.com", "revoked");
    assertThrows(ResponseStatusException.class, () -> access.requireTools(current));
  }
  @Test void twentyOneActiveSeatsAndIndependentExpirations() {
    for (int i=0;i<21;i++) access.setStatus("coach"+i+"@example.com", "approved");
    var first = signIn("coach0@example.com");
    long end = expiry(first);
    assertEquals(21, access.dashboard().get("limit"));
    assertEquals(30, access.dashboard().get("accessDays"));
    assertThrows(ResponseStatusException.class, () -> access.setStatus("coach21@example.com", "approved"));
    access.setStatus("coach1@example.com", "revoked");
    clock.now += Duration.ofDays(1).toMillis();
    access.setStatus("coach21@example.com", "approved");
    var later = signIn("coach21@example.com");
    assertEquals(end + Duration.ofDays(1).toMillis(), expiry(later));
    clock.now = end;
    assertThrows(ResponseStatusException.class, () -> access.requireTools(first));
    assertDoesNotThrow(() -> access.requireTools(later));
    assertDoesNotThrow(() -> access.setStatus("new@example.com", "approved"));
  }
  @Test void oldThreeDayAssignmentsGainThirtyDaysFromOriginalStart() {
    access.setStatus("coach@example.com", "approved");
    var session = signIn("coach@example.com");
    long assigned = clock.millis();
    db.update("UPDATE coach SET expires_at=? WHERE email=?",
        assigned + Duration.ofDays(3).toMillis(), "coach@example.com");
    clock.now += Duration.ofDays(5).toMillis();
    assertEquals("expired", access.state(session).get("state"));
    access = new CoachAccess(db, mail, admin, clock);
    assertEquals(assigned + Duration.ofDays(30).toMillis(), expiry(session));
    assertDoesNotThrow(() -> access.requireTools(session));
  }
  @Test void freshInstallationDoesNotGrantPermanentAccounts() {
    assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM coach WHERE status='permanent'", Integer.class));
    var session = signIn("ordinary@example.com");
    assertEquals("pending", access.state(session).get("state"));
    assertThrows(ResponseStatusException.class, () -> access.requireTools(session));
  }
  @Test void unassignedCoachCannotUseTools() {
    var session = signIn("pending@example.com");
    assertEquals("pending", access.state(session).get("state"));
    assertThrows(ResponseStatusException.class, () -> access.requireTools(session));
  }
  @Test void newCoachRequestsAccessBeforeReceivingACode() {
    assertThrows(ResponseStatusException.class, () -> access.requireCodeEligibility("new@example.com"));
    assertTrue(access.requestAccess("new@example.com"));
    assertFalse(access.requestAccess("new@example.com"));
    assertEquals("pending", db.queryForObject("SELECT status FROM coach WHERE email=?", String.class, "new@example.com"));
    assertThrows(ResponseStatusException.class, () -> access.requireCodeEligibility("new@example.com"));
    access.setStatus("new@example.com", "approved");
    assertDoesNotThrow(() -> access.requireCodeEligibility("new@example.com"));
    access.sendCode("new@example.com");
    assertNotNull(access.verify("new@example.com", lastCode));
  }
  @Test void codesExpireWorkOnceAndLockAfterFiveWrongAttempts() {
    access.sendCode("coach@example.com");
    String code = lastCode;
    assertNotNull(access.verify("coach@example.com", code));
    assertNull(access.verify("coach@example.com", code));
    access.sendCode("other@example.com");
    code = lastCode;
    String wrong = code.equals("000000") ? "111111" : "000000";
    for (int i = 0; i < 5; i++) assertNull(access.verify("other@example.com", wrong));
    assertNull(access.verify("other@example.com", code));
    access.sendCode("expired@example.com"); clock.now += 600000;
    assertNull(access.verify("expired@example.com", lastCode));
  }
  @Test void onlyAdminBypassesTrialAndManagesAccounts() {
    var admin = new MockHttpSession(); admin.setAttribute("adminVersion", 1);
    assertDoesNotThrow(() -> access.requireAdmin(admin));
    assertDoesNotThrow(() -> access.requireTools(admin));
    var guest = new MockHttpSession();
    assertThrows(ResponseStatusException.class, () -> access.requireAdmin(guest));
    assertThrows(ResponseStatusException.class, () -> access.requireTools(guest));
    assertThrows(ResponseStatusException.class, () -> access.sendCode("owner@example.com"));
  }
  static class MutableClock extends Clock {
    long now = Instant.parse("2026-10-11T12:00:00Z").toEpochMilli();
    public ZoneId getZone() { return ZoneOffset.UTC; }
    public Clock withZone(ZoneId zone) { return this; }
    public Instant instant() { return Instant.ofEpochMilli(now); }
  }
}
