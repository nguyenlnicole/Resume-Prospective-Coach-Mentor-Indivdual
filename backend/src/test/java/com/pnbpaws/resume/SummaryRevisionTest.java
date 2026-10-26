package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SummaryRevisionTest {
  private static final String FINISHED = "Mathematics student with experience creating technology communications and supporting training in instructional tools. Explains technical workflows clearly and helps students understand and use available university learning resources.";
  @Test void repairsAnOverLengthGeneratedSummaryAutomatically() {
    var calls = new AtomicInteger();
    assertTrue(SummaryRevision.valid(FINISHED));
    assertEquals(FINISHED, SummaryRevision.finish("word ".repeat(49), "Original.", draft -> { calls.incrementAndGet(); return FINISHED; }));
    assertEquals(1, calls.get());
  }
  @Test void keepsAnAlreadyValidSummaryWithoutAnotherAiCall() {
    assertEquals(FINISHED, SummaryRevision.finish(FINISHED, FINISHED, draft -> { fail("Unexpected rewrite"); return ""; }));
  }
  @Test void preservesAValidRewrittenSummary() {
    assertEquals(FINISHED, SummaryRevision.finish(FINISHED, "Original.", draft -> { fail("Unexpected rewrite"); return ""; }));
  }
  @Test void returnsUsableRepairWhenLengthTargetIsNotMet() {
    var calls = new AtomicInteger();
    assertEquals("Still too short.", SummaryRevision.finish("Too short.", "Original.", draft -> { calls.incrementAndGet(); return "Still too short."; }));
    assertEquals(3, calls.get());
  }
}
