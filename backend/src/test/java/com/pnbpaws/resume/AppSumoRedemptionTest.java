package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:appsumo-tests;DB_CLOSE_DELAY=-1", "app.admin-email=owner@example.com", "app.admin-password-hash=", "app.openai-key=", "app.openai-model=", "app.mail-enabled=false"})
@AutoConfigureMockMvc
@Import(AccountFlowIntegrationTest.MailConfig.class)
class AppSumoRedemptionTest {
  @Autowired AppSumoCodes codes;
  @Autowired RegisteredAccounts accounts;
  @Autowired CoachAccess access;
  @Autowired AdminService admin;
  @Autowired JdbcTemplate db;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  record Batch(String id,String code,String csv){}
  Batch batch(boolean header){
    String prefix="AS-"+UUID.randomUUID();
    String csv=(header?"code\n":"")+java.util.stream.IntStream.range(0,1000).mapToObj(i->prefix+"-"+i).collect(java.util.stream.Collectors.joining("\n"));
    return new Batch(codes.importCodes(csv).get("id").toString(),prefix+"-0",csv);
  }
  MockHttpSession buyer(String email){
    accounts.register(email,"Resume12!","coach");accounts.verified(email);
    var session=new MockHttpSession();session.setAttribute("coachEmail",email);session.setAttribute("coachToken",access.issueToken(email));return session;
  }
  org.springframework.test.web.servlet.ResultActions send(MockHttpSession session,String path,Object input)throws Exception{
    var result=mvc.perform(get("/api/csrf").session(session)).andReturn();
    String csrf=mapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    return mvc.perform(post(path).session(session).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(input)));
  }
  @Test void verifiedBuyerGetsThirtyDaysAndUnlimitedReviewBuildWithoutPasswordChange()throws Exception{
    var b=batch(false);codes.configure(b.id(),true);var session=buyer("paid@example.com");
    String hash=db.queryForObject("SELECT password_hash FROM registered_account WHERE email='paid@example.com'",String.class);
    send(session,"/api/appsumo/redeem",Map.of("code",b.code())).andExpect(status().isOk()).andExpect(jsonPath("$.plan").value("appsumo")).andExpect(jsonPath("$.allowed").value(true));
    long expiry=((Number)codes.entitlement("paid@example.com").get("expiresAt")).longValue();
    assertTrue(Math.abs(expiry-System.currentTimeMillis()-Duration.ofDays(30).toMillis())<5000);
    for(int i=0;i<3;i++) for(String operation:List.of("review","build"))accounts.runTrial(session,operation,"Senior","",()->mapper.createObjectNode().put("ok",true));
    assertEquals(hash,db.queryForObject("SELECT password_hash FROM registered_account WHERE email='paid@example.com'",String.class));
    assertTrue(accounts.matches("paid@example.com","Resume12!"));

    mvc.perform(get("/api/access").session(session).header("Host","resumep.pnbpaws.com")).andExpect(jsonPath("$.plan").value("appsumo"));
    codes.redeem("paid@example.com",b.code());assertEquals(expiry,codes.entitlement("paid@example.com").get("expiresAt"));
    codes.configure(b.id(),false);assertEquals(true,accounts.state("paid@example.com").get("allowed"));
  }
  @Test void inactiveInvalidAndReusedCodesAreRejected()throws Exception{
    var b=batch(true);var first=buyer("first@example.com");
    send(first,"/api/appsumo/redeem",Map.of("code",b.code())).andExpect(status().isConflict());
    codes.configure(b.id(),true);
    send(first,"/api/appsumo/redeem",Map.of("code","made-up-invalid-code")).andExpect(status().isBadRequest());
    codes.redeem("first@example.com",b.code());
    send(buyer("second@example.com"),"/api/appsumo/redeem",Map.of("code",b.code())).andExpect(status().isBadRequest());
    assertThrows(Exception.class,()->codes.redeem("first@example.com",b.code().substring(0,b.code().length()-1)+"1"));
    assertEquals(1000,db.queryForObject("SELECT COUNT(*) FROM appsumo_code WHERE batch_id=?",Integer.class,b.id()));
  }
  @Test void guestUnverifiedAndNonAdminCannotGrantAccess()throws Exception{
    var b=batch(false);codes.configure(b.id(),true);
    send(new MockHttpSession(),"/api/appsumo/redeem",Map.of("code",b.code())).andExpect(status().isUnauthorized());
    var session=buyer("unverified@example.com");db.update("UPDATE registered_account SET verified=FALSE WHERE email='unverified@example.com'");
    send(session,"/api/appsumo/redeem",Map.of("code",b.code())).andExpect(status().isUnauthorized());
    send(session,"/api/admin/appsumo/activate",Map.of("id",b.id(),"enabled",true)).andExpect(status().isForbidden());
    send(session,"/api/admin/appsumo/import",Map.of("csv",b.csv())).andExpect(status().isForbidden());
  }
  @Test void expirationAndRefundRevocationBlockTools()throws Exception{
    var b=batch(false);codes.configure(b.id(),true);var session=buyer("expired@example.com");codes.redeem("expired@example.com",b.code());
    codes.revoke("expired@example.com",true);assertEquals(false,accounts.state("expired@example.com").get("allowed"));
    send(session,"/api/job-prep",Map.of()).andExpect(status().isForbidden());
    codes.revoke("expired@example.com",false);assertEquals(true,accounts.state("expired@example.com").get("allowed"));
    db.update("UPDATE appsumo_code SET expires_at=? WHERE redeemed_by=?",System.currentTimeMillis()-1,"expired@example.com");
    assertEquals("expired",accounts.state("expired@example.com").get("state"));
    assertThrows(Exception.class,()->accounts.runTrial(session,"review","Senior","",()->mapper.createObjectNode()));
  }
  @Test void badImportRollsBackAndPlaintextCodesAreNotStored(){
    var b=batch(true);int count=db.queryForObject("SELECT COUNT(*) FROM appsumo_batch",Integer.class);
    assertThrows(Exception.class,()->codes.importCodes(b.csv()));
    assertEquals(count,db.queryForObject("SELECT COUNT(*) FROM appsumo_batch",Integer.class));
    assertThrows(Exception.class,()->codes.importCodes("a\nb"));
    assertThrows(Exception.class,()->codes.importCodes(("duplicate-code-123\n").repeat(1000)));
    assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM appsumo_code WHERE code_hash=?",Integer.class,b.code()));
  }
  @Test void concurrentBuyersCannotClaimSameCodeTwice()throws Exception{
    var b=batch(false);codes.configure(b.id(),true);
    try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
      var gate=new java.util.concurrent.CountDownLatch(1);
      var jobs=List.of("race1@example.com","race2@example.com").stream().map(email->executor.submit(()->{gate.await();try{codes.redeem(email,b.code());return 1;}catch(Exception e){return 0;}})).toList();
      gate.countDown();assertEquals(1,jobs.get(0).get()+jobs.get(1).get());
    }
  }
}
