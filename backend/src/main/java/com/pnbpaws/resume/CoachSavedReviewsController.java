package com.pnbpaws.resume;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/coach/saved")
class CoachSavedReviewsController {
  private final JdbcTemplate db;
  private final CoachAccess access;
  private final ObjectMapper mapper;
  CoachSavedReviewsController(JdbcTemplate db, CoachAccess access, ObjectMapper mapper) {
    this.db=db; this.access=access; this.mapper=mapper;
    db.execute("CREATE TABLE IF NOT EXISTS coach_saved_review (id VARCHAR(36) PRIMARY KEY, owner VARCHAR(254) NOT NULL, first_name VARCHAR(100), saved_at BIGINT, payload CLOB NOT NULL)");
  }
  private String owner(HttpSession session) {
    var state=access.state(session);
    if (!Boolean.TRUE.equals(state.get("allowed")) || !"coach".equals(state.get("role")) || "beta-pass".equals(state.get("accessKind")))
      throw new ResponseStatusException(FORBIDDEN,"Coach and Mentor access is required.");
    return Objects.toString(state.get("email"), "");
  }
  @GetMapping List<Map<String,Object>> list(HttpSession session) {
    return db.query("SELECT id,first_name,saved_at FROM coach_saved_review WHERE owner=? ORDER BY saved_at DESC", (r,i)->Map.of("id",r.getString(1),"firstName",r.getString(2),"savedAt",r.getLong(3)),owner(session));
  }
  @PostMapping Map<String,String> save(HttpSession session) {
    String coach=owner(session);
    Object reviewed=session.getAttribute("reviewResult");
    if (!(reviewed instanceof JsonNode review)) throw new ResponseStatusException(CONFLICT,"Review a resume first.");
    String id=Objects.toString(session.getAttribute("savedReviewId"),UUID.randomUUID().toString());
    var data=mapper.createObjectNode(); data.set("review",review);
    for(String key:List.of("reviewedLevel","reviewUserEmail","coachHistoryId","reviewFormatRating","reviewOverallRating"))
      data.put(key,Objects.toString(session.getAttribute(key),""));
    String first=review.path("resume").path("contact").path("name").asText("Client").strip().split("\\s+")[0];
    if(first.isBlank()) first="Client";
    if(first.length()>100) first=first.substring(0,100);
    db.update("MERGE INTO coach_saved_review (id,owner,first_name,saved_at,payload) KEY(id) VALUES(?,?,?,?,?)",id,coach,first,System.currentTimeMillis(),data.toString());
    session.setAttribute("savedReviewId",id);
    return Map.of("id",id);
  }
  @PostMapping("/{id}/open") JsonNode open(@PathVariable String id,HttpSession session) throws Exception {
    String coach=owner(session);
    var rows=db.query("SELECT payload FROM coach_saved_review WHERE id=? AND owner=?",(r,i)->r.getString(1),id,coach);
    if(rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND,"Saved review not found.");
    JsonNode data=mapper.readTree(rows.getFirst());
    for(String key:List.of("reviewedLevel","reviewUserEmail","coachHistoryId","reviewFormatRating","reviewOverallRating")) session.setAttribute(key,data.path(key).asText());
    session.setAttribute("reviewResult",data.path("review")); session.setAttribute("reviewCoachEmail",coach); session.setAttribute("savedReviewId",id);
    for(String key:List.of("builderStartNotified","coachCopy:draft","coachCopy:final","coachAiRated")) session.removeAttribute(key);
    return data;
  }
  @DeleteMapping("/{id}") Map<String,Boolean> delete(@PathVariable String id,HttpSession session) {
    if(db.update("DELETE FROM coach_saved_review WHERE id=? AND owner=?",id,owner(session))==0) throw new ResponseStatusException(NOT_FOUND,"Saved review not found.");
    if(id.equals(session.getAttribute("savedReviewId"))) session.removeAttribute("savedReviewId");
    return Map.of("ok",true);
  }
}
