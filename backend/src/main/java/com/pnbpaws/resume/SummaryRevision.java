package com.pnbpaws.resume;

import java.util.function.Function;

final class SummaryRevision {
  static boolean valid(String summary) {
    String text = summary.strip();
    int words = text.isEmpty() ? 0 : text.split("\\s+").length;
    int sentences = text.isEmpty() ? 0 : text.split("[.!?]+(?:\\s+|$)").length;
    return words >= 25 && words <= 48 && sentences >= 1 && sentences <= 2;
  }

  static String finish(String draft, String original, Function<String, String> rewrite) {
    String summary = draft.strip();
    for (int attempt = 0; attempt < 3; attempt++) {
      if (valid(summary)) return summary;
      try {
        String revised = rewrite.apply(summary).strip();
        if (!revised.isBlank()) summary = revised;
      } catch (RuntimeException unavailable) { break; }
    }

    return summary.isBlank() ? draft.strip() : summary;
  }
}
