package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class NewProjectsTest {
  @Test void acceptsProjectDetailsWithoutConfirmation() {
    var data = new ObjectMapper().createObjectNode();
    var project = data.putArray("newProjects").addObject().put("name", "Planner")
        .put("description", "Plans appointments").put("contribution", "Built calendar views").put("stack", "React");
    assertDoesNotThrow(() -> NewProjects.validate(data));
    project.put("confirmed", true);
    assertDoesNotThrow(() -> NewProjects.validate(data));
    project.put("contribution", "").put("stack", "");
    assertDoesNotThrow(() -> NewProjects.validate(data));
    project.put("description", "");
    assertThrows(ResponseStatusException.class, () -> NewProjects.validate(data));
  }
}
