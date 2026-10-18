package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;
import jakarta.servlet.http.HttpSession;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class BetaPasses {
  static final int LIMIT = 50;
  static final int THREE_DAY_LIMIT = 25;
  static final long DURATION = Duration.ofHours(24).toMillis();
  private final JdbcTemplate db;
  private final CoachAccess access;
  private final Clock clock;
  private final String betaUrl;

  BetaPasses(JdbcTemplate db, CoachAccess access, Clock clock,
      @Value("${app.beta-url:https://betarp.pnbpaws.com}") String betaUrl) {
    this.db = db;
    this.access = access;
    this.clock = clock;
    this.betaUrl = betaUrl.replaceFirst("/+$", "");
    db.execute("CREATE TABLE IF NOT EXISTS beta_pass_pool (id INT PRIMARY KEY)");
    db.update("MERGE INTO beta_pass_pool KEY(id) VALUES(1)");
    db.execute("CREATE TABLE IF NOT EXISTS beta_pass (id VARCHAR(36) PRIMARY KEY, code_hash VARCHAR(64) NOT NULL UNIQUE, created BIGINT NOT NULL, redeemed_by VARCHAR(254), redeemed_at BIGINT, expires_at BIGINT, revoked BOOLEAN DEFAULT FALSE NOT NULL)");
    db.execute("ALTER TABLE beta_pass ADD COLUMN IF NOT EXISTS duration_hours INT DEFAULT 24 NOT NULL");
    db.execute("ALTER TABLE beta_pass ADD COLUMN IF NOT EXISTS assigned_to VARCHAR(254)");
  }

  Map<String, Object> dashboard(HttpSession session) {
    access.requireAdmin(session);
    var passes = db.query("SELECT id,created,assigned_to,redeemed_by,redeemed_at,expires_at,revoked,duration_hours FROM beta_pass ORDER BY created,id", (r, i) -> {
      String email = Objects.toString(r.getString("redeemed_by"), "");
      String assignedEmail = Objects.toString(r.getString("assigned_to"), "");
      long expires = r.getLong("expires_at");
      String status = r.getBoolean("revoked") ? "revoked" : email.isBlank() ? "unused" : expires > clock.millis() ? "active" : "expired";
      return Map.of("id", r.getString("id"), "createdAt", r.getLong("created"), "email", email, "assignedEmail", assignedEmail,
          "redeemedAt", r.getLong("redeemed_at"), "expiresAt", expires, "status", status,
          "durationDays", r.getInt("duration_hours") / 24);
    });
    int oneDay = (int) passes.stream().filter(pass -> pass.get("durationDays").equals(1)).count();
    int threeDay = (int) passes.stream().filter(pass -> pass.get("durationDays").equals(3)).count();
    return Map.of("limit", LIMIT, "remaining", LIMIT - oneDay, "threeDayLimit", THREE_DAY_LIMIT,
        "threeDayRemaining", THREE_DAY_LIMIT - threeDay, "passes", passes, "betaUrl", betaUrl);
  }

  @Transactional
  public Map<String, Object> issue(HttpSession session, int count) {
    return issue(session, count, 1);
  }

  @Transactional
  public Map<String, Object> issue(HttpSession session, int count, int days) {
    return issue(session, count, days, List.of());
  }

  @Transactional
  public Map<String, Object> issue(HttpSession session, int count, int days, List<String> emails) {
    access.requireAdmin(session);
    if (days != 1 && days != 3) throw new ResponseStatusException(BAD_REQUEST, "Choose a one-day or three-day pass.");
    List<String> assigned = emails == null ? List.of() : emails.stream().map(this::normalizeEmail).toList();
    if (!assigned.isEmpty() && (assigned.size() != count || new HashSet<>(assigned).size() != assigned.size()))
      throw new ResponseStatusException(BAD_REQUEST, "Provide one different user email for each pass.");
    int limit = days == 1 ? LIMIT : THREE_DAY_LIMIT;
    if (count < 1 || count > limit) throw new ResponseStatusException(BAD_REQUEST, "Choose a valid pass count.");
    db.queryForMap("SELECT id FROM beta_pass_pool WHERE id=1 FOR UPDATE");
    int issued = db.queryForObject("SELECT COUNT(*) FROM beta_pass WHERE duration_hours=?", Integer.class, days * 24);
    if (issued + count > limit)
      throw new ResponseStatusException(CONFLICT, "This access pass pool is full.");
    List<Map<String, String>> codes = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String code = Base64.getUrlEncoder().withoutPadding().encodeToString(AdminService.random(32));
      String id = UUID.randomUUID().toString();
      String email = assigned.isEmpty() ? "" : assigned.get(i);
      db.update("INSERT INTO beta_pass(id,code_hash,created,duration_hours,assigned_to) VALUES(?,?,?,?,?)", id, AdminService.digest(code), clock.millis(), days * 24, email.isBlank() ? null : email);
      codes.add(Map.of("id", id, "code", code, "url", betaUrl + "/#pass=" + code, "assignedEmail", email,
          "durationDays", Integer.toString(days)));
    }
    return Map.of("issued", codes, "dashboard", dashboard(session));
  }

  @Transactional
  public Map<String, Object> assign(HttpSession session, String id, String email) {
    access.requireAdmin(session);
    String assignedEmail = normalizeEmail(email);
    String code = Base64.getUrlEncoder().withoutPadding().encodeToString(AdminService.random(32));
    int changed = db.update("UPDATE beta_pass SET assigned_to=?,code_hash=? WHERE id=? AND redeemed_by IS NULL AND revoked=FALSE",
        assignedEmail, AdminService.digest(code), id);
    if (changed == 0) throw new ResponseStatusException(CONFLICT, "Only unused, active passes can be assigned.");
    return Map.of("issued", List.of(Map.of("id", id, "url", betaUrl + "/#pass=" + code, "assignedEmail", assignedEmail)),
        "dashboard", dashboard(session));
  }

  @Transactional
  public void approveRequest(String address) {
    String email = normalizeEmail(address);
    db.queryForMap("SELECT id FROM beta_pass_pool WHERE id=1 FOR UPDATE");
    if (eligibleForSignIn(email)) return;
    var unused = db.queryForList("SELECT id FROM beta_pass WHERE redeemed_by IS NULL AND revoked=FALSE AND assigned_to IS NULL ORDER BY created,id");
    if (!unused.isEmpty()) {
      db.update("UPDATE beta_pass SET assigned_to=? WHERE id=?", email, unused.getFirst().get("ID"));
      return;
    }
    int issued = db.queryForObject("SELECT COUNT(*) FROM beta_pass WHERE duration_hours=24", Integer.class);
    if (issued >= LIMIT) throw new ResponseStatusException(CONFLICT, "No access passes remain. Manage passes in the admin page.");
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(AdminService.random(32));
    db.update("INSERT INTO beta_pass(id,code_hash,created,duration_hours,assigned_to) VALUES(?,?,?,?,?)",
        UUID.randomUUID().toString(), AdminService.digest(token), clock.millis(), 24, email);
  }

  private String normalizeEmail(String email) {
    if (email == null) throw new ResponseStatusException(BAD_REQUEST, "Enter a user email.");
    String normalized = email.trim().toLowerCase(Locale.ROOT);
    if (normalized.length() > 254 || !normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new ResponseStatusException(BAD_REQUEST, "Enter a valid user email.");
    return normalized;
  }

  boolean eligibleForSignIn(String address) {
    String email = access.email(address);
    return db.queryForObject("SELECT COUNT(*) FROM beta_pass WHERE revoked=FALSE AND "
        + "((assigned_to=? AND redeemed_by IS NULL) OR (redeemed_by=? AND expires_at>?))",
        Integer.class, email, email, clock.millis()) > 0;
  }

  @Transactional
  public void activateAssigned(HttpSession session) {
    var state = access.state(session);
    if (!"coach".equals(state.get("role")))
      throw new ResponseStatusException(UNAUTHORIZED, "Verify your email before using resume access.");
    if ((boolean) state.get("allowed")) return;
    String email = state.get("email").toString();
    String coachStatus = db.queryForObject("SELECT status FROM coach WHERE email=?", String.class, email);
    if ("revoked".equals(coachStatus)) return;
    db.queryForMap("SELECT id FROM beta_pass_pool WHERE id=1 FOR UPDATE");
    var available = db.queryForList("SELECT id,duration_hours FROM beta_pass WHERE assigned_to=? "
        + "AND redeemed_by IS NULL AND revoked=FALSE ORDER BY created,id", email);
    if (available.isEmpty()) return;
    var pass = available.getFirst();
    long now = clock.millis();
    long duration = Duration.ofHours(((Number) pass.get("DURATION_HOURS")).longValue()).toMillis();
    db.update("UPDATE beta_pass SET redeemed_by=?,redeemed_at=?,expires_at=? WHERE id=? AND redeemed_by IS NULL",
        email, now, now + duration, pass.get("ID"));
  }

  @Transactional
  public void redeem(HttpSession session, String code) {
    var state = access.state(session);
    if (!state.get("role").equals("coach")) throw new ResponseStatusException(UNAUTHORIZED, "Verify your email in Sign In before redeeming a pass.");
    if (state.get("state").equals("revoked")) throw new ResponseStatusException(FORBIDDEN, "Your access has been revoked. Contact the app owner.");
    if (code == null || !code.matches("[A-Za-z0-9_-]{43}")) throw invalid();
    String email = state.get("email").toString();
    db.queryForMap("SELECT id FROM beta_pass_pool WHERE id=1 FOR UPDATE");
    var rows = db.queryForList("SELECT * FROM beta_pass WHERE code_hash=? FOR UPDATE", AdminService.digest(code));
    if (rows.isEmpty()) throw invalid();
    var pass = rows.getFirst();
    if (Boolean.TRUE.equals(pass.get("REVOKED"))) throw invalid();
    String assignedEmail = Objects.toString(pass.get("ASSIGNED_TO"), "");
    if (!assignedEmail.isBlank() && !assignedEmail.equals(email))
      throw new ResponseStatusException(FORBIDDEN, "This access pass is assigned to a different email. Sign in with the invited email.");
    if (pass.get("REDEEMED_BY") != null) {
      if (email.equals(pass.get("REDEEMED_BY")) && ((Number)pass.get("EXPIRES_AT")).longValue() > clock.millis()) return;
      throw invalid();
    }
    if ((boolean)state.get("allowed") || db.queryForObject("SELECT COUNT(*) FROM beta_pass WHERE redeemed_by=? AND revoked=FALSE AND expires_at>?", Integer.class, email, clock.millis()) > 0)
      throw new ResponseStatusException(CONFLICT, "You already have active access. This pass has not been used.");
    long now = clock.millis();
    long duration = Duration.ofHours(((Number)pass.get("DURATION_HOURS")).longValue()).toMillis();
    db.update("UPDATE beta_pass SET redeemed_by=?,redeemed_at=?,expires_at=? WHERE id=? AND redeemed_by IS NULL", email, now, now + duration, pass.get("ID"));
  }

  @Transactional
  public void revoke(HttpSession session, String id) {
    access.requireAdmin(session);
    if (db.update("UPDATE beta_pass SET revoked=TRUE WHERE id=?", id) == 0)
      throw new ResponseStatusException(NOT_FOUND, "Pass not found.");
  }

  private ResponseStatusException invalid() {
    return new ResponseStatusException(BAD_REQUEST, "This pass is invalid, already used, expired, or revoked.");
  }
}
