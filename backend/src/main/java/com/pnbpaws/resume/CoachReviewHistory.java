package com.pnbpaws.resume;

import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
class CoachReviewHistory {
  private final JdbcTemplate db;
  private final Clock clock;

  CoachReviewHistory(JdbcTemplate db, Clock clock) {
    this.db = db;
    this.clock = clock;
    db.execute("CREATE TABLE IF NOT EXISTS coach_review_history (id VARCHAR(36) PRIMARY KEY, "
        + "coach_email VARCHAR(254) NOT NULL, client_email VARCHAR(254) NOT NULL, "
        + "first_name VARCHAR(100) NOT NULL, reviewed_at BIGINT NOT NULL, "
        + "original_name VARCHAR(255) NOT NULL, original_file BLOB NOT NULL, "
        + "feedback_pdf BLOB NOT NULL, final_pdf BLOB, final_docx BLOB)");
    db.execute("CREATE INDEX IF NOT EXISTS coach_review_owner ON coach_review_history(coach_email,reviewed_at)");
    db.execute("ALTER TABLE coach_review_history ADD COLUMN IF NOT EXISTS workflow_snapshot CLOB");
    db.execute("ALTER TABLE coach_review_history ADD COLUMN IF NOT EXISTS workflow_context CLOB");
  }

  void startProgress(String coach, String id, com.fasterxml.jackson.databind.JsonNode review,
      String level, String email, jakarta.servlet.http.HttpSession session) {
    var snapshot = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    snapshot.put("version", 1).put("page", "format").put("level", level).put("email", email)
        .put("profession", Set.of("General", "Medical Field", "Educator").contains(level) ? level : "Technical");
    snapshot.set("review", review); snapshot.set("resume", review.path("resume"));
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var context = mapper.createObjectNode();
    for (String key : WorkflowDrafts.CONTEXT)
      if (session.getAttribute(key) != null) context.set(key, mapper.valueToTree(session.getAttribute(key)));
    db.update("UPDATE coach_review_history SET workflow_snapshot=?,workflow_context=? WHERE id=? AND coach_email=?",
        snapshot.toString(), context.toString(), id, coach);
  }

  @Transactional
  public String saveReview(String coach, String client, String firstName, String originalName,
      byte[] original, byte[] feedback) {
    db.update("DELETE FROM coach_review_history WHERE coach_email=? AND client_email=?", coach, client);
    String id = UUID.randomUUID().toString();
    db.update("INSERT INTO coach_review_history(id,coach_email,client_email,first_name,reviewed_at,"
        + "original_name,original_file,feedback_pdf) VALUES(?,?,?,?,?,?,?,?)",
        id, coach, client, cleanFirstName(firstName, client), clock.millis(),
        safeFilename(originalName), original, feedback);
    var older = db.queryForList("SELECT id FROM coach_review_history WHERE coach_email=? "
        + "ORDER BY reviewed_at DESC,id DESC LIMIT 100 OFFSET 5", String.class, coach);
    for (String old : older) db.update("DELETE FROM coach_review_history WHERE id=? AND coach_email=?", old, coach);
    return id;
  }

  void saveFinal(String coach, String id, byte[] pdf, byte[] docx) {
    if (db.update("UPDATE coach_review_history SET final_pdf=?,final_docx=? WHERE id=? AND coach_email=?",
        pdf, docx, id, coach) == 0) throw new ResponseStatusException(NOT_FOUND, "Review not found.");
  }

  List<Map<String, Object>> list(String coach) {
    return db.query("SELECT id,first_name,reviewed_at,final_pdf,workflow_snapshot FROM coach_review_history "
        + "WHERE coach_email=? ORDER BY reviewed_at DESC,id DESC LIMIT 5", (r, i) -> Map.of(
            "id", r.getString("id"), "firstName", r.getString("first_name"),
            "reviewedAt", r.getLong("reviewed_at"), "hasFinished", r.getBytes("final_pdf") != null,
            "hasProgress", r.getString("workflow_snapshot") != null), coach);
  }

  record Download(String filename, String type, byte[] bytes) {}

  Download download(String coach, String id, String kind) {
    var rows = db.queryForList("SELECT first_name,original_name,original_file,feedback_pdf,final_pdf,final_docx "
        + "FROM coach_review_history WHERE id=? AND coach_email=?", id, coach);
    if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Review not found.");
    var row = rows.getFirst();
    String prefix = row.get("FIRST_NAME") + "-" + id.substring(0, 8);
    return switch (kind) {
      case "original" -> new Download(prefix + "-uploaded-resume" + extension((String) row.get("ORIGINAL_NAME")),
          originalType((String) row.get("ORIGINAL_NAME")), (byte[]) row.get("ORIGINAL_FILE"));
      case "feedback" -> new Download(prefix + "-feedback.pdf", "application/pdf", (byte[]) row.get("FEEDBACK_PDF"));
      case "pdf" -> new Download(prefix + "-finished-resume.pdf", "application/pdf", (byte[]) row.get("FINAL_PDF"));
      case "docx" -> new Download(prefix + "-finished-resume.docx",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document", (byte[]) row.get("FINAL_DOCX"));
      default -> throw new ResponseStatusException(NOT_FOUND, "File not found.");
    };
  }

  private static String cleanFirstName(String name, String email) {
    String source = name == null || name.isBlank() ? email.split("@", 2)[0] : name;
    String first = source.trim().split("[\\s._-]+", 2)[0].replaceAll("[^\\p{L}\\p{N}'-]", "");
    return first.isBlank() ? "Client" : first.substring(0, Math.min(100, first.length()));
  }

  private static String safeFilename(String name) {
    String safe = name == null ? "resume.pdf" : name.replace('\\', '/');
    safe = safe.substring(safe.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N}._ -]", "_");
    return safe.isBlank() ? "resume.pdf" : safe.substring(0, Math.min(safe.length(), 180));
  }

  private static String extension(String name) {
    String safe = safeFilename(name);
    int dot = safe.lastIndexOf('.');
    return dot > 0 && dot < safe.length() - 1 ? safe.substring(dot).toLowerCase(Locale.ROOT) : ".pdf";
  }

  static String originalType(String name) {
    String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".pdf")) return "application/pdf";
    if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    if (lower.endsWith(".doc")) return "application/msword";
    if (lower.endsWith(".png")) return "image/png";
    return "image/jpeg";
  }
}
