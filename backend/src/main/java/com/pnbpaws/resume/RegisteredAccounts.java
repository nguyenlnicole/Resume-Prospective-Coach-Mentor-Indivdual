package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.Callable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class RegisteredAccounts {
  @org.springframework.beans.factory.annotation.Autowired AppSumoCodes appSumo;
  @org.springframework.beans.factory.annotation.Value("${app.review-email:review@example.com}")
  String trialNotificationInbox = "review@example.com";
  private final JdbcTemplate db;
  private final AdminService admin;
  private final MailService mail;
  private final Clock clock;
  private final ObjectMapper mapper;
  private final Pbkdf2PasswordEncoder passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  RegisteredAccounts(JdbcTemplate db, AdminService admin, MailService mail, Clock clock, ObjectMapper mapper) {
    this.db=db; this.admin=admin; this.mail=mail; this.clock=clock; this.mapper=mapper;
    db.execute("CREATE TABLE IF NOT EXISTS registered_account(email VARCHAR(254) PRIMARY KEY, password_hash VARCHAR(512) NOT NULL, edition VARCHAR(16) NOT NULL, approval VARCHAR(24) NOT NULL DEFAULT 'pending', verified BOOLEAN NOT NULL DEFAULT FALSE, subscription_requested BOOLEAN NOT NULL DEFAULT FALSE, created BIGINT NOT NULL, review_state VARCHAR(16) NOT NULL DEFAULT 'free', build_state VARCHAR(16) NOT NULL DEFAULT 'free', review_payload CLOB, review_level VARCHAR(40), resume_email VARCHAR(254), build_payload CLOB)");
    db.execute("ALTER TABLE registered_account ADD COLUMN IF NOT EXISTS subscription_ends BIGINT");
    db.execute("ALTER TABLE registered_account ADD COLUMN IF NOT EXISTS trial_notice_pending BOOLEAN NOT NULL DEFAULT FALSE");
    db.execute("ALTER TABLE registered_account ADD COLUMN IF NOT EXISTS review_count INT");
    db.execute("ALTER TABLE registered_account ADD COLUMN IF NOT EXISTS build_count INT");
    db.update("UPDATE registered_account SET review_count=CASE WHEN review_state='used' OR review_payload IS NOT NULL THEN 1 ELSE 0 END WHERE review_count IS NULL");
    db.update("UPDATE registered_account SET build_count=CASE WHEN build_state='used' OR build_payload IS NOT NULL THEN 1 ELSE 0 END WHERE build_count IS NULL");
    db.execute("ALTER TABLE registered_account ALTER COLUMN review_count SET DEFAULT 0");
    db.execute("ALTER TABLE registered_account ALTER COLUMN build_count SET DEFAULT 0");
    db.execute("CREATE TABLE IF NOT EXISTS workflow_draft (owner VARCHAR(254) PRIMARY KEY, snapshot CLOB NOT NULL, context CLOB NOT NULL, original_file BLOB, original_name VARCHAR(255), original_type VARCHAR(120), updated_at BIGINT NOT NULL)");
    db.execute("CREATE TABLE IF NOT EXISTS account_password_reset(token_hash VARCHAR(64) PRIMARY KEY,email VARCHAR(254) NOT NULL,expires BIGINT NOT NULL)");
  }
  @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
  synchronized void sendTrialNotifications() {
    for (String email : db.queryForList("SELECT email FROM registered_account WHERE trial_notice_pending=TRUE", String.class)) {
      try {
        mail.text(trialNotificationInbox,"Free trial used — Resume Prospective",email+" used a free resume review. Each email has two free reviews and builds total; usage stays saved across updates.");
        db.update("UPDATE registered_account SET trial_notice_pending=FALSE WHERE email=?",email);
      } catch (RuntimeException failure) {
        org.slf4j.LoggerFactory.getLogger(RegisteredAccounts.class).warn("Trial notification delivery failed; will retry.", failure);
      }
    }
  }
  static boolean validPassword(String password) {
    return password != null && password.length() >= 8 && password.length() <= 128
      && password.chars().filter(c -> c >= '0' && c <= '9').count() >= 2
      && password.codePoints().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
  }
  boolean exists(String email) { return db.queryForObject("SELECT COUNT(*) FROM registered_account WHERE email=?", Integer.class,email)>0; }
  boolean deleted(String email) { return db.queryForObject("SELECT COUNT(*) FROM registered_account WHERE email=? AND approval='deleted'", Integer.class,email)>0; }
  void requestPasswordReset(String email) {
    var rows=db.queryForList("SELECT edition FROM registered_account WHERE email=? AND verified=TRUE AND approval<>'deleted' AND password_hash<>''",email);
    if(rows.isEmpty() && !email.equals(admin.email()))return;
    String token=Base64.getUrlEncoder().withoutPadding().encodeToString(AdminService.random(32));
    db.update("DELETE FROM account_password_reset WHERE email=? OR expires<=?",email,clock.millis());
    db.update("INSERT INTO account_password_reset(token_hash,email,expires) VALUES(?,?,?)",AdminService.digest(token),email,clock.millis()+1800000);
    String site=email.equals(admin.email()) || "coach".equals(rows.getFirst().get("EDITION"))?"https://crp.pnbpaws.com":"https://resumep.pnbpaws.com";
    mail.text(email,"Reset your Resume Prospective password","Use this link to choose a new password:\n\n"+site+"/#account-reset="+token+"\n\nThis link expires in 30 minutes and works once. If you did not request it, ignore this email.");
  }
  @org.springframework.transaction.annotation.Transactional
  public void resetPassword(String token,String password) {
    if(token==null || !token.matches("[A-Za-z0-9_-]{43}"))throw new ResponseStatusException(BAD_REQUEST,"This reset link is invalid or expired.");
    if(!validPassword(password))throw new ResponseStatusException(BAD_REQUEST,"Use 8–128 characters, including at least 2 digits and 1 symbol.");
    String hash=AdminService.digest(token);
    var rows=db.queryForList("SELECT email,expires FROM account_password_reset WHERE token_hash=? FOR UPDATE",hash);
    if(rows.isEmpty() || ((Number)rows.getFirst().get("EXPIRES")).longValue()<=clock.millis())throw new ResponseStatusException(BAD_REQUEST,"This reset link is invalid or expired.");
    String email=rows.getFirst().get("EMAIL").toString();
    if(email.equals(admin.email())) {
      if(!exists(email)) db.update("INSERT INTO registered_account(email,password_hash,edition,approval,verified,created) VALUES(?,'','coach','subscribed',TRUE,?)",email,clock.millis());
      else db.update("UPDATE registered_account SET verified=TRUE,approval='subscribed',edition='coach' WHERE email=?",email);
    }
    if(db.update("UPDATE registered_account SET password_hash=? WHERE email=? AND verified=TRUE AND approval<>'deleted'",passwords.encode(password),email)!=1)
      throw new ResponseStatusException(BAD_REQUEST,"This reset link is invalid or expired.");
    db.update("DELETE FROM account_password_reset WHERE email=?",email);
    db.update("UPDATE coach SET session_hash=NULL WHERE email=?",email);
  }
  void register(String email,String password,String edition) {
    if(!validPassword(password)) throw new ResponseStatusException(BAD_REQUEST,"Use 8–128 characters, including at least 2 digits and 1 symbol.");
    if(!List.of("coach","individual").contains(edition)) throw new ResponseStatusException(BAD_REQUEST,"Choose a valid account type.");
    if(email.equals(admin.email()) || db.queryForObject("SELECT COUNT(*) FROM coach WHERE email=?",Integer.class,email)>0)
      throw new ResponseStatusException(CONFLICT,"An account already exists. Sign in with your email code.");
    try { db.update("INSERT INTO registered_account(email,password_hash,edition,created) VALUES(?,?,?,?)",email,passwords.encode(password),edition,clock.millis()); }
    catch(org.springframework.dao.DuplicateKeyException e) { throw new ResponseStatusException(CONFLICT,"An account already exists. Sign in or request an email code."); }
  }
  boolean matches(String email,String password) {
    if(password==null || password.length()>128) return false;
    var rows=db.queryForList("SELECT password_hash FROM registered_account WHERE email=? AND approval<>'deleted'",email);
    return !rows.isEmpty() && !rows.getFirst().get("PASSWORD_HASH").toString().isBlank() && passwords.matches(password,rows.getFirst().get("PASSWORD_HASH").toString());
  }
  void startTrial(String email,String edition) {
    if(!List.of("coach","individual").contains(edition)) throw new ResponseStatusException(BAD_REQUEST,"Choose a valid account type.");
    var existing=state(email);
    if(existing!=null && "trial".equals(existing.get("plan")) && "verify-email".equals(existing.get("state")))return;
    if(email.equals(admin.email()) || db.queryForObject("SELECT COUNT(*) FROM coach WHERE email=?",Integer.class,email)>0 || exists(email))
      throw new ResponseStatusException(CONFLICT,"This email already has an account or trial. Log in with an email code to continue it.");
    try { db.update("INSERT INTO registered_account(email,password_hash,edition,approval,created) VALUES(?,'',?,'trial',?)",email,edition,clock.millis()); }
    catch(org.springframework.dao.DuplicateKeyException e) { throw new ResponseStatusException(CONFLICT,"This email already has a trial. Log in to resume it."); }
  }
  void verified(String email) {
    if(db.update("UPDATE registered_account SET verified=TRUE WHERE email=? AND verified=FALSE AND approval<>'deleted'",email)>0 && "pending".equals(state(email).get("plan")))
      mail.text(admin.email(),"Account approval requested — Resume Prospective","A verified user requested an account: "+email+". Review the request at https://adminrp.pnbpaws.com. Ongoing access needs your approval.");
  }
  void requestAccount(String email,String password) {
    if(!validPassword(password))throw new ResponseStatusException(BAD_REQUEST,"Use 8–128 characters, including at least 2 digits and 1 symbol.");
    if(db.update("UPDATE registered_account SET password_hash=?,subscription_requested=TRUE WHERE email=? AND verified=TRUE AND approval<>'deleted'",passwords.encode(password),email)!=1)
      throw new ResponseStatusException(FORBIDDEN,"Verify your email first.");
    mail.text(admin.email(),"Account approval requested — Resume Prospective",email+" requested an account after trying the app. Approve ongoing access at https://adminrp.pnbpaws.com.");
  }
  Map<String,Object> state(String email) {
    var rows=db.queryForList("SELECT * FROM registered_account WHERE email=?",email);
    if(rows.isEmpty())return null;
    var r=rows.getFirst(); String approval=r.get("APPROVAL").toString();
    boolean verified=Boolean.TRUE.equals(r.get("VERIFIED"));
    var deal=appSumo==null?null:appSumo.entitlement(email);
    if(deal!=null && verified && !List.of("deleted","revoked").contains(approval)
        && (Boolean.TRUE.equals(deal.get("active")) || !"subscribed".equals(approval))) {
      boolean active=Boolean.TRUE.equals(deal.get("active"));
      return Map.ofEntries(Map.entry("role","coach"),Map.entry("email",email),Map.entry("registered",true),
        Map.entry("allowed",active),Map.entry("state",active?"active":Boolean.TRUE.equals(deal.get("revoked"))?"revoked":"expired"),
        Map.entry("accessKind","appsumo"),Map.entry("plan","appsumo"),Map.entry("expiresAt",deal.get("expiresAt")),
        Map.entry("reviewUsed",false),Map.entry("buildUsed",false),Map.entry("subscriptionRequested",false),
        Map.entry("trial",Map.of("active",active,"started",true,"startsAt",0L,"expiresAt",deal.get("expiresAt"))));
    }
    long subscriptionEnds=r.get("SUBSCRIPTION_ENDS")==null?0:((Number)r.get("SUBSCRIPTION_ENDS")).longValue();
    boolean expired="subscribed".equals(approval) && subscriptionEnds>0 && clock.millis()>=subscriptionEnds;
    boolean allowed=verified && !expired && List.of("trial","subscribed").contains(approval);
    String status=List.of("deleted","revoked").contains(approval)?approval:!verified?"verify-email":expired?"expired":allowed?"active":approval;
    return Map.ofEntries(Map.entry("role","coach"),Map.entry("email",email),Map.entry("allowed",allowed),Map.entry("state",status),
      Map.entry("accessKind",r.get("EDITION").equals("individual")?"beta-pass":"registered-coach"),Map.entry("registered",true),
      Map.entry("reviewsRemaining",Math.max(0,2-((Number)r.get("REVIEW_COUNT")).intValue())),Map.entry("buildsRemaining",Math.max(0,2-((Number)r.get("BUILD_COUNT")).intValue())),Map.entry("plan",approval),Map.entry("reviewUsed",!r.get("REVIEW_STATE").equals("free")),Map.entry("buildUsed",!r.get("BUILD_STATE").equals("free")),
      Map.entry("expiresAt",subscriptionEnds),Map.entry("subscriptionRequested",r.get("SUBSCRIPTION_REQUESTED")),Map.entry("trial",Map.of("active",allowed,"started",allowed,"startsAt",0L,"expiresAt",0L)));
  }
  List<Map<String,Object>> dashboard() {
    return db.queryForList("SELECT email,edition,approval,verified,subscription_requested,subscription_ends,created,review_state,build_state,(approval='trial' AND password_hash='') AS resettable FROM registered_account ORDER BY created DESC");
  }
  void decide(String email,String decision) {
    if(!List.of("trial","subscribed","revoked").contains(decision))throw new ResponseStatusException(BAD_REQUEST,"Choose trial, subscription, or revoke.");
    if(db.update("UPDATE registered_account SET approval=?,subscription_requested=FALSE WHERE email=? AND verified=TRUE AND approval<>'deleted'",decision,email)!=1)
      throw new ResponseStatusException(CONFLICT,"The account must verify its email before approval.");
    mail.text(email,"Your Resume Prospective account",decision.equals("revoked")?"Your access has been revoked. Contact support for help.":decision.equals("trial")?"Your account is approved for up to two free resume reviews and builds total across all versions. Sign in with your email and password.":"Your subscription request is approved. You can continue using Resume Prospective. No payment was collected.");
  }
  void decide(String email,String decision,Long expiresAt) {
    if(expiresAt!=null && expiresAt<=clock.millis())throw new ResponseStatusException(BAD_REQUEST,"Choose a future subscription end date.");
    if("subscribed".equals(decision))db.update("UPDATE registered_account SET subscription_ends=? WHERE email=? AND verified=TRUE AND approval<>'deleted'",expiresAt,email);
    if("revoked".equals(decision))db.update("UPDATE coach SET session_hash=NULL WHERE email=?",email);
    decide(email,decision);
  }
  void subscribe(String email) {
    if(db.update("UPDATE registered_account SET subscription_requested=TRUE WHERE email=? AND verified=TRUE AND approval NOT IN ('subscribed','deleted') AND subscription_requested=FALSE",email)>0)
      mail.text(admin.email(),"Subscription approval requested — Resume Prospective",email+" requested continued access. Review the request at https://adminrp.pnbpaws.com. No payment has been collected.");
  }
  @org.springframework.transaction.annotation.Transactional
  public void remove(String email,boolean resetTrial) {
    if(email.equals(admin.email())) throw new ResponseStatusException(FORBIDDEN,"The owner account cannot be deleted.");
    var rows=db.queryForList("SELECT approval,password_hash,review_state,build_state FROM registered_account WHERE email=? FOR UPDATE",email);
    if(rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND,"Account not found.");
    var row=rows.getFirst();
    if("running".equals(row.get("REVIEW_STATE")) || "running".equals(row.get("BUILD_STATE")))
      throw new ResponseStatusException(CONFLICT,"Wait for the current review or build to finish.");
    if(resetTrial) {
      if(!"trial".equals(row.get("APPROVAL")) || !row.get("PASSWORD_HASH").toString().isBlank())
        throw new ResponseStatusException(CONFLICT,"Only a free trial without a created account can be reset.");
      db.update("DELETE FROM registered_account WHERE email=?",email);
      db.update("DELETE FROM coach WHERE email=?",email);
    } else {
      db.update("UPDATE registered_account SET approval='deleted',password_hash='',verified=FALSE,subscription_requested=FALSE,trial_notice_pending=FALSE,review_payload=NULL,build_payload=NULL,resume_email=NULL WHERE email=?",email);
      db.update("UPDATE coach SET status='revoked',session_hash=NULL WHERE email=?",email);
    }
    db.update("DELETE FROM coach_code WHERE email=?",email);
    db.update("DELETE FROM account_password_reset WHERE email=?",email);
    db.update("DELETE FROM coach_saved_review WHERE owner=?",email);
    db.update("DELETE FROM workflow_draft WHERE owner=?",email);
  }
  JsonNode runTrial(HttpSession session,String operation,String level,String resumeEmail,Callable<JsonNode> task) throws Exception {
    String email=Objects.toString(session.getAttribute("coachEmail"),"");
    var state=state(email);
    if(state!=null && "appsumo".equals(state.get("plan"))) {
      if(!Boolean.TRUE.equals(state.get("allowed")))throw new ResponseStatusException(FORBIDDEN,"Your AppSumo access has ended or been revoked.");
      return task.call();
    }
    if(state==null || "subscribed".equals(state.get("plan")))return task.call();
    if(!Boolean.TRUE.equals(state.get("allowed")))throw new ResponseStatusException(FORBIDDEN,"Your account needs approval first.");
    if(!List.of("review","build").contains(operation))throw new IllegalArgumentException();
    String column=operation+"_state";
    String counter=operation+"_count";
    if(db.update("UPDATE registered_account SET "+column+"='running' WHERE email=? AND approval='trial' AND "+counter+"<2 AND review_state<>'running' AND build_state<>'running'",email)!=1)
      throw new ResponseStatusException(CONFLICT,"A trial operation is in progress, or both free "+operation+" credits have been used. Resume your saved trial or request continued access.");
    try {
      JsonNode result=task.call();
      if(operation.equals("review")) db.update("UPDATE registered_account SET review_state='used',review_count=review_count+1,build_payload=NULL,trial_notice_pending=TRUE,review_payload=?,review_level=?,resume_email=? WHERE email=?",result.toString(),level,resumeEmail,email);
      else db.update("UPDATE registered_account SET build_state='used',build_count=build_count+1,build_payload=? WHERE email=?",result.toString(),email);
      if(operation.equals("review")) sendTrialNotifications();
      return result;
    } catch(Exception e) { db.update("UPDATE registered_account SET "+column+"=CASE WHEN "+counter+">0 THEN 'used' ELSE 'free' END WHERE email=? AND "+column+"='running'",email);throw e; }
  }
  Map<String,Object> restore(HttpSession session) throws Exception {
    String email=Objects.toString(session.getAttribute("coachEmail"),"");
    var rows=db.queryForList("SELECT review_payload,review_level,resume_email,build_payload FROM registered_account WHERE email=?",email);
    if(rows.isEmpty() || rows.getFirst().get("REVIEW_PAYLOAD")==null)throw new ResponseStatusException(NOT_FOUND,"No saved trial review yet.");
    var row=rows.getFirst();
    String review=db.queryForObject("SELECT review_payload FROM registered_account WHERE email=?",String.class,email);
    String build=db.queryForObject("SELECT build_payload FROM registered_account WHERE email=?",String.class,email);
    JsonNode result=mapper.readTree(review);
    session.setAttribute("reviewedLevel",row.get("REVIEW_LEVEL")); session.setAttribute("reviewUserEmail",row.get("RESUME_EMAIL"));session.setAttribute("reviewResult",result);
    return Map.of("review",result,"level",row.get("REVIEW_LEVEL"),"email",row.get("RESUME_EMAIL"),"build",build==null?mapper.nullNode():mapper.readTree(build));
  }
}
