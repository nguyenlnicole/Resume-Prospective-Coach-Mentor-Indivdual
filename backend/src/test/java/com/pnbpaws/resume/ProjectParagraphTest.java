package com.pnbpaws.resume;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProjectParagraphTest {
  @Test void allowsDetailedParagraphsAndSuppliesHeadingSeparately() {
    var mapper = new ObjectMapper();
    var paragraph = "Supported project details ".repeat(22).strip();
    var ai = new AiService(mapper, "test", "test") {
      @Override ObjectNode call(String prompt, JsonNode input, ObjectNode schema) {
        assertEquals("RP | GitHub", input.path("title").asText());
        assertTrue(prompt.contains("do not repeat the project title"));
        assertTrue(prompt.contains("no 45-word cap"));
        assertTrue(prompt.contains("one complete sentence of about 25-30 words"));
        assertTrue(prompt.contains("work experience is clearly limited"));
        assertEquals("One internship",input.path("experience").asText());
        return mapper.createObjectNode().put("text", paragraph);
      }
    };
    try { assertEquals(paragraph, ai.rewriteProject("Source facts", "RP | GitHub", "One internship")); }
    finally { ai.closeRewrites(); }
  }
}
