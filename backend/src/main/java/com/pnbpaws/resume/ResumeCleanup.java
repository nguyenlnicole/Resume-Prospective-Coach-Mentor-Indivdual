package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.regex.*;

class ResumeCleanup {

  private static final String EDUCATION_DATE = "(?:(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\.?\\s+)?(?:19|20)\\d{2}";
  private static final Pattern EDUCATION_TAIL = Pattern.compile("(?i)(?:\\s*\\|\\s*|\\s+|,\\s*)(?:(Graduated|Expected)\\s+)?(" + EDUCATION_DATE + ")(?:\\s*[–—-]\\s*(" + EDUCATION_DATE + "|Present))?$");
  private static String educationDateOnce(String heading) {
    String details = heading, suffix = null;
    Matcher match = EDUCATION_TAIL.matcher(details);
    while (match.find()) {
      if (suffix == null) {
        String end = match.group(3) == null ? match.group(2) : match.group(3);
        if (end.equalsIgnoreCase("Present")) return heading;
        suffix = ("Expected".equalsIgnoreCase(match.group(1)) ? "Expected " : "") + end;
      }
      details = details.substring(0, match.start()).strip();
      match = EDUCATION_TAIL.matcher(details);
    }
    return suffix == null ? heading.replaceFirst("(?i)\\s*\\|\\s*Graduated$", "") : details + " | " + suffix;
  }

  static String compactEducation(String source) {
    List<String> entries = new ArrayList<>();
    Pattern description = Pattern.compile("(?i)^(?:[•*\\-]\\s|(?:relevant\\s+)?coursework\\s*:|(?:completed|attended|focused|studied|learned|developed|participated|accelerated\\s+.+\\s+program\\s+focused)\\b)");
    Pattern degree = Pattern.compile("(?i)^(?:bachelor|master|doctor|associate|b\\.?s\\.?\\b|b\\.?a\\.?\\b|m\\.?s\\.?\\b|m\\.?a\\.?\\b|mba\\b|ph\\.?d\\.?\\b|diploma\\b|certificate\\b|degree\\s*:)");
    for (String raw : source.split("\\R")) {
      String line = raw.strip();
      if (line.isBlank() || line.matches("(?i)education:?") || description.matcher(line).find()) continue;
      int last = entries.size() - 1;
      String previous = last < 0 ? "" : entries.get(last);
      if (last >= 0 && degree.matcher(line).find() && !previous.matches(".*\\s[—–-]\\s.*")) {
        entries.set(last, previous + " — " + line);
      } else if (last >= 0 && line.matches("(?i)(?:(?:Graduated|Expected)\\s+)?" + EDUCATION_DATE + "(?:\\s*[–—-]\\s*(?:" + EDUCATION_DATE + "|Present))?")) {
        entries.set(last, previous + " | " + line);
      } else if (last < 0 || line.matches(".*(?:\\s[—–-]\\s|\\s\\|\\s).*")
          || Pattern.compile("(?i)\\b(?:university|college|school|institute|academy|bootcamp)\\b").matcher(line).find()) {
        entries.add(line);
      }
    }
    return String.join("\n\n", entries.stream().map(ResumeCleanup::educationDateOnce).toList());
  }

  static JsonNode normalize(JsonNode source) {
    if (!(source instanceof ObjectNode)) return source;
    ObjectNode result = source.deepCopy();
    Map<String,String> headings = Map.of(
      "clinicalRotations", "clinical rotations",
      "clinicalTraining", "clinical training",
      "proceduresSpecialty", "procedures\\s*(?:&|and)\\s*specialty experience",
      "experience", "(?:(?:additional|earlier|previous|other|professional|work|relevant)\\s+)?(?:experience|jobs|roles|companies|employment history|work history)",
      "skills", "(?:technical |professional )?skills", "projects", "(?:technical |personal |selected |software )?projects",
      "education", "education", "certifications", "certifications(?:\\s*(?:&|and)\\s*training)?", "summary", "(?:professional )?summary");
    headings.forEach((key, pattern) -> result.put(key, result.path(key).asText().lines()
      .filter(line -> !line.strip().replaceAll("^#+\\s*|[*_:]+", "").matches("(?i)"+pattern))
      .collect(java.util.stream.Collectors.joining("\n")).strip()));
    List<String> projects = new ArrayList<>();
    Pattern urls = Pattern.compile("https?://[^\\s|)]+", Pattern.CASE_INSENSITIVE);
    for (String block : result.path("projects").asText().split("\\n\\s*\\n")) {
      var lines = new ArrayList<>(Arrays.asList(block.split("\\R")));
      if (lines.isEmpty()) continue;
      String heading = lines.get(0);
      Set<String> seen = new HashSet<>();
      var existing = urls.matcher(heading);
      while(existing.find()) seen.add(existing.group());
      List<String> body = new ArrayList<>();
      for (String line : lines.subList(1, lines.size())) {
        var matches = urls.matcher(line);
        String remainder = line.replaceAll("\\[[^\\]]*\\]\\(https?://[^)]+\\)", "")
            .replaceAll("https?://[^\\s|)]+", "").replaceAll("(?i)\\b(?:github|gitlab|bitbucket|repo(?:sitory)?|live|demo|links?)\\b", "")
            .replaceAll("[\\s:|•*–—-]", "");
        if ((remainder.isEmpty() || line.matches("(?i)^\\s*(?:demo|live demo|repository|repo|github(?: repo)?|links?)\\s*:.*")) && matches.find()) {
          do { String url = matches.group(); if (seen.add(url)) {
            String label = url.matches("(?i).*//(?:www\\.)?(?:github\\.com|gitlab\\.com|bitbucket\\.org)/.*") ? "GitHub" : "Demo";
            String link = " | ["+label+"]("+url+")";
            int stack = heading.indexOf(" — ");
            heading = stack < 0 ? heading+link : heading.substring(0,stack)+link+heading.substring(stack);
          }} while(matches.find());
        } else body.add(line);
      }
      projects.add(heading+(body.isEmpty()?"":"\n"+String.join("\n",body)));
    }
    result.put("projects",String.join("\n\n",projects));
    result.put("experience", ExperienceOrder.sort(result.path("experience").asText()));
    result.put("education", compactEducation(result.path("education").asText()));
    result.put("certifications", CredentialFormatting.normalize(result.path("certifications").asText()));
    return result;
  }
}
