package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class CoachAccess {
  @org.springframework.beans.factory.annotation.Autowired RegisteredAccounts accounts;
  private static final int COACH_LIMIT = 21;
  private static final int ACCESS_DAYS = 30;
  private static final Set<String> PERMANENT_COACH_EMAILS = Set.of();
  private final JdbcTemplate db;
  private final MailService mail;
  private final AdminService admin;
  private final Clock clock;

  CoachAccess(JdbcTemplate db, MailService mail, AdminService admin, Clock clock) {
    this.db = db;
    this.mail = mail;
    this.admin = admin;
    this.clock = clock;
    db.execute("CREATE TABLE IF NOT EXISTS coach (email VARCHAR(254) PRIMARY KEY, status VARCHAR(20) NOT NULL, created BIGINT NOT NULL, session_hash VARCHAR(64))");
    db.execute("ALTER TABLE coach ADD COLUMN IF NOT EXISTS assigned_at BIGINT");
    db.execute("ALTER TABLE coach ADD COLUMN IF NOT EXISTS expires_at BIGINT");
    db.execute("CREATE TABLE IF NOT EXISTS coach_code (email VARCHAR(254) PRIMARY KEY, hash VARCHAR(64) NOT NULL, expires BIGINT NOT NULL, attempts INT NOT NULL)");
    db.execute("CREATE TABLE IF NOT EXISTS beta_pass (id VARCHAR(36) PRIMARY KEY, code_hash VARCHAR(64) NOT NULL UNIQUE, created BIGINT NOT NULL, redeemed_by VARCHAR(254), redeemed_at BIGINT, expires_at BIGINT, revoked BOOLEAN DEFAULT FALSE NOT NULL)");
    db.execute("ALTER TABLE beta_pass ADD COLUMN IF NOT EXISTS duration_hours INT DEFAULT 24 NOT NULL");
    db.execute("CREATE TABLE IF NOT EXISTS coach_trial (id INT PRIMARY KEY, starts BIGINT, ends BIGINT)");
    if (db.queryForObject("SELECT COUNT(*) FROM coach_trial", Integer.class) == 0)
      db.update("INSERT INTO coach_trial(id) VALUES(1)");
    db.update("UPDATE coach SET assigned_at=(SELECT starts FROM coach_trial WHERE id=1), expires_at=(SELECT ends FROM coach_trial WHERE id=1) WHERE status='approved' AND assigned_at IS NULL AND (SELECT starts FROM coach_trial WHERE id=1) IS NOT NULL");
    db.update("UPDATE coach SET expires_at=assigned_at+? WHERE status='approved' AND assigned_at IS NOT NULL AND expires_at=assigned_at+?",
        Duration.ofDays(ACCESS_DAYS).toMillis(), Duration.ofDays(3).toMillis());
    for (String permanentEmail : PERMANENT_COACH_EMAILS) {
      if (db.queryForObject("SELECT COUNT(*) FROM coach WHERE email=?", Integer.class, permanentEmail) == 0)
        db.update("INSERT INTO coach(email,status,created) VALUES(?,'permanent',?)", permanentEmail, clock.millis());
      else
        db.update("UPDATE coach SET status='permanent',assigned_at=NULL,expires_at=NULL WHERE email=?",
            permanentEmail);
    }
  }

  String email(String address) {
    if (!AdminService.validEmail(address)) throw new ResponseStatusException(BAD_REQUEST, "Enter a valid email address.");
    return address.trim().toLowerCase(Locale.ROOT);
  }
  private boolean ownerCoachEnabled(String email) {
    if(accounts==null || !email.equals(admin.email()))return false;
    var state=accounts.state(email);
    return state!=null && Boolean.TRUE.equals(state.get("allowed"));
  }

  @Transactional
  public void sendCode(String address) {
    String email = email(address);
    if (accounts != null && accounts.deleted(email)) throw new ResponseStatusException(FORBIDDEN, "This account has been deleted. Contact the owner.");
    if (email.equals(admin.email()) && !PERMANENT_COACH_EMAILS.contains(email) && !ownerCoachEnabled(email))
      throw new ResponseStatusException(BAD_REQUEST, "Use Admin Sign In for the owner account.");
    if (!mail.enabled) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Coach sign-in email is not connected yet. Please contact the app owner.");
    admin.limit("coach-code:" + email, 3);
    String code = String.format("%06d", new SecureRandom().nextInt(1000000));
    db.update("MERGE INTO coach_code KEY(email) VALUES(?,?,?,0)", email, AdminService.digest(email + ":" + code), clock.millis() + 600000);
    mail.text(email, "Your Resume Prospective sign-in code", "Enter this code in Sign In: " + code + "\n\nIt expires in 10 minutes and works once. Free trials require email verification only. Full account and subscription requests require the owner's approval. If you didn't request it, ignore this email.");
  }

  @Transactional
  public String verify(String address, String code) {
    String email = email(address);
    if(code!=null)code=code.strip();
    if ((email.equals(admin.email()) && !PERMANENT_COACH_EMAILS.contains(email) && !ownerCoachEnabled(email))
        || code == null || !code.matches("[0-9]{6}")) return null;
    var rows = db.queryForList("SELECT * FROM coach_code WHERE email=? FOR UPDATE", email);
    if (rows.isEmpty()) return null;
    var row = rows.getFirst();
    if (((Number) row.get("EXPIRES")).longValue() <= clock.millis() || ((Number) row.get("ATTEMPTS")).intValue() >= 5) return null;
    db.update("UPDATE coach_code SET attempts=attempts+1 WHERE email=?", email);
    if (!java.security.MessageDigest.isEqual(AdminService.digest(email + ":" + code).getBytes(java.nio.charset.StandardCharsets.US_ASCII), row.get("HASH").toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII))) return null;
    db.update("DELETE FROM coach_code WHERE email=?", email);
    if (db.queryForObject("SELECT COUNT(*) FROM coach WHERE email=?", Integer.class, email) == 0)
      db.update("INSERT INTO coach(email,status,created) VALUES(?,'pending',?)", email, clock.millis());
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(AdminService.random(32));
    db.update("UPDATE coach SET session_hash=? WHERE email=?", AdminService.digest(token), email);
    return token;
  }

  String issueToken(String email) {
    if (db.queryForObject("SELECT COUNT(*) FROM coach WHERE email=?",Integer.class,email)==0)
      db.update("INSERT INTO coach(email,status,created) VALUES(?,'pending',?)",email,clock.millis());
    String token=Base64.getUrlEncoder().withoutPadding().encodeToString(AdminService.random(32));
    db.update("UPDATE coach SET session_hash=? WHERE email=?",AdminService.digest(token),email);return token;
  }

  boolean isAdmin(HttpSession session) {
    return admin.configured() && Objects.equals(session.getAttribute("adminVersion"), admin.version());
  }

  void requireAdmin(HttpSession session) {
    if (!isAdmin(session)) throw new ResponseStatusException(FORBIDDEN, "Only the administrator can manage accounts and settings.");
  }

  Map<String, Object> trial() {
    var row = db.queryForMap("SELECT starts, ends FROM coach_trial WHERE id=1");
    long start = row.get("STARTS") == null ? 0 : ((Number) row.get("STARTS")).longValue();
    long end = row.get("ENDS") == null ? 0 : ((Number) row.get("ENDS")).longValue();
    return Map.of("startsAt", start, "expiresAt", end, "active", start > 0 && clock.millis() >= start && clock.millis() < end, "started", start > 0);
  }

  Map<String, Object> coachTrial(Map<String, Object> row) {
    long start = row.get("ASSIGNED_AT") == null ? 0 : ((Number) row.get("ASSIGNED_AT")).longValue();
    long end = row.get("EXPIRES_AT") == null ? 0 : ((Number) row.get("EXPIRES_AT")).longValue();
    return Map.of("startsAt", start, "expiresAt", end, "started", start > 0,
        "active", start > 0 && clock.millis() >= start && clock.millis() < end);
  }

  Map<String, Object> state(HttpSession session) {
    var trial = trial();
    if (isAdmin(session)) return Map.of("role", "admin", "state", "active", "allowed", true, "email", admin.email(), "trial", trial);
    String email = Objects.toString(session.getAttribute("coachEmail"), "");
    String token = Objects.toString(session.getAttribute("coachToken"), "");
    var rows = db.queryForList("SELECT status, session_hash, assigned_at, expires_at FROM coach WHERE email=?", email);
    if (token.isBlank() || rows.isEmpty() || !Objects.equals(rows.getFirst().get("SESSION_HASH"), AdminService.digest(token)))
      return Map.of("role", "guest", "state", "sign-in", "allowed", false, "trial", trial);
    if (accounts != null) { var registered = accounts.state(email); if (registered != null) return registered; }
    if (PERMANENT_COACH_EMAILS.contains(email) && "permanent".equals(rows.getFirst().get("STATUS")))
      return Map.of("role", "coach", "state", "active", "allowed", true, "email", email,
          "accessKind", "permanent-coach", "trial", Map.of("startsAt", 0L, "expiresAt", 0L,
              "started", true, "active", true));
    trial = coachTrial(rows.getFirst());
    String accountStatus = rows.getFirst().get("STATUS").toString();
    if (!accountStatus.equals("revoked") && !(accountStatus.equals("approved") && (boolean) trial.get("active"))) {
      var passes = db.queryForList("SELECT redeemed_at,expires_at,revoked,duration_hours FROM beta_pass WHERE redeemed_by=? ORDER BY redeemed_at DESC", email);
      if (!passes.isEmpty()) {
        var pass = passes.getFirst();
        long start = ((Number)pass.get("REDEEMED_AT")).longValue();
        long end = ((Number)pass.get("EXPIRES_AT")).longValue();
        boolean revoked = Boolean.TRUE.equals(pass.get("REVOKED"));
        boolean active = !revoked && clock.millis() >= start && clock.millis() < end;
        return Map.of("role", "coach", "state", revoked ? "revoked" : active ? "active" : "expired", "allowed", active,
            "email", email, "accessKind", "beta-pass", "trial", Map.of("startsAt", start, "expiresAt", end, "started", true, "active", active));
      }
    }
    String status = rows.getFirst().get("STATUS").toString();
    String state = !status.equals("approved") ? status : !(boolean) trial.get("started") ? "not-started" : (boolean) trial.get("active") ? "active" : "expired";
    return Map.of("role", "coach", "state", state, "allowed", state.equals("active"), "email", email, "trial", trial);
  }

  void requireTools(HttpSession session) {
    var state = state(session);
    if ((boolean) state.get("allowed")) return;
    String message = switch (state.get("state").toString()) {
      case "pending" -> "Your coach account is awaiting administrator approval.";
      case "not-started" -> "Your account is approved. The administrator has not assigned your access yet.";
      case "expired" -> "Your coach access period has ended.";
      case "revoked" -> "Your coach access has been revoked. Contact the app owner.";
      default -> "Please use Coach Sign In or Admin Sign In to continue.";
    };
    throw new ResponseStatusException(state.get("role").equals("guest") ? UNAUTHORIZED : FORBIDDEN, message);
  }

  @Transactional
  public boolean requestAccess(String address) {
    String coachEmail = email(address);
    if (coachEmail.equals(admin.email()) && !PERMANENT_COACH_EMAILS.contains(coachEmail))
      throw new ResponseStatusException(BAD_REQUEST, "Use Admin Sign In for the owner account.");
    var rows = db.queryForList("SELECT status,created FROM coach WHERE email=?", coachEmail);
    if (!rows.isEmpty()) {
      if ("approved".equals(rows.getFirst().get("STATUS")) && isApproved(coachEmail)) return false;
      if ("permanent".equals(rows.getFirst().get("STATUS"))) return false;
      if ("pending".equals(rows.getFirst().get("STATUS"))
          && clock.millis() - ((Number) rows.getFirst().get("CREATED")).longValue() < Duration.ofDays(1).toMillis())
        return false;
      db.update("UPDATE coach SET status='pending',created=?,session_hash=NULL WHERE email=?", clock.millis(), coachEmail);
      return true;
    }
    db.update("INSERT INTO coach(email,status,created) VALUES(?,'pending',?)", coachEmail, clock.millis());
    return true;
  }

  void requireCodeEligibility(String address) {
    String coachEmail = email(address);
    if (coachEmail.equals(admin.email()) && !PERMANENT_COACH_EMAILS.contains(coachEmail))
      throw new ResponseStatusException(BAD_REQUEST, "Use Admin Sign In for the owner account.");
    var rows = db.queryForList("SELECT status,expires_at FROM coach WHERE email=?", coachEmail);
    if (rows.isEmpty() || !List.of("approved", "permanent").contains(rows.getFirst().get("STATUS")))
      throw new ResponseStatusException(FORBIDDEN, "Request coach access and wait for the administrator to approve it before asking for a sign-in code.");
    if ("approved".equals(rows.getFirst().get("STATUS"))
        && (!(rows.getFirst().get("EXPIRES_AT") instanceof Number expiry)
            || expiry.longValue() <= clock.millis()))
      throw new ResponseStatusException(FORBIDDEN, "Your coach access has ended. Contact the administrator for a new assignment.");
  }

  boolean isApproved(String address) {
    String coachEmail = email(address);
    var rows = db.queryForList("SELECT status,expires_at FROM coach WHERE email=?", coachEmail);
    if (rows.isEmpty()) return false;
    var row = rows.getFirst();
    return "permanent".equals(row.get("STATUS")) || "approved".equals(row.get("STATUS"))
        && row.get("EXPIRES_AT") instanceof Number expiry && expiry.longValue() > clock.millis();
  }

  Map<String, Object> dashboard() {
    var coaches = db.query("SELECT email,status,created,assigned_at,expires_at FROM coach ORDER BY created,email", (r, i) -> {
      long expires = r.getLong("expires_at");
      String status = r.getString("status");
      if (status.equals("approved") && expires <= clock.millis()) status = "expired";
      return Map.of("email", r.getString("email"), "status", status, "requestedAt", r.getLong("created"),
          "assignedAt", r.getLong("assigned_at"), "expiresAt", expires);
    });
    return Map.of("coaches", coaches, "limit", COACH_LIMIT, "accessDays", ACCESS_DAYS);
  }

  @Transactional
  public void setStatus(String address, String status) {
    String email = email(address);
    if (PERMANENT_COACH_EMAILS.contains(email))
      throw new ResponseStatusException(BAD_REQUEST, "This coach's permanent access cannot be changed here.");
    if (email.equals(admin.email())) throw new ResponseStatusException(BAD_REQUEST, "The owner already has admin access.");
    if (!List.of("approved", "revoked").contains(status)) throw new ResponseStatusException(BAD_REQUEST, "Choose assign or revoke.");
    db.queryForMap("SELECT * FROM coach_trial WHERE id=1 FOR UPDATE");
    var current = db.queryForList("SELECT * FROM coach WHERE email=?", email);
    long now = clock.millis();
    if (status.equals("revoked")) {
      db.update("UPDATE coach SET status='revoked',session_hash=NULL WHERE email=?", email);
      return;
    }
    if (!current.isEmpty() && current.getFirst().get("STATUS").equals("approved")
        && (boolean) coachTrial(current.getFirst()).get("active")) return;
    if (db.queryForObject("SELECT COUNT(*) FROM coach WHERE status='approved' AND expires_at>?", Integer.class, now) >= COACH_LIMIT)
      throw new ResponseStatusException(CONFLICT, "All 21 coach spaces are assigned. Revoke a coach or wait for a space to expire.");
    if (current.isEmpty()) db.update("INSERT INTO coach(email,status,created) VALUES(?,'pending',?)", email, now);
    db.update("UPDATE coach SET status='approved',assigned_at=?,expires_at=?,session_hash=NULL WHERE email=?",
        now, now + Duration.ofDays(ACCESS_DAYS).toMillis(), email);
  }

  void signOut(HttpSession session) {
    String email = Objects.toString(session.getAttribute("coachEmail"), "");
    String token = Objects.toString(session.getAttribute("coachToken"), "");
    db.update("UPDATE coach SET session_hash=NULL WHERE email=? AND session_hash=?", email, AdminService.digest(token));
    session.invalidate();
  }
}
