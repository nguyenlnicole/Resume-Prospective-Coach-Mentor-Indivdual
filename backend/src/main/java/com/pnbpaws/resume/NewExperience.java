package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

final class NewExperience {
  static boolean enabled(JsonNode data) { return data.path("newExperience").path("enabled").asBoolean(false); }

  static void validate(JsonNode data) {
    if (!enabled(data)) return;
    validateOne(data);
    JsonNode second = data.path("newExperience").path("secondRole");
    if (second.path("enabled").asBoolean(false)) {
      if (second.path("secondRole").path("enabled").asBoolean(false))
        throw new ResponseStatusException(BAD_REQUEST, "Add at most two new roles.");
      ObjectNode secondData = data.deepCopy();
      secondData.set("newExperience", second);
      validateOne(secondData);
    }
  }

  private static void validateOne(JsonNode data) {
    if (!enabled(data)) return;
    JsonNode value = data.path("newExperience");
    for (String key : new String[]{"role", "employer"}) {
      String text = value.path(key).asText().strip();
      if (text.isEmpty() || text.length() > 160 || text.contains("\n") || text.contains("|"))
        throw new ResponseStatusException(BAD_REQUEST, "Enter a valid role and employer or organization.");
    }
    String description = value.path("description").asText().strip();
    if (description.isEmpty() || description.split("\\s+").length < 50 || description.length() > 10000)
      throw new ResponseStatusException(BAD_REQUEST, "Describe your new experience in at least 50 words (up to 10,000 characters).");
    try {
      YearMonth start = YearMonth.parse(value.path("start").asText());
      if (start.isAfter(YearMonth.now())) throw new IllegalArgumentException();
      if (!value.path("current").asBoolean(false)) {
        YearMonth end = YearMonth.parse(value.path("end").asText());
        if (end.isBefore(start) || end.isAfter(YearMonth.now())) throw new IllegalArgumentException();
      }
    } catch (RuntimeException exception) {
      throw new ResponseStatusException(BAD_REQUEST, "Enter valid experience dates, with the end on or after the start and no future dates.");
    }
  }

  static ObjectNode append(JsonNode data, ObjectNode result) {
    if (!enabled(data)) return result;
    JsonNode first = data.path("newExperience");
    JsonNode second = first.path("secondRole");
    var roles = new java.util.ArrayList<JsonNode>();
    roles.add(first);
    if (second.path("enabled").asBoolean(false)) roles.add(second);
    JsonNode firstBullets = result.path("newExperienceBullets").deepCopy();
    JsonNode secondBullets = result.path("newExperienceSecondBullets").deepCopy();
    roles.sort(java.util.Comparator.comparing((JsonNode role) -> role.path("current").asBoolean(false) ? "9999" : role.path("end").asText()).thenComparing(role -> role.path("start").asText()));
    for (JsonNode role : roles) {
      ObjectNode roleData = data.deepCopy();
      roleData.set("newExperience", role);
      result.set("newExperienceBullets", role == first ? firstBullets : secondBullets);
      appendOne(roleData, result);
    }
    result.remove("newExperienceSecondBullets");
    return result;
  }

  private static ObjectNode appendOne(JsonNode data, ObjectNode result) {
    if (!enabled(data)) return result;
    JsonNode value = data.path("newExperience");
    JsonNode bullets = result.path("newExperienceBullets");
    if (!bullets.isArray() || bullets.isEmpty())
      throw new ResponseStatusException(BAD_GATEWAY, "The new experience bullets were incomplete. Please try building again; your details are saved in the form.");
    DateTimeFormatter format = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.US);
    String dates = YearMonth.parse(value.path("start").asText()).format(format) + " – "
        + (value.path("current").asBoolean(false) ? "Present" : YearMonth.parse(value.path("end").asText()).format(format));
    StringBuilder entry = new StringBuilder(value.path("employer").asText().strip()).append(" — ")
        .append(value.path("role").asText().strip()).append(" | ").append(dates);
    for (JsonNode bullet : bullets) {
      String text = bullet.asText().replaceAll("\\s+", " ").replaceFirst("^[•*\\-]\\s*", "").strip();
      if (text.isEmpty()) throw new ResponseStatusException(BAD_GATEWAY, "Please try building the new experience again.");
      entry.append("\n• ").append(text);
    }
    ObjectNode resume = (ObjectNode) result.path("resume");
    resume.put("experience", entry + (resume.path("experience").asText().isBlank() ? "" : "\n\n" + resume.path("experience").asText()));
    for (JsonNode change : result.path("protectedChanges"))
      if (change.path("section").asText().equals("experience"))
        ((ObjectNode) change).put("after", resume.path("experience").asText());
    result.remove("newExperienceBullets");
    return result;
  }
}
