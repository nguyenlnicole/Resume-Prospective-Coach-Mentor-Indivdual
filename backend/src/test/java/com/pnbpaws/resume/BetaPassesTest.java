package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

class BetaPassesTest {
  JdbcTemplate db;
  CoachAccess access;
  BetaPasses passes;
  MockHttpSession owner;
  MutableClock clock;
  TransactionTemplate transaction;
  String emailCode;

  @BeforeEach void setup() {
    var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    db = new JdbcTemplate(source);
    transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
    var mail = new MailService(null, true, "owner@example.com") {
      @Override void text(String to, String subject, String body) {
        var match = java.util.regex.Pattern.compile("[0-9]{6}").matcher(body);
        assertTrue(match.find()); emailCode = match.group();
      }
    };
    var admin = new AdminService(db, mail, "owner@example.com", "", "review@example.com", "https://example.com");
    clock = new MutableClock();
    access = new CoachAccess(db, mail, admin, clock);
    passes = new BetaPasses(db, access, clock, "https://betarp.pnbpaws.com");
    owner = new MockHttpSession(); owner.setAttribute("adminVersion", admin.version());
  }
  MockHttpSession signIn(String email) {
    access.sendCode(email);
    String token = access.verify(email, emailCode);
    var session = new MockHttpSession(); session.setAttribute("coachEmail", email); session.setAttribute("coachToken", token);
    return session;
  }
  @SuppressWarnings("unchecked")
  Map<String,String> issue() { return ((List<Map<String,String>>)passes.issue(owner, 1).get("issued")).getFirst(); }
  long expiry(MockHttpSession session) { return ((Number)((Map<?,?>)access.state(session).get("trial")).get("expiresAt")).longValue(); }

  @Test void onlyAdminCanIssueListOrRevokeAndCodesAreNotStored() {
    var guest = new MockHttpSession(); var coach = signIn("coach@example.com");
    for (var session : List.of(guest, coach)) {
      assertThrows(ResponseStatusException.class, () -> passes.issue(session, 1));
      assertThrows(ResponseStatusException.class, () -> passes.dashboard(session));
      assertThrows(ResponseStatusException.class, () -> passes.revoke(session, "missing"));
    }
    var pass = issue();
    assertTrue(pass.get("url").startsWith("https://betarp.pnbpaws.com/#pass="));
    assertEquals(AdminService.digest(pass.get("code")), db.queryForObject("SELECT code_hash FROM beta_pass", String.class));
    assertFalse(passes.dashboard(owner).toString().contains(pass.get("code")));
  }
  @Test void exactlyFiftyPassesAndRevocationDoesNotResetTheBatch() {
    passes.issue(owner, 50);
    assertEquals(0, passes.dashboard(owner).get("remaining"));
    assertThrows(ResponseStatusException.class, () -> passes.issue(owner, 1));
    assertThrows(ResponseStatusException.class, () -> passes.issue(owner, 0));
    String id = db.queryForList("SELECT id FROM beta_pass", String.class).getFirst();
    passes.revoke(owner, id);
    assertThrows(ResponseStatusException.class, () -> passes.issue(owner, 1));
  }
  @Test void twentyFiveThreeDayPassesHaveAnIndependentPoolAndExpireAfter72Hours() {
    passes.issue(owner, 50, 1);
    passes.issue(owner, 25, 3);
    assertEquals(0, passes.dashboard(owner).get("remaining"));
    assertEquals(0, passes.dashboard(owner).get("threeDayRemaining"));
    assertThrows(ResponseStatusException.class, () -> passes.issue(owner, 1, 1));
    assertThrows(ResponseStatusException.class, () -> passes.issue(owner, 1, 3));
    assertEquals(25, ((List<?>)passes.dashboard(owner).get("passes")).stream()
        .filter(pass -> ((Map<?,?>)pass).get("durationDays").equals(3)).count());
  }
  @Test void threeDayPassStartsOnRedemption() {
    var pass = ((List<Map<String,String>>)passes.issue(owner, 1, 3).get("issued")).getFirst();
    clock.now += Duration.ofDays(5).toMillis();
    var session = signIn("three-day@example.com");
    passes.redeem(session, pass.get("code"));
    long end = clock.millis() + Duration.ofDays(3).toMillis();
    assertEquals(end, expiry(session));
    clock.now = end - 1;
    assertDoesNotThrow(() -> access.requireTools(session));
    clock.now = end;
    assertThrows(ResponseStatusException.class, () -> access.requireTools(session));
  }
  @Test void assignedPassCanOnlyBeRedeemedByItsTester() {
    var issued = (List<Map<String,String>>) passes.issue(owner, 1, 3, List.of(" Tester@Example.com ")).get("issued");
    var pass = issued.getFirst();
    assertEquals("tester@example.com", pass.get("assignedEmail"));
    assertThrows(ResponseStatusException.class, () -> passes.redeem(signIn("other@example.com"), pass.get("code")));
    passes.redeem(signIn("tester@example.com"), pass.get("code"));
    assertEquals("tester@example.com", db.queryForObject("SELECT redeemed_by FROM beta_pass WHERE id=?", String.class, pass.get("id")));
  }
  @Test void assignedTesterGetsAccessAfterVerifyingEmailWithoutASeparatePassCode() {
    passes.issue(owner, 1, 3, List.of("tester@example.com"));
    assertTrue(passes.eligibleForSignIn("tester@example.com"));
    assertFalse(passes.eligibleForSignIn("other@example.com"));
    clock.now += Duration.ofDays(5).toMillis();
    var session = signIn("tester@example.com");
    passes.activateAssigned(session);
    long end = clock.millis() + Duration.ofDays(3).toMillis();
    assertEquals(end, expiry(session));
    assertTrue((boolean) access.state(session).get("allowed"));
    assertEquals("tester@example.com", db.queryForObject("SELECT redeemed_by FROM beta_pass", String.class));
    clock.now += Duration.ofHours(1).toMillis();
    passes.activateAssigned(session);
    assertEquals(end, expiry(session));
    clock.now = end;
    assertFalse(passes.eligibleForSignIn("tester@example.com"));
  }
  @Test void existingUnusedPassCanBeAssignedAndThenActivatedByEmail() {
    var pass = issue();
    assertFalse(passes.eligibleForSignIn("tester@example.com"));
    var session = signIn("tester@example.com");
    passes.activateAssigned(session);
    assertFalse((boolean) access.state(session).get("allowed"));
    passes.assign(owner, pass.get("id"), "tester@example.com");
    passes.activateAssigned(session);
    assertTrue((boolean) access.state(session).get("allowed"));
  }
  @Test void assignmentRejectsDuplicateOrInvalidEmails() {
    assertThrows(ResponseStatusException.class, () -> passes.issue(owner, 2, 1, List.of("same@example.com", "SAME@example.com")));
    assertThrows(ResponseStatusException.class, () -> passes.issue(owner, 1, 1, List.of("not-an-email")));
    assertEquals(50, passes.dashboard(owner).get("remaining"));
  }
  @Test void assigningUnusedPassReplacesItsLinkWithoutChangingThePool() {
    var pass = issue();
    var result = passes.assign(owner, pass.get("id"), "new@example.com");
    var replacement = ((List<Map<String,String>>)result.get("issued")).getFirst();
    assertNotEquals(pass.get("url"), replacement.get("url"));
    assertEquals("new@example.com", replacement.get("assignedEmail"));
    assertEquals(49, passes.dashboard(owner).get("remaining"));
    assertThrows(ResponseStatusException.class, () -> passes.redeem(signIn("new@example.com"), pass.get("code")));
    String newCode = replacement.get("url").substring(replacement.get("url").indexOf("#pass=") + 6);
    passes.redeem(signIn("new@example.com"), newCode);
    assertThrows(ResponseStatusException.class, () -> passes.assign(owner, pass.get("id"), "other@example.com"));
  }
  @Test void dayStartsOnRedemptionNotCreationAndExpiresExactlyAfter24Hours() {
    var pass = issue(); clock.now += Duration.ofDays(9).toMillis();
    var session = signIn("tester@example.com");
    assertThrows(ResponseStatusException.class, () -> passes.redeem(new MockHttpSession(), pass.get("code")));
    passes.redeem(session, pass.get("code"));
    long end = clock.millis() + BetaPasses.DURATION;
    assertEquals(end, expiry(session));
    assertEquals("beta-pass", access.state(session).get("accessKind"));
    assertDoesNotThrow(() -> access.requireTools(session));
    assertThrows(ResponseStatusException.class, () -> access.requireAdmin(session));
    clock.now = end - 1; assertDoesNotThrow(() -> access.requireTools(session));
    clock.now = end; assertThrows(ResponseStatusException.class, () -> access.requireTools(session));
    var again = signIn("tester@example.com");
    assertEquals(end, expiry(again)); assertThrows(ResponseStatusException.class, () -> access.requireTools(again));
  }
  @Test void aPassCannotBeSharedReusedOrStacked() {
    var pass = issue(); var next = issue();
    var first = signIn("first@example.com"); var other = signIn("other@example.com");
    passes.redeem(first, pass.get("code"));
    long end = expiry(first); clock.now += 1000;
    passes.redeem(first, pass.get("code")); assertEquals(end, expiry(first));
    assertThrows(ResponseStatusException.class, () -> passes.redeem(other, pass.get("code")));
    assertThrows(ResponseStatusException.class, () -> passes.redeem(first, next.get("code")));
    passes.redeem(other, next.get("code")); assertTrue((boolean)access.state(other).get("allowed"));
  }
  @Test void revokeImmediatelyBlocksAccessAndUnusedPassesCannotBeRedeemed() {
    var pass = issue(); var session = signIn("tester@example.com");
    passes.redeem(session, pass.get("code")); passes.revoke(owner, pass.get("id"));
    assertThrows(ResponseStatusException.class, () -> access.requireTools(session));
    var unused = issue(); passes.revoke(owner, unused.get("id"));
    assertThrows(ResponseStatusException.class, () -> passes.redeem(signIn("another@example.com"), unused.get("code")));
  }
  @Test void concurrentRedemptionHasOnlyOneWinner() throws Exception {
    var pass = issue(); var first = signIn("first@example.com"); var second = signIn("second@example.com");
    var ready = new CountDownLatch(2); var go = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var futures = new ArrayList<Future<Boolean>>();
      for (var session : List.of(first, second)) futures.add(executor.submit(() -> {
        ready.countDown(); go.await();
        try { transaction.execute(status -> { passes.redeem(session, pass.get("code")); return null; }); return true; }
        catch (ResponseStatusException e) { return false; }
      }));
      assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
      int successes = 0; for (var future : futures) if (future.get(10, TimeUnit.SECONDS)) successes++;
      assertEquals(1, successes);
    }
  }
  @Test void concurrentIssuingCannotExceedFifty() throws Exception {
    passes.issue(owner, 49);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Boolean> task = () -> {
        try { transaction.execute(status -> passes.issue(owner, 1)); return true; }
        catch (ResponseStatusException e) { return false; }
      };
      var first = executor.submit(task); var second = executor.submit(task);
      assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
      assertEquals(0, passes.dashboard(owner).get("remaining"));
    }
  }
  static class MutableClock extends Clock {
    long now = Instant.parse("2026-10-13T12:00:00Z").toEpochMilli();
    public ZoneId getZone() { return ZoneOffset.UTC; }
    public Clock withZone(ZoneId zone) { return this; }
    public Instant instant() { return Instant.ofEpochMilli(now); }
  }
}
