package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class JobMetadataTest {
  @Test void readsLongWorkdayDescriptionWithReorderedAttributes() {
    String description = "Build software &amp; test features. ".repeat(30);
    String html = "<meta content='Software Engineer 1' property='og:title'>"
        + "<meta content=\"" + description + "\" name=\"description\" property=\"og:description\">";
    String result = JobDescriptionService.jobMetadata(html);
    assertTrue(result.startsWith("Software Engineer 1\n\n"));
    assertTrue(result.contains("software & test"));
    assertFalse(result.contains("&amp;"));
  }
  @Test void ignoresShortSocialSummaries() {
    assertEquals("", JobDescriptionService.jobMetadata("<meta property='og:description' content='Apply here'>"));
    assertEquals("", JobDescriptionService.jobMetadata("<p>Normal job page</p>"));
  }
}
