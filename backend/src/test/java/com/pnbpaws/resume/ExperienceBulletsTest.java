package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceBulletsTest {
  final ObjectMapper mapper = new ObjectMapper();
  ObjectNode role() {
    return mapper.createObjectNode().put("role", "Engineer").put("employer", "Example")
      .put("start", "2020-01").put("current", true)
      .put("description", "Build software with artificial intelligence for community outreach and marketing programs.");
  }
  @Test void generatesThreeBulletsFromOnlyRoleFacts() {
    var ai = new AiService(mapper,"test","test") {
      @Override ObjectNode call(String instructions, JsonNode input, ObjectNode schema) {
        assertEquals(3,schema.at("/properties/bullets/minItems").asInt());
        assertEquals(3,schema.at("/properties/bullets/maxItems").asInt());
        assertFalse(input.path("newExperience").has("previewBullets"));
        assertTrue(instructions.contains("Do not invent"));
        assertTrue(instructions.contains("20-30 words"));
        assertTrue(instructions.contains("distinct focus"));
        var result=mapper.createObjectNode();result.putArray("bullets").add("Build software "+"using supported source facts ".repeat(5)).add("Integrate artificial intelligence "+"using supported source facts ".repeat(5)).add("Support community outreach and marketing programs "+"using supported source facts ".repeat(5));return result;
      }
    };
    try { var role=role();role.putArray("previewBullets").add("Unsupported claim");assertEquals(3,ai.experienceBullets(role).path("bullets").size()); }
    finally { ai.closeRewrites(); }
  }
  @Test void expandsShortBulletsOnceFromOriginalFacts() {
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var ai = new AiService(mapper,"test","test") {
      @Override ObjectNode call(String instructions, JsonNode input, ObjectNode schema) {
        var result=mapper.createObjectNode();var bullets=result.putArray("bullets");
        if(calls.incrementAndGet()==1) bullets.add("Build software.").add("Integrate artificial intelligence.").add("Support outreach.");
        else {
          assertTrue(instructions.contains("at least 18 words"));
          assertEquals(role().path("description"),input.at("/sourceFacts/newExperience/description"));
          for(String action:java.util.List.of("Build","Integrate","Support")) bullets.add(action+" "+"supported source detail ".repeat(7));
        }
        return result;
      }
    };
    try {var result=ai.experienceBullets(role());assertEquals(2,calls.get());for(var bullet:result.path("bullets"))assertTrue(bullet.asText().strip().split("\\s+").length>=18);}
    finally {ai.closeRewrites();}
  }
  @Test void rejectsIncompleteGenerationAndInsufficientNotes() {
    var ai = new AiService(mapper,"test","test") {
      @Override ObjectNode call(String instructions, JsonNode input, ObjectNode schema) {
        var result=mapper.createObjectNode();result.putArray("bullets").add("One bullet.");return result;
      }
    };
    try {
      assertThrows(ResponseStatusException.class,()->ai.experienceBullets(role()));
      assertThrows(ResponseStatusException.class,()->ai.experienceBullets(role().put("description","Too short.")));
    } finally { ai.closeRewrites(); }
  }
}
