package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class HelpDocuments {
  static final String DOCX_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private final JdbcTemplate db;
  private final Clock clock;
  private final String publicUrl;
  record Document(String filename, byte[] bytes) {}
  record Link(String token, String url) {}
  HelpDocuments(JdbcTemplate db, Clock clock, @Value("${app.public-url}") String publicUrl) {
    this.db = db; this.clock = clock; this.publicUrl = publicUrl;
    db.execute("CREATE TABLE IF NOT EXISTS help_document (token_hash VARCHAR(64) PRIMARY KEY, filename VARCHAR(255) NOT NULL, document BLOB NOT NULL, expires BIGINT NOT NULL)");
    cleanup();
  }
  Link store(String filename, byte[] bytes) {
    URI base;
    try {
      base = URI.create(publicUrl);
      if (!"https".equalsIgnoreCase(base.getScheme()) || base.getHost() == null || base.getUserInfo() != null
          || base.getHost().equalsIgnoreCase("localhost") || base.getHost().startsWith("127.")) throw new IllegalArgumentException();
    } catch (Exception e) {
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "The shared service URL must be configured before sending a resume-help link. You can still download your Word document.");
    }
    cleanup();
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(AdminService.random(32));
    db.update("INSERT INTO help_document(token_hash,filename,document,expires) VALUES(?,?,?,?)",
        AdminService.digest(token), filename, bytes, clock.millis() + Duration.ofDays(7).toMillis());
    return new Link(token, base.resolve("/api/help-documents/" + token).toString());
  }
  Document get(String token) {
    cleanup();
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw missing();
    var records = db.query("SELECT filename,document FROM help_document WHERE token_hash=? AND expires>?",
        (r, i) -> new Document(r.getString("filename"), r.getBytes("document")), AdminService.digest(token), clock.millis());
    if (records.isEmpty()) throw missing();
    return records.getFirst();
  }
  void remove(String token) { db.update("DELETE FROM help_document WHERE token_hash=?", AdminService.digest(token)); }
  @Scheduled(fixedDelay = 3600000)
  public void cleanup() { db.update("DELETE FROM help_document WHERE expires<=?", clock.millis()); }
  private ResponseStatusException missing() { return new ResponseStatusException(NOT_FOUND, "This resume link is invalid or has expired."); }
}
