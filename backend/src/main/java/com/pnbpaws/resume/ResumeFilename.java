package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.Normalizer;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;

final class ResumeFilename {
  private ResumeFilename() {}

  static String base(JsonNode resume) {
    return base(resume, YearMonth.now(ZoneId.of("America/Chicago")));
  }

  static String base(JsonNode resume, YearMonth month) {
    String name = resume == null ? "" : resume.path("contact").path("name").asText("").trim();
    String[] parts = name.split("\\s+");
    String first = parts.length > 0 ? safe(parts[0]) : "";
    String last = parts.length > 1 ? safe(parts[parts.length - 1]) : "";
    if (first.isEmpty()) first = "Resume";
    if (last.isEmpty()) last = "Owner";
    return first + "-" + last + "-PNBRP-" + String.format(Locale.ROOT, "%02d%02d", month.getMonthValue(), month.getYear() % 100);
  }

  private static String safe(String part) {
    return Normalizer.normalize(part, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
        .replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-|-$", "");
  }
}
