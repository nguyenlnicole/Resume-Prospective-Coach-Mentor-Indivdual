package com.pnbpaws.resume;


record ResumeTypography(String font, float body, float name, float heading, float contact,
                        float side, float vertical, float leading) {
  static ResumeTypography forLevel(String level) {
    return ResumeProfiles.technical(level)
        ? new ResumeTypography("Calibri", 10, 23, 10, 10, 43.2f, 36, 12)
        : new ResumeTypography("Arial", 11, 24, 12, 10.5f, 54, 54, 13.5f);
  }
}
