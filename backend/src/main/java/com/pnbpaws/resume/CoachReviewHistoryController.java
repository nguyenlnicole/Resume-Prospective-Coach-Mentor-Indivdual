package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/coach/history")
class CoachReviewHistoryController {
  private final CoachAccess access;
  private final CoachReviewHistory history;
  private final PdfService pdf;
  private final DocxService docx;

  CoachReviewHistoryController(CoachAccess access, CoachReviewHistory history, PdfService pdf, DocxService docx) {
    this.access = access;
    this.history = history;
    this.pdf = pdf;
    this.docx = docx;
  }

  private String coach(HttpSession session) {
    var state = access.state(session);
    if (!Boolean.TRUE.equals(state.get("allowed")) || !"coach".equals(state.get("role"))
        || "beta-pass".equals(state.get("accessKind")))
      throw new ResponseStatusException(FORBIDDEN, "Coach and Mentor access is required.");
    return Objects.toString(state.get("email"), "");
  }

  @GetMapping
  ResponseEntity<List<Map<String, Object>>> list(HttpSession session) {
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(history.list(coach(session)));
  }

  record FinalResume(JsonNode resume, String level, String font, String dates, String linkColor) {}

  @PostMapping("/final")
  Map<String, Boolean> saveFinal(@RequestBody FinalResume input, HttpSession session) {
    String owner = coach(session);
    String id = Objects.toString(session.getAttribute("coachHistoryId"), "");
    if (id.isBlank() || !Objects.equals(input.level(), session.getAttribute("reviewedLevel")))
      throw new ResponseStatusException(CONFLICT, "Review a resume before saving its final version.");
    if (input.resume() == null || !input.resume().isObject() || input.resume().toString().length() > 25_000)
      throw new ResponseStatusException(BAD_REQUEST, "Choose a valid finished resume.");
    byte[] pdfBytes = pdf.create(input.resume(), input.level(), input.font(), input.dates(), input.linkColor());
    byte[] docxBytes = docx.create(input.resume(), input.level(), input.font(), input.dates(), input.linkColor());
    history.saveFinal(owner, id, pdfBytes, docxBytes);
    return Map.of("ok", true);
  }

  @GetMapping("/{id}/{kind}")
  ResponseEntity<byte[]> download(@PathVariable String id, @PathVariable String kind, HttpSession session) {
    var file = history.download(coach(session), id, kind);
    if (file.bytes() == null) throw new ResponseStatusException(NOT_FOUND, "The finished resume is not available yet.");
    String filename = URLEncoder.encode(file.filename(), StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(MediaType.parseMediaType(file.type())).body(file.bytes());
  }

  @GetMapping("/{id}/bundle")
  ResponseEntity<byte[]> bundle(@PathVariable String id, @RequestParam List<String> kinds, HttpSession session) {
    String owner = coach(session);
    var chosen = new LinkedHashSet<>(kinds);
    if (chosen.isEmpty() || chosen.size() > 4
        || !Set.of("original", "feedback", "pdf", "docx").containsAll(chosen))
      throw new ResponseStatusException(BAD_REQUEST, "Select valid files to download.");
    try (var out = new ByteArrayOutputStream(); var zip = new ZipOutputStream(out)) {
      for (String kind : chosen) {
        var file = history.download(owner, id, kind);
        if (file.bytes() == null) throw new ResponseStatusException(NOT_FOUND, "A selected file is not available.");
        zip.putNextEntry(new ZipEntry(file.filename()));
        zip.write(file.bytes());
        zip.closeEntry();
      }
      zip.finish();
      return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student-review-files.zip")
          .contentType(MediaType.parseMediaType("application/zip")).body(out.toByteArray());
    } catch (IOException e) {
      throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "The selected files could not be packaged.");
    }
  }
}
