package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class AppSumoCodes {
  private final JdbcTemplate db;
  private final Clock clock;
  AppSumoCodes(JdbcTemplate db, Clock clock) {
    this.db=db; this.clock=clock;
    db.execute("CREATE TABLE IF NOT EXISTS appsumo_batch(id VARCHAR(36) PRIMARY KEY, enabled BOOLEAN NOT NULL DEFAULT FALSE, duration_days INT NOT NULL DEFAULT 30, created BIGINT NOT NULL)");
    db.execute("CREATE TABLE IF NOT EXISTS appsumo_code(code_hash VARCHAR(64) PRIMARY KEY, batch_id VARCHAR(36) NOT NULL, redeemed_by VARCHAR(254) UNIQUE, redeemed_at BIGINT, expires_at BIGINT, revoked BOOLEAN NOT NULL DEFAULT FALSE)");
  }
  @Transactional
  public Map<String,Object> importCodes(String csv) {
    if(csv==null || csv.length()>2_100_000) throw new ResponseStatusException(BAD_REQUEST,"Choose a single-column codes CSV under 2.1 MB.");
    var lines=new ArrayList<>(csv.strip().replace("\uFEFF", "").lines().toList());
    if(!lines.isEmpty() && lines.getFirst().equalsIgnoreCase("code"))lines.removeFirst();
    if(lines.size()<1000 || lines.size()>10000)throw new ResponseStatusException(BAD_REQUEST,"Import between 1,000 and 10,000 codes.");
    var hashes=new HashSet<String>();
    for(String line:lines) {
      if(!line.matches("[A-Za-z0-9_-]{16,200}") || !hashes.add(AdminService.digest(line)))
        throw new ResponseStatusException(BAD_REQUEST,"Codes must be unique, with 16–200 letters, digits, hyphens or underscores, one per line.");
    }
    String id=UUID.randomUUID().toString();
    db.update("INSERT INTO appsumo_batch(id,created) VALUES(?,?)",id,clock.millis());
    try { for(String hash:hashes)db.update("INSERT INTO appsumo_code(code_hash,batch_id) VALUES(?,?)",hash,id); }
    catch(org.springframework.dao.DuplicateKeyException e){throw new ResponseStatusException(CONFLICT,"This file includes codes already imported. No codes were imported.");}
    return Map.of("id",id,"count",hashes.size(),"enabled",false);
  }
  List<Map<String,Object>> batches() {
    return db.queryForList("SELECT b.id,b.enabled,b.duration_days,b.created,COUNT(c.code_hash) AS total,COUNT(c.redeemed_by) AS redeemed FROM appsumo_batch b LEFT JOIN appsumo_code c ON c.batch_id=b.id GROUP BY b.id,b.enabled,b.duration_days,b.created ORDER BY b.created DESC");
  }
  @Transactional
  public void configure(String id,boolean enabled) {
    var rows=db.queryForList("SELECT * FROM appsumo_batch WHERE id=? FOR UPDATE",id);
    if(rows.isEmpty())throw new ResponseStatusException(NOT_FOUND,"Batch not found.");
    db.update("UPDATE appsumo_batch SET enabled=? WHERE id=?",enabled,id);
  }
  @Transactional
  public void redeem(String email,String code) {
    if(code==null || !code.matches("[A-Za-z0-9_-]{16,200}"))throw invalid();
    var rows=db.queryForList("SELECT c.*,b.enabled,b.duration_days FROM appsumo_code c JOIN appsumo_batch b ON c.batch_id=b.id WHERE c.code_hash=? FOR UPDATE",AdminService.digest(code));
    if(rows.isEmpty())throw invalid();
    var r=rows.getFirst();
    if(email.equals(r.get("REDEEMED_BY")) && !Boolean.TRUE.equals(r.get("REVOKED")))return;
    if(r.get("REDEEMED_BY")!=null || Boolean.TRUE.equals(r.get("REVOKED")))throw invalid();
    if(!Boolean.TRUE.equals(r.get("ENABLED")))throw new ResponseStatusException(CONFLICT,"This code batch is not active yet. Please contact support.");
    try { db.update("UPDATE appsumo_code SET redeemed_by=?,redeemed_at=?,expires_at=? WHERE code_hash=?",email,clock.millis(),clock.millis()+Duration.ofDays(30).toMillis(),AdminService.digest(code)); }
    catch(org.springframework.dao.DuplicateKeyException e){throw new ResponseStatusException(CONFLICT,"This account already has an AppSumo code. Code stacking is not enabled.");}
  }
  private ResponseStatusException invalid(){return new ResponseStatusException(BAD_REQUEST,"This AppSumo code is invalid, already claimed, or revoked.");}
  Map<String,Object> entitlement(String email) {
    var rows=db.queryForList("SELECT revoked,expires_at FROM appsumo_code WHERE redeemed_by=?",email);
    if(rows.isEmpty())return null;
    var r=rows.getFirst();
    boolean revoked=Boolean.TRUE.equals(r.get("REVOKED"));
    long expires=((Number)r.get("EXPIRES_AT")).longValue();
    return Map.of("active",!revoked && expires>clock.millis(),"revoked",revoked,"expiresAt",expires);
  }
  void revoke(String email,boolean revoked) {
    if(db.update("UPDATE appsumo_code SET revoked=? WHERE redeemed_by=?",revoked,email)!=1)
      throw new ResponseStatusException(NOT_FOUND,"No redeemed AppSumo code for this account.");
  }
}
