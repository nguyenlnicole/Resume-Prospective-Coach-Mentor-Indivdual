package com.pnbpaws.resume;

import java.util.function.Function;

final class SummaryRevision {
  static boolean valid(String summary) {
    String text = summary.strip();
    int words = text.isEmpty() ? 0 : text.split("\\s+").length;
    int sentences = text.isEmpty() ? 0 : text.split("[.!?]+(?:\\s+|$)").length;
    return words >= 35 && words <= 45 && sentences >= 2 && sentences <= 4;
  }

  static String finish(String draft, String original, Function<String, String> rewrite) {
    String summary = draft.strip();
    for (int attempt = 0; attempt < 2; attempt++) {
      if (valid(summary) && !summary.equals(original.strip())) return summary;
      summary = rewrite.apply(summary).strip();
    }

    return summary.isBlank() ? draft.strip() : summary;
  }
}
