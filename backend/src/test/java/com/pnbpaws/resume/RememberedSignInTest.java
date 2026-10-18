package com.pnbpaws.resume;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.*;
class RememberedSignInTest {
 JdbcTemplate db; CoachAccess access; RegisteredAccounts accounts; RememberedSignIn remembered;
 String email="person@example.com"; Clock clock=Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"),ZoneOffset.UTC);
 @BeforeEach void setup(){
  db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
  var admin=mock(AdminService.class);when(admin.email()).thenReturn("owner@example.com");var mail=mock(MailService.class);
  access=new CoachAccess(db,mail,admin,clock);accounts=new RegisteredAccounts(db,admin,mail,clock,new ObjectMapper());access.accounts=accounts;remembered=new RememberedSignIn(db,access,clock);
 }
 MockHttpServletRequest request(){var r=new MockHttpServletRequest();r.setRequestURI("/api/access");r.setSecure(true);return r;}
 MockHttpServletRequest signedIn(boolean keep){var r=request();r.getSession().setAttribute("coachEmail",email);r.getSession().setAttribute("coachToken",access.issueToken(email));var response=new MockHttpServletResponse();remembered.choose(r,response,keep);String cookie=response.getHeader("Set-Cookie");assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("SameSite=Strict"));return r;}
 MockHttpServletRequest reopen(MockHttpServletRequest r){var next=request();next.setCookies(new Cookie(RememberedSignIn.COOKIE,r.getSession().getAttribute("coachToken").toString()));return next;}
 void individual(){accounts.register(email,"Resume12!","individual");accounts.verified(email);accounts.decide(email,"subscribed");}
 @Test void coachSurvivesRestartButNotExpiration(){access.setStatus(email,"approved");var login=signedIn(true);var next=reopen(login);remembered.check(next,new MockHttpServletResponse());assertEquals(true,access.state(next.getSession()).get("allowed"));db.update("UPDATE coach SET expires_at=? WHERE email=?",clock.millis(),email);remembered.check(next,new MockHttpServletResponse());assertNull(next.getSession(false));var retry=reopen(login);remembered.check(retry,new MockHttpServletResponse());assertNull(retry.getSession(false));}
 @Test void uncheckedCannotResume(){individual();var next=reopen(signedIn(false));remembered.check(next,new MockHttpServletResponse());assertNull(next.getSession(false));}
 @Test void revocationAndApprovalDoNotResurrectOldToken(){individual();var next=reopen(signedIn(true));accounts.decide(email,"revoked",null);accounts.decide(email,"subscribed",null);remembered.check(next,new MockHttpServletResponse());assertNull(next.getSession(false));}
 @Test void individualSubscriptionEndsOnServerClock(){individual();var next=reopen(signedIn(true));db.update("UPDATE registered_account SET subscription_ends=? WHERE email=?",clock.millis(),email);remembered.check(next,new MockHttpServletResponse());assertNull(next.getSession(false));}
 @Test void deletedIndividualCannotResume(){individual();var next=reopen(signedIn(true));db.update("UPDATE registered_account SET approval='deleted',verified=FALSE WHERE email=?",email);remembered.check(next,new MockHttpServletResponse());assertNull(next.getSession(false));}
 @Test void signOutInvalidatesCookie(){individual();var login=signedIn(true);var next=reopen(login);access.signOut(login.getSession());remembered.check(next,new MockHttpServletResponse());assertNull(next.getSession(false));}
 @Test void invalidCookieDoesNotCreateSession(){var next=request();next.setCookies(new Cookie(RememberedSignIn.COOKIE,"fake"));remembered.check(next,new MockHttpServletResponse());assertNull(next.getSession(false));}
}
