package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class NewExperienceTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private ObjectNode data() {
    ObjectNode data = mapper.createObjectNode();
    data.putObject("newExperience").put("enabled", true).put("role", "Teacher")
        .put("employer", "School").put("start", "2024-08").put("current", true)
        .put("description", "Taught students and prepared lessons. ".repeat(10));
    return data;
  }
  @Test void requiresFiftyWordsAndValidDates() {
    ObjectNode data = data();
    assertDoesNotThrow(() -> NewExperience.validate(data));
    ((ObjectNode)data.path("newExperience")).put("description", "Too short");
    assertThrows(ResponseStatusException.class, () -> NewExperience.validate(data));
    ObjectNode reversed = data();
    ((ObjectNode)reversed.path("newExperience")).put("current", false).put("end", "2023-07");

    assertThrows(ResponseStatusException.class, () -> NewExperience.validate(reversed));
  }
  @Test void addsExactRoleAndDatesWithoutChangingExistingExperience() {
    ObjectNode result = mapper.createObjectNode();
    result.putObject("resume").put("experience", "Existing role");
    result.putArray("newExperienceBullets").add("Prepared lessons and taught students.");
    NewExperience.append(data(), result);
    assertEquals("School — Teacher | August 2024 – Present\n• Prepared lessons and taught students.\n\nExisting role", result.path("resume").path("experience").asText());
  }
  @Test void missingBulletsCannotSilentlyDropTheNewRole() {
    ObjectNode result = mapper.createObjectNode();
    result.putObject("resume").put("experience", "Existing role");
    assertThrows(ResponseStatusException.class, () -> NewExperience.append(data(), result));
  }
}
