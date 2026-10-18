package com.pnbpaws.resume;

import java.util.*;

final class ExperienceIntegrity {
  private record Role(String employer, String heading, int bullets) {}

  private static String key(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
  }

  private static List<Role> roles(String text) {
    List<Role> result = new ArrayList<>();
    String employer = "", heading = null;
    int bullets = 0;
    for (String raw : text.split("\\R")) {
      String line = raw.strip();
      if (line.isBlank()) continue;
      if (line.matches("^[•*\\-]\\s+.*")) {
        if (heading != null) bullets++;
      } else if (line.contains("|") && line.matches(".*\\b(?:19|20)\\d{2}\\b.*")) {
        if (heading != null) result.add(new Role(key(employer), key(heading), bullets));
        String[] combined = line.split("\\s+[—–]\\s+", 2);
        if (combined.length == 2 && !combined[0].contains("|")) {
          employer = combined[0];
          heading = combined[1];
        } else heading = line;
        bullets = 0;
      } else if (!line.contains("|") && !line.matches(".*[.!?]$")) {
        if (heading != null) result.add(new Role(key(employer), key(heading), bullets));
        employer = line;
        heading = null;
        bullets = 0;
      }
    }
    if (heading != null) result.add(new Role(key(employer), key(heading), bullets));
    return result;
  }

  static String preserveGroupedRoles(String original, String proposed) {
    List<Role> source = roles(original);
    List<Role> draft = roles(proposed);
    Map<String, Long> counts = new HashMap<>();
    for (Role role : source) counts.merge(role.employer(), 1L, Long::sum);
    for (Role role : source) {
      if (role.employer().isBlank() || counts.get(role.employer()) < 2) continue;
      List<Role> matches = draft.stream().filter(candidate -> candidate.employer().equals(role.employer())
          && candidate.heading().equals(role.heading())).toList();
      if (matches.size() != 1 || matches.get(0).bullets() != role.bullets()) return original;
    }
    return proposed;
  }
}