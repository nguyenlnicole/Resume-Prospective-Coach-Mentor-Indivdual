package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

final class NewProjects {
  static void validate(JsonNode data) {
    JsonNode projects = data.path("newProjects");
    if (projects.isMissingNode()) return;
    if (!projects.isArray() || projects.size() > 5)
      throw new ResponseStatusException(BAD_REQUEST, "Add up to five new projects at a time.");
    for (JsonNode project : projects) {
      if (!project.path("confirmed").asBoolean(false))
        throw new ResponseStatusException(BAD_REQUEST, "Confirm each new project's details before building.");
      for (String key : new String[]{"name", "description", "contribution", "stack"}) {
        String text = project.path(key).asText().strip();
        if (text.isEmpty() || text.length() > 4000)
          throw new ResponseStatusException(BAD_REQUEST, "Complete the project name, description, your contribution, and technologies.");
      }
    }
  }
}
