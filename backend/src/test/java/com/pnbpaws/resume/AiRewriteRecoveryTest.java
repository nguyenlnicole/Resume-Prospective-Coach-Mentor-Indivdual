package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AiRewriteRecoveryTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final String source = "Example Company\nEngineer II | August 2020 – August 2022\n• Built 8 APIs.\n• Reduced tickets by 35%.\nEngineer | August 2018 – August 2020\n• Wrote tests.";

  @Test void missingOrExtraBulletsKeepTheWholeSectionWithoutShiftingClaims() throws Exception {
    assertEquals(source, AiService.replaceBullets(source, mapper.readTree("[\"Built 8 APIs.\"]"), false, false));
    assertEquals(source, AiService.replaceBullets(source, mapper.readTree("[\"A\",\"B\",\"C\",\"D\"]"), false, false));
    assertEquals(source, AiService.replaceBullets(source, mapper.nullNode(), false, false));
  }

  @Test void completeRewritePreservesHeadingsDatesAndNumbers() throws Exception {
    String result = AiService.replaceBullets(source, mapper.readTree("[\"Shipped 8 APIs.\",\"Cut tickets by 35%.\",\"Added tests.\"]"), false, false);
    assertTrue(result.contains("Engineer II | August 2020 – August 2022\n• Shipped 8 APIs.\n• Cut tickets by 35%."));
    assertTrue(result.contains("Engineer | August 2018 – August 2020\n• Added tests."));
  }

  @Test void changedMetricsKeepTheOriginalBullet() throws Exception {
    String result = AiService.replaceBullets(source, mapper.readTree("[\"Built 80 APIs.\",\"Reduced tickets by 35%.\",\"Wrote tests.\"]"), false, false);
    assertEquals(source, result);
  }
}