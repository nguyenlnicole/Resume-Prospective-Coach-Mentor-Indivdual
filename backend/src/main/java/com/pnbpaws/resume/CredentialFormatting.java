package com.pnbpaws.resume;

import java.util.regex.Pattern;

record CredentialFormatting(String name, String issuer, String date) {
  static CredentialFormatting parse(String source) {
    String clean = source.replaceAll("(?i)&#(?:x20|32);|&nbsp;", " ")
        .replaceAll("[*_]", "").replaceAll("\\s+", " ").strip();
    var dated = Pattern.compile("^(.*?)\\s*\\|\\s*([^|]*\\b(?:19|20)\\d{2}[^|]*)$").matcher(clean);
    String title = clean, date = "";
    if (dated.matches()) { title = dated.group(1).strip(); date = dated.group(2).strip(); }
    var separator = Pattern.compile(",\\s+|\\s+[—–-]\\s+").matcher(title);
    return separator.find() ? new CredentialFormatting(title.substring(0, separator.start()), title.substring(separator.end()), date)
        : new CredentialFormatting(title, "", date);
  }
  String formatted() { return name + (issuer.isBlank() ? "" : ", " + issuer) + (date.isBlank() ? "" : " | " + date); }
  static String normalize(String source) {
    return source.lines().map(line -> parse(line).formatted()).collect(java.util.stream.Collectors.joining("\n"));
  }
}
