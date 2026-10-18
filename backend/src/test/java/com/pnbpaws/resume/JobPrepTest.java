package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JobPrepTest {
  @Test
  void outsideFactsNeedUrlsFromTheActualSearch() throws Exception {
    var mapper = new ObjectMapper();
    var sheet = Map.of(
        "study", List.of("one", "two", "three", "four", "five", "six"),
        "prepare", List.of("practice", "bring examples"),
        "facts", List.of(
            Map.of("fact", "Verified fact", "sourceUrl", "https://example.org/source", "sourceTitle", "Source"),
            Map.of("fact", "Made up fact", "sourceUrl", "https://fake.example/fact", "sourceTitle", "Fake")));
    var response = mapper.readTree("""
        {"output":[
          {"type":"web_search_call","action":{"sources":[{"url":"https://example.org/source"}]}},
          {"type":"message","content":[{"type":"output_text","text":%s}]}
        ]}
        """.formatted(mapper.writeValueAsString(mapper.writeValueAsString(sheet))));
    var result = AiService.verifiedJobPrep(mapper, response);
    assertEquals(6, result.path("study").size());
    assertEquals(2, result.path("prepare").size());
    assertEquals(1, result.path("facts").size());
    assertEquals("Verified fact", result.path("facts").get(0).path("fact").asText());
  }
}
