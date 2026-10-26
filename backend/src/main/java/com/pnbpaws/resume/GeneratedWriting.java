package com.pnbpaws.resume;

import java.util.function.Function;

final class GeneratedWriting {
  static String instructions(String level) {
    int count = java.util.List.of("Senior", "Management").contains(level) ? 4 : 3;
    return "\nFinal writing targets override earlier length guidance: professional summary 25-48 words; "
        + count + " experience bullets per role, 20-30 words each; project description at most 30 words, excluding title, links and technology lists. "
        + "Automatically revise wording using only supplied facts. Never invent facts, pad with generic claims, remove roles or block progress to meet a word target. Preserve user-selected claims and all factual metrics. Use actual bullet symbols for experience. ";
  }
  static boolean valid(String text, String section) {
    if (text.isBlank()) return true;
    if (section.equals("description")) return words(text) <= 30;
    if (section.equals("experience")) {
      for (String line : text.split("\\R")) {
        if (line.matches("^\\s*[•*\\-]\\s+.*")) {
          int count = words(line.replaceFirst("^\\s*[•*\\-]\\s+", ""));
          if (count < 20 || count > 30) return false;
        }
      }
    }
    if (section.equals("projects")) {
      for (String line : text.split("\\R")) {
        if (line.matches("(?i)^\\s*(technologies|stack|organization|role)\\s*:.*") || line.contains("http") || line.contains("|")) continue;
        if (line.matches(".*[.!?]\\s*$") && words(line) > 30) return false;
      }
    }
    return true;
  }
  static int words(String text) { return text.isBlank() ? 0 : text.strip().split("\\s+").length; }
  static String finish(String draft, String section, Function<String,String> rewrite) {
    String current = draft.strip();
    for (int attempt = 0; attempt < 2 && !valid(current, section); attempt++) {
      try {
        String next = rewrite.apply(current).strip();
        if (!next.isBlank()) current = next;
      } catch (RuntimeException unavailable) { break; }
    }
    return current;
  }
}
