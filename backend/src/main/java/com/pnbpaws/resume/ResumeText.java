package com.pnbpaws.resume;

import java.util.regex.Pattern;

class ResumeText {
  static long sentences(String text) {
    String prepared =
        text.strip()
            .replaceAll("(?i)\\b(?:e\\.g|i\\.e|B\\.S|M\\.S|Ph\\.D|U\\.S)\\.", "abbreviation")
            .replaceAll("(?i)\\b(?:Mr|Ms|Mrs|Dr|Prof|Sr|Jr|Inc|Ltd|etc)\\.", "abbreviation")
            .replaceAll("(?<=\\d)\\.(?=\\d)", "decimal")
            .replaceAll("(?i)\\b(?:Node|Next|Vue|React)\\.js\\b", "technology");
    return Pattern.compile("[^.!?]+(?:[.!?]+|$)")
        .matcher(prepared)
        .results()
        .filter(match -> !match.group().isBlank())
        .count();
  }
}
