package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PasswordPersistenceTest {
  @Test void savedPasswordsAndAppSumoAccessSurviveRestartAndNewBootstrapHash(@TempDir Path dir) {
    String url="jdbc:h2:file:"+dir.resolve("accounts").toAbsolutePath().toString().replace('\\','/');
    var db=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
    var mail=mock(MailService.class);
    var admin=new AdminService(db,mail,"owner@example.com","original-bootstrap-hash","support@example.com","http://localhost");
    var accounts=new RegisteredAccounts(db,admin,mail,Clock.systemUTC(),new ObjectMapper());
    db.execute("CREATE TABLE coach(email VARCHAR(254) PRIMARY KEY)");
    accounts.register("buyer@example.com","Resume12!","coach");accounts.verified("buyer@example.com");
    db.update("UPDATE administrator SET password_hash='saved-owner-hash',version=7 WHERE id=1");
    String buyerHash=db.queryForObject("SELECT password_hash FROM registered_account WHERE email='buyer@example.com'",String.class);
    var codes=new AppSumoCodes(db,Clock.systemUTC());
    String csv=java.util.stream.IntStream.range(0,1000).mapToObj(i->"AS-PERSIST-EXAMPLE-"+i).collect(java.util.stream.Collectors.joining("\n"));
    String batch=codes.importCodes(csv).get("id").toString();codes.configure(batch,true);codes.redeem("buyer@example.com","AS-PERSIST-EXAMPLE-0");
    long expiry=((Number)codes.entitlement("buyer@example.com").get("expiresAt")).longValue();
    db.execute("SHUTDOWN");
    var reopened=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
    var afterAdmin=new AdminService(reopened,mail,"owner@example.com","different-bootstrap-hash","support@example.com","http://localhost");
    var after=new RegisteredAccounts(reopened,afterAdmin,mail,Clock.systemUTC(),new ObjectMapper());
    after.appSumo=new AppSumoCodes(reopened,Clock.systemUTC());
    assertEquals("saved-owner-hash",reopened.queryForObject("SELECT password_hash FROM administrator WHERE id=1",String.class));
    assertEquals(7,afterAdmin.version());
    assertEquals(buyerHash,reopened.queryForObject("SELECT password_hash FROM registered_account WHERE email='buyer@example.com'",String.class));
    assertTrue(after.matches("buyer@example.com","Resume12!"));
    assertEquals("appsumo",after.state("buyer@example.com").get("plan"));
    assertEquals(expiry,after.state("buyer@example.com").get("expiresAt"));
    reopened.execute("SHUTDOWN");
  }
}
