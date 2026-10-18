package com.pnbpaws.resume;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

class HelpDocumentsTest {
  @Test void privateLinksExpireAndExpiredFilesAreDeleted() {
    var db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    var clock = new BetaPassesTest.MutableClock();
    var files = new HelpDocuments(db, clock, "https://betarp.pnbpaws.com");
    var link = files.store("resume.docx", new byte[]{1,2,3});
    assertEquals("https://betarp.pnbpaws.com/api/help-documents/" + link.token(), link.url());
    assertArrayEquals(new byte[]{1,2,3}, files.get(link.token()).bytes());
    assertThrows(ResponseStatusException.class, () -> files.get("unknown"));
    assertFalse(db.queryForObject("SELECT token_hash FROM help_document", String.class).equals(link.token()));
    clock.now += Duration.ofDays(7).toMillis();
    assertThrows(ResponseStatusException.class, () -> files.get(link.token()));
    assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM help_document", Integer.class));
  }
  @Test void failedMailCanRemoveTheLinkAndLocalAddressesCannotBeShared() {
    var db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    var clock = new BetaPassesTest.MutableClock();
    var files = new HelpDocuments(db, clock, "https://betarp.pnbpaws.com");
    var link = files.store("resume.docx", new byte[]{1}); files.remove(link.token());
    assertThrows(ResponseStatusException.class, () -> files.get(link.token()));
    var local = new HelpDocuments(db, clock, "http://127.0.0.1:5173");
    assertThrows(ResponseStatusException.class, () -> local.store("resume.docx", new byte[]{1}));
  }
}
