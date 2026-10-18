package com.pnbpaws.resume;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
class CoachApprovalLinks {
  private final JdbcTemplate db;
  private final Clock clock;
  private final CoachAccess access;
  private final BetaPasses betaPasses;

  CoachApprovalLinks(JdbcTemplate db, Clock clock, CoachAccess access) {
    this(db, clock, access, null);
  }

  @org.springframework.beans.factory.annotation.Autowired
  CoachApprovalLinks(JdbcTemplate db, Clock clock, CoachAccess access, BetaPasses betaPasses) {
    this.betaPasses = betaPasses;
    this.db = db;
    this.clock = clock;
    this.access = access;
    db.execute("CREATE TABLE IF NOT EXISTS coach_approval_link (token_hash VARCHAR(64) PRIMARY KEY, "
        + "coach_email VARCHAR(254) NOT NULL, expires BIGINT NOT NULL)");
    db.execute("ALTER TABLE coach_approval_link ADD COLUMN IF NOT EXISTS edition VARCHAR(10) DEFAULT 'coach' NOT NULL");
  }

  @Transactional
  public String create(String email) {
    return create(email, "coach");
  }

  @Transactional
  public String create(String email, String edition) {
    db.update("DELETE FROM coach_approval_link WHERE coach_email=? AND edition=?", email, edition);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(AdminService.random(32));
    db.update("INSERT INTO coach_approval_link(token_hash,coach_email,expires,edition) VALUES(?,?,?,?)",
        AdminService.digest(token), email, clock.millis() + Duration.ofDays(7).toMillis(), edition);
    return token;
  }

  Map<String, Object> details(String token) {
    var row = validRow(token, false);
    return Map.of("email", row.get("COACH_EMAIL"), "expiresAt", row.get("EXPIRES"), "edition", row.get("EDITION"));
  }

  @Transactional
  public String approve(String token) {
    var row = validRow(token, true);
    String email = row.get("COACH_EMAIL").toString();
    if ("beta".equals(row.get("EDITION"))) betaPasses.approveRequest(email);
    else access.setStatus(email, "approved");
    access.sendCode(email);
    db.update("DELETE FROM coach_approval_link WHERE token_hash=?", AdminService.digest(token));
    return email;
  }

  private Map<String, Object> validRow(String token, boolean lock) {
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}"))
      throw new ResponseStatusException(NOT_FOUND, "This approval link is invalid or has expired.");
    var rows = db.queryForList("SELECT coach_email,expires,edition FROM coach_approval_link WHERE token_hash=?"
        + (lock ? " FOR UPDATE" : ""), AdminService.digest(token));
    if (rows.isEmpty() || ((Number) rows.getFirst().get("EXPIRES")).longValue() <= clock.millis())
      throw new ResponseStatusException(NOT_FOUND, "This approval link is invalid or has expired.");
    return rows.getFirst();
  }
}
