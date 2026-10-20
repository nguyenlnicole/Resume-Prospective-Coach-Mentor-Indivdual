package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:account-flow;DB_CLOSE_DELAY=-1", "app.admin-email=owner@example.com", "app.admin-password-hash=", "app.openai-key=", "app.openai-model=", "app.mail-enabled=false"})
@AutoConfigureMockMvc
@Import(AccountFlowIntegrationTest.MailConfig.class)
class AccountFlowIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 @Autowired TestMail mail;
 @Autowired RegisteredAccounts accounts;
 @Autowired AdminService admin;
 @Autowired CoachAccess access;
 static final java.util.concurrent.atomic.AtomicInteger clientAddress = new java.util.concurrent.atomic.AtomicInteger();
 record Client(MockHttpSession session,String csrf,String address){}
 Client client()throws Exception{
  var response=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
  return new Client((MockHttpSession)response.getRequest().getSession(),mapper.readTree(response.getResponse().getContentAsString()).path("token").asText(),"192.0.2."+clientAddress.incrementAndGet());
 }
 org.springframework.test.web.servlet.ResultActions send(Client client,String path,Map<String,String> data)throws Exception{
  return mvc.perform(post("/api"+path).with(request->{request.setRemoteAddr(client.address());return request;}).session(client.session()).header("X-CSRF-TOKEN",client.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(data)));
 }
 @Test void verifiedTrialWorksAcrossEditionsButCannotBeClaimedTwice()throws Exception{
  var c=client();String email="trial-flow@example.com";
  send(c,"/accounts/trial",Map.of("email","TRIAL-FLOW@example.com","edition","coach")).andExpect(status().isOk());
  assertNotNull(mail.codes.get(email));
  send(c,"/accounts/trial",Map.of("email",email,"edition","coach")).andExpect(status().isOk());
  assertEquals(false,accounts.state(email).get("allowed"));
  mvc.perform(get("/api/access").session(c.session())).andExpect(jsonPath("$.allowed").value(false));
  send(c,"/coach/verify",Map.of("email",email,"code","000000","edition","coach")).andExpect(status().isUnauthorized());
  String code=mail.codes.get(email);
  send(c,"/coach/verify",Map.of("email",email,"code"," "+code+" ","edition","beta")).andExpect(status().isOk()).andExpect(jsonPath("$.allowed").value(true)).andExpect(jsonPath("$.plan").value("trial"));
  send(c,"/coach/verify",Map.of("email",email,"code",code,"edition","coach")).andExpect(status().isUnauthorized());
  send(c,"/accounts/trial",Map.of("email",email,"edition","individual")).andExpect(status().isOk());
  mvc.perform(get("/api/admin/accounts").session(c.session())).andExpect(status().isForbidden());
  accounts.runTrial(c.session(),"review","Senior",email,()->mapper.createObjectNode().put("saved",true));
  send(c,"/accounts/restore",Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.review.saved").value(true));
  send(c,"/accounts/subscribe",Map.of()).andExpect(status().isOk());
  assertEquals("trial",accounts.state(email).get("plan"));
 }
 @Test void returningTrialGetsFreshCodeWithoutRefillingCredits()throws Exception{
  String email="second-trial@example.com";
  var first=client();
  send(first,"/accounts/trial",Map.of("email",email,"edition","individual")).andExpect(status().isOk());
  send(first,"/coach/verify",Map.of("email",email,"code",mail.codes.get(email),"edition","beta")).andExpect(status().isOk());
  accounts.runTrial(first.session(),"review","Senior",email,()->mapper.createObjectNode());
  var second=client();
  send(second,"/accounts/trial",Map.of("email",email,"edition","coach")).andExpect(status().isOk());
  send(second,"/coach/verify",Map.of("email",email,"code",mail.codes.get(email),"edition","coach")).andExpect(status().isOk()).andExpect(jsonPath("$.reviewsRemaining").value(1));
  accounts.runTrial(second.session(),"review","Senior",email,()->mapper.createObjectNode());
  assertEquals(0,accounts.state(email).get("reviewsRemaining"));
  send(second,"/accounts/trial",Map.of("email",email,"edition","coach")).andExpect(status().isOk());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.runTrial(second.session(),"review","Senior",email,()->mapper.createObjectNode()));
 }
 @Test void signupNeedsCodeAndOwnerApprovalThenAllowsPasswordLogin()throws Exception{
  var c=client();String email="signup-flow@example.com";
  send(c,"/accounts/signup",Map.of("email",email,"password","weak","edition","individual")).andExpect(status().isBadRequest());
  send(c,"/accounts/signup",Map.of("email",email,"password","Resume12!","edition","individual")).andExpect(status().isOk());
  send(c,"/coach/verify",Map.of("email",email,"code",mail.codes.get(email),"edition","beta")).andExpect(status().isOk()).andExpect(jsonPath("$.allowed").value(false));
  send(c,"/admin/accounts",Map.of("email",email,"decision","subscribed")).andExpect(status().isForbidden());
  var owner=client();owner.session().setAttribute("adminVersion",admin.version());
  send(owner,"/admin/accounts",Map.of("email",email,"decision","subscribed")).andExpect(status().isOk());
  var login=client();
  send(login,"/accounts/login",Map.of("email",email,"password","Resume12!")).andExpect(status().isOk()).andExpect(jsonPath("$.allowed").value(true)).andExpect(jsonPath("$.plan").value("subscribed"));
 }
 @Test void ownerCanDeleteAndResetEmailWithoutBlockingIt()throws Exception{
  var trial=client();String email="reset-flow@example.com";
  send(trial,"/accounts/trial",Map.of("email",email,"edition","coach")).andExpect(status().isOk());
  send(trial,"/coach/verify",Map.of("email",email,"code",mail.codes.get(email),"edition","coach")).andExpect(status().isOk());
  var data=Map.of("email",email,"confirmation",email,"resetTrial",true);
  mvc.perform(post("/api/admin/accounts/delete").session(trial.session()).header("X-CSRF-TOKEN",trial.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(data))).andExpect(status().isForbidden());
  var owner=client();owner.session().setAttribute("adminVersion",admin.version());
  mvc.perform(post("/api/admin/accounts/delete").session(owner.session()).header("X-CSRF-TOKEN",owner.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("email",email,"confirmation","wrong","resetTrial",true)))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/admin/accounts/delete").session(owner.session()).header("X-CSRF-TOKEN",owner.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(data))).andExpect(status().isOk());
  mvc.perform(get("/api/access").session(trial.session())).andExpect(jsonPath("$.allowed").value(false));
  assertFalse(accounts.exists(email));
  trial=client();
  send(trial,"/accounts/trial",Map.of("email",email,"edition","individual")).andExpect(status().isOk());
  mvc.perform(post("/api/admin/accounts/delete").session(owner.session()).header("X-CSRF-TOKEN",owner.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("email",email,"confirmation",email,"resetTrial",false)))).andExpect(status().isOk());
  assertFalse(accounts.exists(email));
  send(trial,"/accounts/trial",Map.of("email",email,"edition","coach")).andExpect(status().isOk());
  assertEquals(2,accounts.state(email).get("reviewsRemaining"));
  assertFalse(accounts.deleted(email));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.decide(email,"subscribed"));
 }
 @Test void passwordResetIsSingleUseAndPreservesApproval()throws Exception{
  String email="password-flow@example.com";accounts.register(email,"Resume12!","coach");accounts.verified(email);
  var c=client();
  send(c,"/accounts/forgot-password",Map.of("email",email)).andExpect(status().isOk());
  String body=mail.bodies.get(email);
  var match=java.util.regex.Pattern.compile("account-reset=([A-Za-z0-9_-]{43})").matcher(body);assertTrue(match.find());String token=match.group(1);
  send(c,"/accounts/reset-password",Map.of("token",token,"password","Updated34!")).andExpect(status().isOk());
  assertTrue(accounts.matches(email,"Updated34!"));assertFalse(accounts.matches(email,"Resume12!"));
  assertEquals(false,accounts.state(email).get("allowed"));
  send(c,"/accounts/reset-password",Map.of("token",token,"password","Again56!")).andExpect(status().isBadRequest());
  send(c,"/accounts/forgot-password",Map.of("email","unknown@example.com")).andExpect(status().isOk());
  assertNull(mail.bodies.get("unknown@example.com"));
 }
 @Test void ownerCanSetCoachPasswordWithoutGainingAdminSession()throws Exception{
  var c=client();String email="owner@example.com";
  send(c,"/accounts/forgot-password",Map.of("email",email)).andExpect(status().isOk());
  assertFalse(accounts.exists(email));
  var match=java.util.regex.Pattern.compile("account-reset=([A-Za-z0-9_-]{43})").matcher(mail.bodies.get(email));
  assertTrue(match.find());String token=match.group(1);
  send(c,"/accounts/reset-password",Map.of("token",token,"password","Owner12!")).andExpect(status().isOk());
  var result=send(c,"/accounts/login",Map.of("email",email,"password","Owner12!")).andExpect(status().isOk())
    .andExpect(jsonPath("$.role").value("coach")).andExpect(jsonPath("$.allowed").value(true)).andReturn();
  var coachSession=(MockHttpSession)result.getRequest().getSession();
  mvc.perform(get("/api/admin/accounts").session(coachSession)).andExpect(status().isForbidden());
  var codeClient=client();
  send(codeClient,"/coach/code",Map.of("email",email,"edition","coach")).andExpect(status().isOk());
  send(codeClient,"/coach/verify",Map.of("email",email,"code",mail.codes.get(email),"edition","coach")).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("coach"));
  var owner=client();
  send(owner,"/admin/code",Map.of("email",email)).andExpect(status().isOk());
  send(owner,"/admin/verify",Map.of("email",email,"code",mail.codes.get(email))).andExpect(status().isOk());
  mvc.perform(get("/api/access").session(owner.session())).andExpect(jsonPath("$.role").value("admin"));
 }
 @Test void coverLetterPdfRequiresSignInAndProducesPreviewPages()throws Exception{
  var c=client();
  send(c,"/cover-letter/document",Map.of("text","Dear Hiring Team,\nI build reliable applications.")).andExpect(status().isUnauthorized());
  send(c,"/job-prep/document",Map.of("text","JOB PREP SHEET\nReview the supplied job posting.")).andExpect(status().isUnauthorized());
  String email="cover-document@example.com";
  accounts.startTrial(email,"coach");accounts.verified(email);
  c.session().setAttribute("coachEmail",email);c.session().setAttribute("coachToken",access.issueToken(email));
  send(c,"/cover-letter/document",Map.of("text","Dear Hiring Team,\nI build reliable applications.")).andExpect(status().isOk()).andExpect(jsonPath("$.pdf").isString()).andExpect(jsonPath("$.pages[0]").isString());
  send(c,"/job-prep/document",Map.of("text","JOB PREP SHEET\nReview the supplied job posting.")).andExpect(status().isOk()).andExpect(jsonPath("$.pdf").isString()).andExpect(jsonPath("$.pages[0]").isString());
 }
 @TestConfiguration static class MailConfig { @Bean @Primary TestMail testMail(){return new TestMail();} }
 static class TestMail extends MailService {
  Map<String,String> codes=new java.util.concurrent.ConcurrentHashMap<>();
  Map<String,String> bodies=new java.util.concurrent.ConcurrentHashMap<>();
  TestMail(){super(null,true,"owner@example.com");}
  @Override void text(String to,String subject,String body){
   bodies.put(to,body);
   if(subject.contains("sign-in code")){
    var match=java.util.regex.Pattern.compile("[0-9]{6}").matcher(body);
    if(match.find())codes.put(to,match.group());
   }
  }
 }
}
