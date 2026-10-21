package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class SectionRewritesTest {
  final ObjectMapper mapper=new ObjectMapper();
  static final String SUMMARY="Software engineer with experience building reliable services and improving team workflows through practical development and testing. Contributes clear documentation and maintainable code while working with colleagues to solve problems, support users, and deliver useful improvements to existing systems.";
  @Test void startsIndependentSectionsTogetherAndReturnsResultsInOrder() throws Exception {
    try(var runner=new SectionRewrites()) {
      var entered=new CountDownLatch(3);var release=new CountDownLatch(1);
      var jobs=new ArrayList<SectionRewrites.Job>();
      for(String name:List.of("summary","experience","projects")) jobs.add(new SectionRewrites.Job(name,name,()->{
        entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("Timed out");}catch(InterruptedException e){throw new RuntimeException(e);}
        return mapper.createObjectNode().put("name",name);
      }));
      var future=CompletableFuture.supplyAsync(()->runner.run(jobs,new SectionRewrites.Cache()));
      try {assertTrue(entered.await(3,TimeUnit.SECONDS),"All independent sections should start before any finishes");}
      finally{release.countDown();}
      assertEquals(List.of("summary","experience","projects"),new ArrayList<>(future.get(5,TimeUnit.SECONDS).keySet()));
    }
  }
  @Test void failedCallsAreRetriedAndCachedResultsCannotBeMutated() {
    try(var runner=new SectionRewrites()) {
      var cache=new SectionRewrites.Cache();var calls=new AtomicInteger();
      var jobs=List.of(new SectionRewrites.Job("section","input",()->{
        if(calls.incrementAndGet()==1)throw new IllegalStateException("Temporary failure");
        return mapper.createObjectNode().put("text","Original");
      }));
      assertThrows(IllegalStateException.class,()->runner.run(jobs,cache));
      runner.run(jobs,cache).get("section").put("text","Changed");
      assertEquals("Original",runner.run(jobs,cache).get("section").path("text").asText());
      assertEquals(2,calls.get());
    }
  }
  @Test void cacheIsIsolatedBySessionAndAccountAndExpires() {
    var first=new MockHttpSession();var second=new MockHttpSession();
    assertNotSame(SectionRewrites.sessionCache(first),SectionRewrites.sessionCache(second));
    var old=SectionRewrites.sessionCache(first);first.setAttribute("coachEmail","other@example.com");
    assertNotSame(old,SectionRewrites.sessionCache(first));
    var clock=org.mockito.Mockito.mock(java.time.Clock.class);
    org.mockito.Mockito.when(clock.millis()).thenReturn(0L);
    var cache=new SectionRewrites.Cache(clock);cache.put("key",mapper.createObjectNode().put("ok",true));
    org.mockito.Mockito.when(clock.millis()).thenReturn(16*60*1000L);
    assertNull(cache.get("key"));
  }
  @Test void projectChangesReuseExperienceButJobAndWritingChangesInvalidateIt() {
    var calls=new java.util.concurrent.ConcurrentHashMap<String,AtomicInteger>();
    var ai=new AiService(mapper,"test","test") {
      @Override ObjectNode call(String instructions,JsonNode input,ObjectNode schema) {
        String part=schema.path("properties").path("resume").path("properties").has("summary")?"summary":schema.path("properties").path("resume").path("properties").has("experience")?"experience":schema.path("properties").path("resume").path("properties").has("projects")?"projects":"other";
        calls.computeIfAbsent(part,k->new AtomicInteger()).incrementAndGet();
        ObjectNode result=mapper.createObjectNode();ObjectNode resume=result.putObject("resume");
        schema.path("properties").path("resume").path("properties").fieldNames().forEachRemaining(field->{
          if(field.equals("summary"))resume.put(field,SUMMARY);
          else if(field.equals("contact"))resume.set(field,input.path("resume").path(field).deepCopy());
          else resume.put(field,input.path("resume").path(field).asText());
        });
        result.set("resume", ResumeCleanup.normalize(resume));
        result.putArray("approvalQuestions");return result;
      }
    };
    try {
      assertTrue(SummaryRevision.valid(SUMMARY));
      var input=mapper.createObjectNode();var resume=input.putObject("resume");resume.putObject("contact").put("name","Example");
      resume.put("experience","Employer - Engineer | 2020-Present\n- Built services.").put("projects","Project A\n- Built a tool.");
      input.put("targetRole","Engineer");input.putArray("preferences").add("Clear wording");
      var cache=new SectionRewrites.Cache();
      var built=ai.build(input,"Junior",cache);
      assertEquals(SUMMARY,built.path("resume").path("summary").asText());
      assertFalse(built.path("resume").path("experience").asText().isBlank());
      assertFalse(built.path("resume").path("projects").asText().isBlank());
      ai.build(input,"Junior",cache);
      assertTrue(calls.values().stream().allMatch(n->n.get()==1));
      resume.put("projects","Project A\n- Built a different tool.");ai.build(input,"Junior",cache);
      assertEquals(1,calls.get("experience").get());assertEquals(2,calls.get("projects").get());assertEquals(2,calls.get("summary").get());
      input.put("targetRole","Analyst");ai.build(input,"Junior",cache);assertEquals(2,calls.get("experience").get());
      input.withArray("preferences").add("Concise wording");ai.build(input,"Junior",cache);assertEquals(3,calls.get("experience").get());
    } finally {ai.closeRewrites();}
  }
}
