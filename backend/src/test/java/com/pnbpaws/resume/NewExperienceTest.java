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
  @Test void acceptsShortNotesAndRequiresValidDates() {
    ObjectNode data = data();
    ((ObjectNode)data.path("newExperience")).put("description", "Taught students.");
    assertDoesNotThrow(() -> NewExperience.validate(data));
    ((ObjectNode)data.path("newExperience")).put("description", "");
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

  @Test void keepsTwoRolesAndTheirBulletsSeparateNewestFirst() {
    ObjectNode data = data();
    ObjectNode second = ((ObjectNode)data.path("newExperience")).putObject("secondRole");
    second.put("enabled", true).put("role", "Assistant").put("employer", "Library")
        .put("start", "2020-01").put("end", "2022-02").put("current", false)
        .put("description", "Helped visitors find books daily. ".repeat(10));
    assertDoesNotThrow(() -> NewExperience.validate(data));
    ObjectNode result = mapper.createObjectNode();
    result.putObject("resume").put("experience", "Existing role");
    result.putArray("newExperienceBullets").add("Taught students.");
    result.putArray("newExperienceSecondBullets").add("Helped library visitors.");
    NewExperience.append(data, result);
    String experience = result.path("resume").path("experience").asText();
    assertTrue(experience.indexOf("Teacher") < experience.indexOf("Assistant"));
    assertTrue(experience.indexOf("Taught students.") < experience.indexOf("Assistant"));
    assertTrue(experience.indexOf("Helped library visitors.") > experience.indexOf("Assistant"));
    assertTrue(experience.contains("January 2020 \u2013 February 2022"), experience);
    second.put("description", "");
    assertThrows(ResponseStatusException.class, () -> NewExperience.validate(data));
  }
}
