package com.pnbpaws.resume;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.*;
import org.springframework.web.server.ResponseStatusException;

class WorkflowDraftsTest {
  ObjectMapper mapper=new ObjectMapper();
  WorkflowDrafts service(String url){return new WorkflowDrafts(new JdbcTemplate(new DriverManagerDataSource(url,"sa","")),mapper,Clock.systemUTC());}
  @Test void progressAndUploadSurviveDatabaseReopenAndStayWithTheirOwner(@org.junit.jupiter.api.io.TempDir java.nio.file.Path folder)throws Exception{
    String url="jdbc:h2:file:"+folder.resolve("drafts").toAbsolutePath().toString().replace('\\','/');
    var drafts=service(url);var session=new MockHttpSession();
    session.setAttribute("reviewedLevel","Junior");session.setAttribute("reviewUserEmail","client@example.com");
    session.setAttribute("reviewResult",mapper.readTree("{\"resume\":{\"summary\":\"Trusted review\"}}"));
    session.setAttribute("coachToken","never-copy-this");
    String snapshot="{\"version\":1,\"page\":\"questions\",\"review\":{},\"answers\":{\"impact\":\"Cut waiting time\"},\"context\":{\"adminVersion\":\"forged\"}}";
    drafts.save("coach@example.com",snapshot,new MockMultipartFile("file","resume.pdf","application/pdf",new byte[]{1,2,3}),session);
    var reopened=service(url);assertEquals(true,reopened.info("coach@example.com").get("available"));
    assertEquals(false,reopened.info("other@example.com").get("available"));
    assertThrows(ResponseStatusException.class,()->reopened.resume("other@example.com",new MockHttpSession()));
    var restored=new MockHttpSession();restored.setAttribute("reviewedLevel","Senior");
    var result=reopened.resume("coach@example.com",restored);
    assertTrue(result.get("snapshot").toString().contains("Cut waiting time"));
    assertEquals("Junior",restored.getAttribute("reviewedLevel"));
    assertEquals("client@example.com",restored.getAttribute("reviewUserEmail"));
    assertNull(restored.getAttribute("coachToken"));assertNull(restored.getAttribute("adminVersion"));
    assertEquals("resume.pdf",((Map<?,?>)result.get("file")).get("name"));
    assertEquals("AQID",((Map<?,?>)result.get("file")).get("base64"));
  }
  @Test void laterSaveReplacesProgressAndInvalidPagesAreRejected()throws Exception{
    var drafts=service("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";DB_CLOSE_DELAY=-1");
    var session=new MockHttpSession();
    drafts.save("owner@example.com","{\"version\":1,\"page\":\"preferences\"}",null,session);
    drafts.save("owner@example.com","{\"version\":1,\"page\":\"edit\"}",null,session);
    assertTrue(drafts.resume("owner@example.com",session).get("snapshot").toString().contains("edit"));
    assertThrows(ResponseStatusException.class,()->drafts.save("owner@example.com","{\"version\":1,\"page\":\"admin\"}",null,session));
    assertThrows(ResponseStatusException.class,()->drafts.save("owner@example.com","broken json",null,session));
  }
  @Test void deniedAccountsCannotReadOrRestoreDrafts()throws Exception{
    var access=mock(CoachAccess.class);var drafts=mock(WorkflowDrafts.class);
    var session=new MockHttpSession();doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED)).when(access).requireTools(session);
    var controller=new WorkflowDraftController(drafts,access);
    assertThrows(ResponseStatusException.class,()->controller.info(session,new MockHttpServletResponse()));
    assertThrows(ResponseStatusException.class,()->controller.resume(session,new MockHttpServletResponse()));
    verifyNoInteractions(drafts);
  }
  @Test void recentReviewsRestoreTheirOwnEditsAndRejectAnotherOwnerOrStaleTab() throws Exception {
    var db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
    var history = new CoachReviewHistory(db, Clock.systemUTC());
    var drafts = new WorkflowDrafts(db, mapper, Clock.systemUTC());
    var session = new MockHttpSession();
    String first = history.saveReview("coach", "first", "First", "first.pdf", new byte[]{1}, new byte[]{2});
    String second = history.saveReview("coach", "second", "Second", "second.pdf", new byte[]{3}, new byte[]{4});
    session.setAttribute("coachHistoryId", first);
    session.setAttribute("reviewedLevel", "Educator");
    var snapshot = mapper.createObjectNode().put("version",1).put("page","questions");
    snapshot.putObject("review").put("historyId",first);
    snapshot.putObject("answers").put("learners","Grades 2–3");
    drafts.save("coach", snapshot.toString(), null, session);
    session.setAttribute("coachHistoryId", second);
    assertThrows(ResponseStatusException.class, () -> drafts.save("coach", snapshot.toString(), null, session));
    var other = snapshot.deepCopy();
    other.withObject("review").put("historyId",second);
    other.put("page","edit"); other.withObject("answers").put("learners","Adults");
    drafts.save("coach",other.toString(),null,session);
    assertTrue(drafts.resume("coach",new MockHttpSession()).get("snapshot").toString().contains("Adults"));
    var restored = new MockHttpSession();
    var result = drafts.resumeReview("coach", first, restored);
    assertTrue(result.get("snapshot").toString().contains("Grades 2–3"));
    assertEquals(first,restored.getAttribute("coachHistoryId"));
    assertEquals("first.pdf",((Map<?,?>)result.get("file")).get("name"));
    assertThrows(ResponseStatusException.class, () -> drafts.resumeReview("different-coach",first,new MockHttpSession()));
    assertTrue(history.list("coach").stream().allMatch(item -> Boolean.TRUE.equals(item.get("hasProgress"))));
  }
}
