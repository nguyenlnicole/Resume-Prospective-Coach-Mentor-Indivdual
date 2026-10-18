package com.pnbpaws.resume;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
class AdminServiceTest {
  AdminService service;
  JdbcTemplate db;
  String code;
  int sent;
  @BeforeEach void setup() {
    db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    var mail = new MailService(null, true, "admin@example.com") {
      @Override void text(String to, String subject, String body) {
        assertEquals("admin@example.com", to);
        var match = java.util.regex.Pattern.compile("[0-9]{6}").matcher(body);
        assertTrue(match.find()); code = match.group(); sent++;
      }
    };
    service = new AdminService(db, mail, "admin@example.com", "", "review@example.com", "http://localhost");
  }
  @Test void noPasswordRequiredAndCodeWorksOnce() {
    assertTrue(service.configured()); service.sendCode("admin@example.com");
    assertEquals(1, service.verifyCode("ADMIN@example.com", code));
    assertNull(service.verifyCode("admin@example.com", code));
  }
  @Test void onlyOwnerReceivesCodesAndCanVerify() {
    service.sendCode("other@example.com"); assertEquals(0, sent);
    service.sendCode("admin@example.com");
    assertNull(service.verifyCode("other@example.com", code));
    assertEquals(1, service.verifyCode("admin@example.com", code));
  }
  @Test void expiredCodesAndFiveWrongAttemptsAreRejected() {
    service.sendCode("admin@example.com");
    String wrong = code.equals("000000") ? "111111" : "000000";
    for (int i=0;i<5;i++) assertNull(service.verifyCode("admin@example.com", wrong));
    assertNull(service.verifyCode("admin@example.com", code));
    service.sendCode("admin@example.com"); db.update("UPDATE admin_code SET expires=0");
    assertNull(service.verifyCode("admin@example.com", code));
  }
  @Test void resendReplacesCodeAndRequestLimitApplies() {
    service.sendCode("admin@example.com"); service.sendCode("admin@example.com"); service.sendCode("admin@example.com");
    assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM admin_code", Integer.class));
    assertThrows(Exception.class, () -> service.sendCode("admin@example.com"));
  }
  @Test void disabledEmailCannotIssueCodes() {
    var disabled = new AdminService(db, new MailService(null, false, "admin@example.com"), "admin@example.com", "", "review@example.com", "http://localhost");
    assertThrows(Exception.class, () -> disabled.sendCode("admin@example.com"));
    assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM admin_code", Integer.class));
  }
  @Test void recipientsRejectEmptyAndInvalidLists() {
    assertThrows(Exception.class, () -> service.recipients(List.of()));
    assertThrows(Exception.class, () -> service.recipients(List.of("invalid")));
    service.recipients(List.of("new@example.com"));
    assertEquals(List.of("new@example.com"), service.recipients());
  }
}
