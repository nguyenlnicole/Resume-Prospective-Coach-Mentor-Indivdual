package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;

import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class AdminService {
  private final JdbcTemplate db;
  private final String email;
  final MailService mail;
  final String publicUrl;
  private final Map<String, Deque<Long>> limits = new ConcurrentHashMap<>();

  AdminService(
      JdbcTemplate db,
      MailService mail,
      @Value("${app.admin-email}") String email,
      @Value("${app.admin-password-hash}") String hash,
      @Value("${app.review-email}") String recipient,
      @Value("${app.public-url}") String publicUrl) {
    this.db = db;
    this.email = email.trim().toLowerCase(Locale.ROOT);
    this.mail = mail;
    this.publicUrl = publicUrl;
    db.execute(
        "CREATE TABLE IF NOT EXISTS administrator (id INT PRIMARY KEY, email VARCHAR(254),"
            + " password_hash VARCHAR(512), version INT NOT NULL)");
    db.execute("CREATE TABLE IF NOT EXISTS admin_code (email VARCHAR(254) PRIMARY KEY, hash VARCHAR(64) NOT NULL, expires BIGINT NOT NULL, attempts INT NOT NULL)");
    db.execute("CREATE TABLE IF NOT EXISTS review_recipient (email VARCHAR(254) PRIMARY KEY)");
    db.execute(
        "CREATE TABLE IF NOT EXISTS reset_token (hash VARCHAR(64) PRIMARY KEY, expires BIGINT NOT"
            + " NULL)");
    if (db.queryForObject("SELECT COUNT(*) FROM administrator", Integer.class) == 0
        && validEmail(this.email))
      db.update("INSERT INTO administrator VALUES(1,?,?,1)", this.email, hash);
    if (db.queryForObject("SELECT COUNT(*) FROM review_recipient", Integer.class) == 0)
      db.update("INSERT INTO review_recipient VALUES(?)", recipient);
  }

  synchronized void limit(String key, int maximum) {
    long now = System.currentTimeMillis();
    if (limits.size() > 10000)
      limits
          .entrySet()
          .removeIf(e -> e.getValue().isEmpty() || e.getValue().peekLast() < now - 900000);
    Deque<Long> attempts = limits.computeIfAbsent(key, k -> new ArrayDeque<>());
    while (!attempts.isEmpty() && attempts.peekFirst() < now - 900000) attempts.removeFirst();
    if (attempts.size() >= maximum)
      throw new ResponseStatusException(
          TOO_MANY_REQUESTS, "Too many attempts. Please try again in 15 minutes.");
    attempts.addLast(now);
  }

  public String email() { return email; }

  boolean configured() {
    return db.queryForObject("SELECT COUNT(*) FROM administrator", Integer.class) > 0;
  }

  int version() {
    return db.queryForObject("SELECT version FROM administrator WHERE id=1", Integer.class);
  }

  @Transactional
  public void sendCode(String address) {
    if (!validEmail(address)) throw new ResponseStatusException(BAD_REQUEST, "Enter a valid email address.");
    if (!mail.enabled) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Admin sign-in email is not connected yet. Connect the email service before signing in.");
    if (!configured() || !email.equalsIgnoreCase(address.trim())) return;
    limit("admin-code:" + email, 3);
    String code = String.format("%06d", new SecureRandom().nextInt(1000000));
    db.update("MERGE INTO admin_code KEY(email) VALUES(?,?,?,0)", email, digest(email + ":" + code), System.currentTimeMillis() + 600000);
    mail.text(email, "Your Resume Prospective admin sign-in code",
        "Enter this code in Admin Sign In: " + code + "\n\nIt expires in 10 minutes and works once. If you did not request it, ignore this email.");
  }

  @Transactional
  public Integer verifyCode(String address, String code) {
    if (address == null || !email.equalsIgnoreCase(address.trim()) || code == null || !code.matches("[0-9]{6}")) return null;
    var rows = db.queryForList("SELECT * FROM admin_code WHERE email=? FOR UPDATE", email);
    if (rows.isEmpty()) return null;
    var row = rows.getFirst();
    if (((Number) row.get("EXPIRES")).longValue() <= System.currentTimeMillis() || ((Number) row.get("ATTEMPTS")).intValue() >= 5) return null;
    db.update("UPDATE admin_code SET attempts=attempts+1 WHERE email=?", email);
    if (!MessageDigest.isEqual(digest(email + ":" + code).getBytes(java.nio.charset.StandardCharsets.US_ASCII), row.get("HASH").toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII))) return null;
    db.update("DELETE FROM admin_code WHERE email=?", email);
    return version();
  }

  List<String> recipients() {
    return db.queryForList("SELECT email FROM review_recipient ORDER BY email", String.class);
  }

  @Transactional
  public void recipients(List<String> recipients) {
    var unique =
        recipients.stream().map(x -> x.trim().toLowerCase(Locale.ROOT)).distinct().toList();
    if (unique.isEmpty() || unique.size() > 20 || unique.stream().anyMatch(x -> !validEmail(x)))
      throw new ResponseStatusException(BAD_REQUEST, "Add between 1 and 20 valid email addresses.");
    db.update("DELETE FROM review_recipient");
    for (String address : unique) db.update("INSERT INTO review_recipient VALUES(?)", address);
  }

  @Transactional
  public void forgot(String address) {
    if (!mail.enabled)
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE,
          "Password reset email is not connected yet. Please contact the app owner.");
    if (!configured() || !email.equalsIgnoreCase(address.trim())) return;
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random(32));
    db.update("DELETE FROM reset_token");
    db.update(
        "INSERT INTO reset_token VALUES(?,?)", digest(token), System.currentTimeMillis() + 1800000);
    mail.text(
        email,
        "Reset your Resume Prospective password",
        "Return to Resume Prospective, choose Admin Sign In, then Forgot password and Enter My Reset Code. Paste this single-use code within 30 minutes:\n\n"
            + token
            + "\n\nIf you did not request this reset, ignore this email.");
  }

  @Transactional
  public void reset(String token, String password) {
    if (token == null
        || token.length() > 100
        || password == null
        || password.length() < 12
        || password.length() > 256)
      throw new ResponseStatusException(
          BAD_REQUEST, "Use a valid reset code and a password with 12–256 characters.");
    int removed =
        db.update(
            "DELETE FROM reset_token WHERE hash=? AND expires>?",
            digest(token),
            System.currentTimeMillis());
    if (removed != 1)
      throw new ResponseStatusException(
          BAD_REQUEST, "This reset code is invalid or has expired. Request a new one.");
    db.update(
        "UPDATE administrator SET password_hash=?, version=version+1 WHERE id=1", hash(password));
    db.update("DELETE FROM reset_token");
  }

  static boolean validEmail(String x) {
    return x != null
        && x.length() <= 254
        && x.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        && !x.contains("\r")
        && !x.contains("\n");
  }

  static byte[] random(int length) {
    byte[] b = new byte[length];
    new SecureRandom().nextBytes(b);
    return b;
  }

  static String hash(String password) {
    byte[] salt = random(16);
    return "pbkdf2$600000$"
        + Base64.getEncoder().encodeToString(salt)
        + "$"
        + Base64.getEncoder().encodeToString(derive(password, salt, 600000));
  }

  static boolean verify(String password, String stored) {
    try {
      String[] p = stored.split("\\$");
      return MessageDigest.isEqual(
          derive(password, Base64.getDecoder().decode(p[2]), Integer.parseInt(p[1])),
          Base64.getDecoder().decode(p[3]));
    } catch (Exception e) {
      return false;
    }
  }

  static byte[] derive(String password, byte[] salt, int iterations) {
    try {
      var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
      try {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .getEncoded();
      } finally {
        spec.clearPassword();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Password hashing unavailable");
    }
  }

  static String digest(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException();
    }
  }
}
