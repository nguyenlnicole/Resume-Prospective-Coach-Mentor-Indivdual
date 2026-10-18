package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SummaryRevisionTest {
  private static final String FINISHED = "Mathematics student with experience creating student-facing technology communications and supporting training in instructional tools. Creates clear written content, explains technical workflows, and draws on university projects to help students understand and use available learning resources effectively.";
  @Test void repairsAnOverLengthGeneratedSummaryAutomatically() {
    var calls = new AtomicInteger();
    assertTrue(SummaryRevision.valid(FINISHED));
    assertEquals(FINISHED, SummaryRevision.finish("word ".repeat(46), "Original.", draft -> { calls.incrementAndGet(); return FINISHED; }));
    assertEquals(1, calls.get());
  }
  @Test void rewritesAnUnchangedOriginalRatherThanReturningIt() {
    assertEquals(FINISHED + " Today.", SummaryRevision.finish(FINISHED, FINISHED, draft -> FINISHED + " Today."));
  }
  @Test void preservesAValidRewrittenSummary() {
    assertEquals(FINISHED, SummaryRevision.finish(FINISHED, "Original.", draft -> { fail("Unexpected rewrite"); return ""; }));
  }
  @Test void returnsUsableRepairWhenLengthTargetIsNotMet() {
    var calls = new AtomicInteger();
    assertEquals("Still too short.", SummaryRevision.finish("Too short.", "Original.", draft -> { calls.incrementAndGet(); return "Still too short."; }));
    assertEquals(2, calls.get());
  }
}
