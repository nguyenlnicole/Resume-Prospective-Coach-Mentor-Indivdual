package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ResumeFilenameTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test void usesFirstLastAndMonthYear() throws Exception {
    var resume = mapper.readTree("{\"contact\":{\"name\":\"Jordan Avery\"}}");
    assertEquals("Jordan-Avery-PNBRP-1026", ResumeFilename.base(resume, YearMonth.of(2026, 10)));
  }

  @Test void keepsFilenameSafeWhenNameHasPunctuation() throws Exception {
    var resume = mapper.readTree("{\"contact\":{\"name\":\"Anne-Marie O'Neil\"}}");
    assertEquals("Anne-Marie-O-Neil-PNBRP-0127", ResumeFilename.base(resume, YearMonth.of(2027, 1)));
  }
}
