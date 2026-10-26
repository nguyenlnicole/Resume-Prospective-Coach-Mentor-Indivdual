package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GeneratedWritingTest {
  @Test void fixesShortAndLongExperienceBullets() {
    String good = "• " + "fact ".repeat(20);
    assertEquals(good.strip(), GeneratedWriting.finish("• Short claim.", "experience", value -> good));
    assertEquals(good.strip(), GeneratedWriting.finish("• " + "fact ".repeat(31), "experience", value -> good));
  }
  @Test void keepsUsableTextWhenRewriteUnavailable() {
    assertEquals("• Short claim.", GeneratedWriting.finish("• Short claim.", "experience", value -> { throw new IllegalStateException(); }));
  }
  @Test void preservesExactBoundaryAndSkipsCalls() {
    for (int words : new int[]{20, 30}) assertTrue(GeneratedWriting.valid("• " + "fact ".repeat(words), "experience"));
    assertTrue(GeneratedWriting.valid("fact ".repeat(30), "description"));
    assertFalse(GeneratedWriting.valid("fact ".repeat(31), "description"));
  }
}
