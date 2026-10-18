package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ExperienceIntegrityTest {
  private final String source = "Example Company\nSoftware Engineer II | August 2020 – August 2022\n• Owned eight API features.\n• Reduced tickets by 35%.\nSoftware Engineer | August 2018 – August 2020\n• Implemented updates.\n• Wrote unit tests.";

  @Test void stackedTitlesAndSharedBulletsRestoreTheSeparateSourceRoles() {
    String bad = "Example Company\nSoftware Engineer II | August 2020 – August 2022\nSoftware Engineer | August 2018 – August 2020\n• Owned eight API features.\n• Reduced tickets by 35%.\n• Implemented updates.";
    assertEquals(source, ExperienceIntegrity.preserveGroupedRoles(source, bad));
  }

  @Test void missingBulletIsNotSilentlyAccepted() {
    assertEquals(source, ExperienceIntegrity.preserveGroupedRoles(source, source.replace("\n• Wrote unit tests.", "")));
  }

  @Test void separateRoleRewritesRemainAllowed() {
    String revised = source.replace("Implemented updates", "Delivered updates");
    assertEquals(revised, ExperienceIntegrity.preserveGroupedRoles(source, revised));
  }

  @Test void inventedLevelSuffixIsNotAccepted() {
    assertEquals(source, ExperienceIntegrity.preserveGroupedRoles(source, source.replace("Software Engineer |", "Software Engineer I |")));
  }
}