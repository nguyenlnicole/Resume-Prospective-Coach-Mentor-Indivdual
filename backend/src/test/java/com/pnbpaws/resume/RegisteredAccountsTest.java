package com.pnbpaws.resume;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
class RegisteredAccountsTest {
 JdbcTemplate db; RegisteredAccounts accounts; MailService mail; ObjectMapper mapper=new ObjectMapper();
 @BeforeEach void setup(){
  db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
  db.execute("CREATE TABLE coach(email VARCHAR(254) PRIMARY KEY)");
  mail=mock(MailService.class);var admin=mock(AdminService.class);when(admin.email()).thenReturn("owner@example.com");
  accounts=new RegisteredAccounts(db,admin,mail,Clock.systemUTC(),mapper);
 }
 @Test void unverifiedTrialCanRequestAnotherCodeWithoutResettingItsRecord(){
  String email="retry-trial@example.com";accounts.startTrial(email,"coach");accounts.startTrial(email,"individual");
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM registered_account WHERE email=?",Integer.class,email));
  assertEquals(false,accounts.state(email).get("allowed"));
  accounts.verified(email);assertEquals(true,accounts.state(email).get("allowed"));
  assertThrows(ResponseStatusException.class,()->accounts.startTrial(email,"coach"));
 }
 @Test void passwordRules(){assertTrue(RegisteredAccounts.validPassword("Resume12!"));assertFalse(RegisteredAccounts.validPassword("Ab12!"));assertFalse(RegisteredAccounts.validPassword("Resume1!"));assertFalse(RegisteredAccounts.validPassword("Resume123"));}
 @Test void trialEmailAndCreditSurviveDatabaseReopen(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory)throws Exception{
  String url="jdbc:h2:file:"+directory.resolve("accounts").toAbsolutePath().toString().replace('\\','/');
  var disk=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
  disk.execute("CREATE TABLE coach(email VARCHAR(254) PRIMARY KEY)");
  var admin=mock(AdminService.class);when(admin.email()).thenReturn("owner@example.com");
  var before=new RegisteredAccounts(disk,admin,mail,Clock.systemUTC(),mapper);
  String email="persistent@example.com";before.startTrial(email,"coach");before.verified(email);
  var session=new MockHttpSession();session.setAttribute("coachEmail",email);
  before.runTrial(session,"review","Senior",email,()->mapper.createObjectNode().put("saved",true));
  disk.execute("SHUTDOWN");
  var reopened=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
  var after=new RegisteredAccounts(reopened,admin,mail,Clock.systemUTC(),mapper);
  assertEquals(true,after.state(email).get("reviewUsed"));
  assertEquals(1,after.state(email).get("reviewsRemaining"));
  assertThrows(ResponseStatusException.class,()->after.startTrial(email,"individual"));
  assertEquals(true,((com.fasterxml.jackson.databind.JsonNode)after.restore(session).get("review")).path("saved").asBoolean());
  reopened.execute("SHUTDOWN");
 }
 @Test void accountNeedsVerificationAndApproval(){
  accounts.register("person@example.com","Resume12!","individual");assertEquals(false,accounts.state("person@example.com").get("allowed"));
  assertThrows(ResponseStatusException.class,()->accounts.decide("person@example.com","subscribed"));
  accounts.verified("person@example.com");assertEquals(false,accounts.state("person@example.com").get("allowed"));
  accounts.decide("person@example.com","subscribed");assertEquals(true,accounts.state("person@example.com").get("allowed"));
  assertTrue(accounts.matches("person@example.com","Resume12!"));assertFalse(accounts.matches("person@example.com","wrong"));
  assertNotEquals("Resume12!",db.queryForObject("SELECT password_hash FROM registered_account",String.class));
 }
 @Test void trialNeedsCodeButNoApprovalAndCannotRepeatAcrossEditions()throws Exception{
  String email="trial@example.com";accounts.startTrial(email,"coach");assertEquals(false,accounts.state(email).get("allowed"));
  accounts.verified(email);assertEquals(true,accounts.state(email).get("allowed"));assertFalse(accounts.matches(email,""));
  assertThrows(ResponseStatusException.class,()->accounts.startTrial(email,"individual"));
  var session=new MockHttpSession();session.setAttribute("coachEmail",email);
  var result=mapper.createObjectNode().put("ok",true);
  assertEquals(result,accounts.runTrial(session,"review","Senior",email,()->result));
  assertEquals(1, accounts.state(email).get("reviewsRemaining"));
  assertEquals(result,accounts.runTrial(session,"review","Senior",email,()->result));
  assertThrows(ResponseStatusException.class,()->accounts.runTrial(session,"review","Senior",email,()->result));
  verify(mail,times(2)).text(eq("review@example.com"),eq("Free trial used — Resume Prospective"),contains(email));
  accounts.runTrial(session,"build","Senior",email,()->result);
  assertEquals(result,accounts.runTrial(session,"build","Senior",email,()->result));
  assertThrows(ResponseStatusException.class,()->accounts.runTrial(session,"build","Senior",email,()->result));
  assertEquals(result,accounts.restore(session).get("review"));
 }
 @Test void failedReviewDoesNotConsumeTrial()throws Exception{
  accounts.startTrial("retry@example.com","individual");accounts.verified("retry@example.com");var session=new MockHttpSession();session.setAttribute("coachEmail","retry@example.com");
  assertThrows(Exception.class,()->accounts.runTrial(session,"review","Senior","retry@example.com",()->{throw new Exception("upstream failed");}));
  assertEquals(false,accounts.state("retry@example.com").get("reviewUsed"));
  accounts.runTrial(session,"review","Senior","retry@example.com",()->mapper.createObjectNode());
 }
 @Test void failedNotificationRetriesWithoutRestoringCredit()throws Exception{
  String email="notice@example.com";accounts.startTrial(email,"individual");accounts.verified(email);
  var session=new MockHttpSession();session.setAttribute("coachEmail",email);
  doThrow(new RuntimeException("temporary mail failure")).doNothing().when(mail).text(eq("review@example.com"),eq("Free trial used — Resume Prospective"),contains(email));
  accounts.runTrial(session,"review","Senior",email,()->mapper.createObjectNode());
  assertEquals(true,accounts.state(email).get("reviewUsed"));
  assertTrue(db.queryForObject("SELECT trial_notice_pending FROM registered_account",Boolean.class));
  accounts.sendTrialNotifications();
  assertFalse(db.queryForObject("SELECT trial_notice_pending FROM registered_account",Boolean.class));
  verify(mail,times(2)).text(eq("review@example.com"),eq("Free trial used — Resume Prospective"),contains(email));
  assertEquals(1,accounts.state(email).get("reviewsRemaining"));
 }
 @Test void legacyUsageMigratesOnceAndPasswordsSurviveRestart()throws Exception{
  String email="legacy@example.com";
  accounts.register(email,"Resume12!","coach");accounts.verified(email);accounts.decide(email,"trial");
  db.update("UPDATE registered_account SET review_state='used',build_state='used',review_count=NULL,build_count=NULL WHERE email=?",email);
  var admin=mock(AdminService.class);when(admin.email()).thenReturn("owner@example.com");
  var migrated=new RegisteredAccounts(db,admin,mail,Clock.systemUTC(),mapper);
  assertTrue(migrated.matches(email,"Resume12!"));
  assertEquals(1,migrated.state(email).get("reviewsRemaining"));
  assertEquals(1,migrated.state(email).get("buildsRemaining"));
  assertThrows(ResponseStatusException.class,()->migrated.startTrial(email,"individual"));
  var otherDevice=new MockHttpSession();otherDevice.setAttribute("coachEmail",email);
  migrated.runTrial(otherDevice,"review","Senior",email,()->mapper.createObjectNode());
  var restarted=new RegisteredAccounts(db,admin,mail,Clock.systemUTC(),mapper);
  assertEquals(0,restarted.state(email).get("reviewsRemaining"));
  assertTrue(restarted.matches(email,"Resume12!"));
  assertThrows(ResponseStatusException.class,()->restarted.runTrial(otherDevice,"review","Senior",email,()->mapper.createObjectNode()));
 }
 @Test void runningTrialCannotSpendAnotherCredit()throws Exception{
  String email="parallel@example.com";accounts.startTrial(email,"coach");accounts.verified(email);
  var session=new MockHttpSession();session.setAttribute("coachEmail",email);
  accounts.runTrial(session,"review","Senior",email,()->{
   assertThrows(ResponseStatusException.class,()->accounts.runTrial(session,"review","Senior",email,()->mapper.createObjectNode()));
   return mapper.createObjectNode();
  });
  assertEquals(1,accounts.state(email).get("reviewsRemaining"));
 }
 @Test void subscriptionRequestDoesNotGrantAccess(){accounts.register("pending@example.com","Resume12!","coach");accounts.verified("pending@example.com");accounts.subscribe("pending@example.com");assertEquals(false,accounts.state("pending@example.com").get("allowed"));}
}
