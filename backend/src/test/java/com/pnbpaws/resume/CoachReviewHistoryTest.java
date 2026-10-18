package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

class CoachReviewHistoryTest {
  private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }

  @Test void keepsOnlyFiveClientsPerCoachAndDownloadsStayPrivate() {
    var db = new JdbcTemplate(new DriverManagerDataSource(
        "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    var clock = new CoachAccessTest.MutableClock();
    var history = new CoachReviewHistory(db, clock);
    String first = history.saveReview("coach-a@example.com", "ada@example.com", "Ada Lovelace",
        "ada.pdf", bytes("original"), bytes("feedback"));
    history.saveFinal("coach-a@example.com", first, bytes("finished-pdf"), bytes("finished-word"));
    assertEquals("Ada", history.list("coach-a@example.com").getFirst().get("firstName"));
    assertArrayEquals(bytes("original"), history.download("coach-a@example.com", first, "original").bytes());
    assertEquals("Ada-" + first.substring(0, 8) + "-uploaded-resume.pdf",
        history.download("coach-a@example.com", first, "original").filename());
    assertArrayEquals(bytes("feedback"), history.download("coach-a@example.com", first, "feedback").bytes());
    assertArrayEquals(bytes("finished-word"), history.download("coach-a@example.com", first, "docx").bytes());
    assertThrows(ResponseStatusException.class, () -> history.download("coach-b@example.com", first, "original"));
    assertThrows(ResponseStatusException.class,
        () -> history.saveFinal("coach-b@example.com", first, bytes("wrong"), bytes("wrong")));

    for (int i = 0; i < 5; i++) {
      clock.now += 1000;
      history.saveReview("coach-a@example.com", "client" + i + "@example.com", "Client " + i,
          "client.pdf", bytes("original"), bytes("feedback"));
    }
    assertEquals(5, history.list("coach-a@example.com").size());
    assertThrows(ResponseStatusException.class, () -> history.download("coach-a@example.com", first, "original"));
    assertTrue(history.list("coach-b@example.com").isEmpty());
  }

  @Test void repeatClientReplacesFolderInsteadOfUsingAnotherSlot() {
    var db = new JdbcTemplate(new DriverManagerDataSource(
        "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    var history = new CoachReviewHistory(db, new CoachAccessTest.MutableClock());
    history.saveReview("coach@example.com", "sam@example.com", "Sam First", "old.docx",
        bytes("old"), bytes("feedback"));
    String updated = history.saveReview("coach@example.com", "sam@example.com", "Sam New", "new.docx",
        bytes("new"), bytes("feedback"));
    assertEquals(1, history.list("coach@example.com").size());
    assertArrayEquals(bytes("new"), history.download("coach@example.com", updated, "original").bytes());
  }
}
