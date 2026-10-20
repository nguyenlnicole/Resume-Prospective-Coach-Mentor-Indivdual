package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
class WorkflowDrafts {
  static final Set<String> PAGES = Set.of("create-resume", "home", "format", "overall", "invitation", "preferences", "questions", "approval", "edit", "layout", "complete", "experience-feedback", "upload-preview");
  static final List<String> CONTEXT = List.of("reviewedLevel", "reviewUserEmail", "reviewResult", "reviewCoachEmail", "coachHistoryId", "reviewFormatRating", "reviewOverallRating", "builderStartNotified");
  private final JdbcTemplate db;
  private final ObjectMapper mapper;
  private final Clock clock;
  WorkflowDrafts(JdbcTemplate db, ObjectMapper mapper, Clock clock) {
    this.db=db; this.mapper=mapper; this.clock=clock;
    db.execute("CREATE TABLE IF NOT EXISTS workflow_draft (owner VARCHAR(254) PRIMARY KEY, snapshot CLOB NOT NULL, context CLOB NOT NULL, original_file BLOB, original_name VARCHAR(255), original_type VARCHAR(120), updated_at BIGINT NOT NULL)");
  }
  Map<String,Object> info(String owner) {
    var rows=db.queryForList("SELECT updated_at FROM workflow_draft WHERE owner=?",owner);
    return rows.isEmpty()?Map.of("available",false):Map.of("available",true,"updatedAt",rows.getFirst().get("UPDATED_AT"));
  }
  @org.springframework.transaction.annotation.Transactional
  public Map<String,Object> save(String owner, String text, MultipartFile file, HttpSession session) throws Exception {
    if(text==null || text.length()>2_000_000)throw new ResponseStatusException(BAD_REQUEST,"This draft is too large to save.");
    JsonNode snapshot;
    try { snapshot=mapper.readTree(text); } catch(com.fasterxml.jackson.core.JsonProcessingException error) { throw new ResponseStatusException(BAD_REQUEST,"This draft could not be saved. Please try again."); }
    if(snapshot==null || !snapshot.isObject() || snapshot.path("version").asInt()!=1 || !PAGES.contains(snapshot.path("page").asText()))
      throw new ResponseStatusException(BAD_REQUEST,"Choose a resume-building page to save.");
    if(file!=null && file.getSize()>10*1024*1024)throw new ResponseStatusException(BAD_REQUEST,"Choose a resume file of 10 MB or less.");
    var context=mapper.createObjectNode();
    for(String key:CONTEXT)if(snapshot.path("review").isObject() && session.getAttribute(key)!=null)context.set(key,mapper.valueToTree(session.getAttribute(key)));
    long updated=clock.millis();
    String historyId = snapshot.path("review").path("historyId").asText("");
    if (!historyId.isBlank()) {
      if (!historyId.equals(session.getAttribute("coachHistoryId")))
        throw new ResponseStatusException(CONFLICT,"Another resume was opened in this session. Reopen this review before saving.");
      if (db.update("UPDATE coach_review_history SET workflow_snapshot=?,workflow_context=? WHERE id=? AND coach_email=?",
          snapshot.toString(), context.toString(), historyId, owner) == 0)
        throw new ResponseStatusException(NOT_FOUND,"This review is no longer in Recent reviews.");
    }
    db.update("MERGE INTO workflow_draft(owner,snapshot,context,original_file,original_name,original_type,updated_at) KEY(owner) VALUES(?,?,?,?,?,?,?)",
        owner,snapshot.toString(),context.toString(),file==null?null:file.getBytes(),file==null?null:Objects.toString(file.getOriginalFilename(),"resume").replaceAll("[^a-zA-Z0-9._-]","_"),file==null?null:Objects.toString(file.getContentType(),"application/octet-stream"),updated);
    return Map.of("available",true,"updatedAt",updated);
  }
  Map<String,Object> resumeReview(String owner, String id, HttpSession session) throws Exception {
    var rows = db.queryForList("SELECT workflow_snapshot,workflow_context,original_file,original_name FROM coach_review_history WHERE id=? AND coach_email=?", id, owner);
    if (rows.isEmpty() || rows.getFirst().get("WORKFLOW_SNAPSHOT") == null)
      throw new ResponseStatusException(NOT_FOUND,"This older review has files only; no saved editing progress is available.");
    var row = rows.getFirst();
    String snapshot = db.queryForObject("SELECT workflow_snapshot FROM coach_review_history WHERE id=? AND coach_email=?", String.class, id, owner);
    String context = db.queryForObject("SELECT workflow_context FROM coach_review_history WHERE id=? AND coach_email=?", String.class, id, owner);
    JsonNode trusted = mapper.readTree(context);
    for (String key : CONTEXT) {
      session.removeAttribute(key);
      if (trusted.has(key)) session.setAttribute(key, key.equals("reviewResult") ? trusted.get(key) : mapper.convertValue(trusted.get(key), Object.class));
    }
    session.removeAttribute("savedReviewId");
    String name = row.get("ORIGINAL_NAME").toString();
    String type = CoachReviewHistory.originalType(name);
    return Map.of("snapshot",mapper.readTree(snapshot),"file",Map.of("name",name,"type",type,"base64",Base64.getEncoder().encodeToString((byte[])row.get("ORIGINAL_FILE"))));
  }
  Map<String,Object> resume(String owner,HttpSession session) throws Exception {
    var rows=db.queryForList("SELECT snapshot,context,original_file,original_name,original_type FROM workflow_draft WHERE owner=?",owner);
    if(rows.isEmpty())throw new ResponseStatusException(NOT_FOUND,"No saved progress yet.");
    String snapshot=db.queryForObject("SELECT snapshot FROM workflow_draft WHERE owner=?",String.class,owner);
    String context=db.queryForObject("SELECT context FROM workflow_draft WHERE owner=?",String.class,owner);
    JsonNode trusted=mapper.readTree(context);
    for(String key:CONTEXT){
      session.removeAttribute(key);
      if(trusted.has(key))session.setAttribute(key,key.equals("reviewResult")?trusted.get(key):mapper.convertValue(trusted.get(key),Object.class));
    }
    var row=rows.getFirst();byte[] original=(byte[])row.get("ORIGINAL_FILE");
    return Map.of("snapshot",mapper.readTree(snapshot),"file",original==null?mapper.nullNode():Map.of("name",row.get("ORIGINAL_NAME"),"type",row.get("ORIGINAL_TYPE"),"base64",Base64.getEncoder().encodeToString(original)));
  }
}

@RestController
@RequestMapping("/api/workflow-draft")
class WorkflowDraftController {
  private final WorkflowDrafts drafts;
  private final CoachAccess access;
  WorkflowDraftController(WorkflowDrafts drafts,CoachAccess access){this.drafts=drafts;this.access=access;}
  private String owner(HttpSession session){access.requireTools(session);return Objects.toString(access.state(session).get("email"),"");}
  @GetMapping Map<String,Object> info(HttpSession session,jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");return drafts.info(owner(session));}
  @PostMapping Map<String,Object> save(@RequestParam String snapshot,@RequestParam(required=false) MultipartFile file,HttpSession session)throws Exception{return drafts.save(owner(session),snapshot,file,session);}
  @PostMapping("/resume") Map<String,Object> resume(HttpSession session,jakarta.servlet.http.HttpServletResponse response)throws Exception{response.setHeader("Cache-Control","no-store");return drafts.resume(owner(session),session);}
  @PostMapping("/review/{id}/resume") Map<String,Object> resumeReview(@PathVariable String id,HttpSession session,jakarta.servlet.http.HttpServletResponse response)throws Exception{response.setHeader("Cache-Control","no-store");return drafts.resumeReview(owner(session),id,session);}
}
