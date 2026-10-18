package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ResumeScoringTest {
  final ObjectMapper mapper = new ObjectMapper();

  ArrayNode ratings(String status) {
    ArrayNode values = mapper.createArrayNode();
    for (int i = 0; i < 10; i++) values.addObject().put("assessment", status);
    return values;
  }

  @Test
  void tenRequiresAllChecksAndOverallIncludesFormat() {
    var format = ratings("pass");
    var content = ratings("pass");
    assertEquals(10, ResumeScoring.score(mapper, format, content).path("value").asDouble());
    ((ObjectNode) format.get(0)).put("assessment", "missing");
    assertEquals(9, ResumeScoring.score(mapper, format, null).path("value").asDouble());
    assertEquals(9.6, ResumeScoring.score(mapper, format, content).path("value").asDouble());
    ((ObjectNode) content.get(0)).put("assessment", "partial");
    assertEquals(9.3, ResumeScoring.score(mapper, format, content).path("value").asDouble());
  }

  @Test
  void unknownChecksShowOneProvisionalScoreWithoutClaimingPerfectScore() {
    var format = ratings("pass");
    ((ObjectNode) format.get(0)).put("assessment", "unverified");
    var score = ResumeScoring.score(mapper, format, ratings("pass"));
    assertEquals(9.6, score.path("value").asDouble());
    assertEquals(9.6, score.path("minimum").asDouble());
    assertEquals(10, score.path("maximum").asDouble());
    assertFalse(score.path("allMet").asBoolean());
    assertEquals("9.6", score.path("display").asText());
  }

  @Test
  void allUnknownShowsZeroEarnedPointsWithVerificationCount() {
    var score = ResumeScoring.score(mapper, ratings("unverified"), null);
    assertEquals(0, score.path("value").asDouble());
    assertEquals("0", score.path("display").asText());
    assertEquals(10, score.path("unverified").asInt());
  }

  PdfService.Extraction extraction(int pages) {
    return new PdfService.Extraction(
        "Built REST endpoints with input validation. Certification Alpha — Issuer A. Certification"
            + " Beta — Issuer B.",
        pages,
        List.of(),
        "inline",
        List.of(),
        List.of(Map.of("id", 1)),
        List.of(),
        List.of());
  }

  ObjectNode review(String level) {
    ObjectNode result = mapper.createObjectNode();
    result.putObject("resume");
    var format = result.putObject("formatChecks");
    var content = result.putObject("contentChecks");
    for (var criterion : ResumeScoring.format(level)) check(format, criterion.id());
    for (var criterion : ResumeScoring.content(level)) check(content, criterion.id());
    return result;
  }

  void check(ObjectNode group, String id) {
    var item =
        group
            .putObject(id)
            .put("status", "pass")
            .put("evidence", "Evidence from source.")
            .put("suggestion", "Keep it.");
    item.putArray("sourceQuotes").add("Built REST endpoints with input validation.");
    item.putArray("lineIds").add(1);
  }

  @Test
  void unsupportedModelClaimsBecomeUnverified() {
    var result = review("Mid-level");
    ((ObjectNode) result.path("contentChecks").path("ownership"))
        .putArray("sourceQuotes")
        .add("Invented impressive outcome");
    ResumeScoring.apply(mapper, result, extraction(1), "Mid-level");
    assertEquals("unverified", result.path("contentFindings").get(0).path("assessment").asText());
    assertEquals(9.4, result.path("overallScore").asDouble());
    assertEquals(10, result.path("formatScore").asDouble());
  }

  @Test
  void anUnlinkedCodingProjectLowersTheProjectRating() {
    var result = review("Mid-level");
    ((ObjectNode) result.path("resume")).put("projects",
        "Care app\nTech: Java\nBuilt a care app.\n\nStore app | https://github.com/example/store\nTech: React\nBuilt a store app.");
    ResumeScoring.apply(mapper, result, extraction(1), "Mid-level");
    assertTrue(java.util.stream.StreamSupport.stream(result.path("formatFindings").spliterator(), false)
        .anyMatch(item -> item.path("id").asText().equals("projects")
            && item.path("assessment").asText().equals("partial")));
    assertTrue(result.path("formatScore").asDouble() < 10);
  }

  @Test
  void missingChecksAndBadLineIdsCannotPass() {
    var result = review("Senior");
    result.remove("contentChecks");
    var header = (ObjectNode) result.path("formatChecks").path("header");
    header.putArray("sourceQuotes");
    header.putArray("lineIds").add(9999);
    ResumeScoring.apply(mapper, result, extraction(1), "Senior");
    assertEquals(10, result.path("contentFindings").size());
    assertEquals("unverified", result.path("formatFindings").get(1).path("assessment").asText());
    assertEquals(3.6, result.path("overallScore").asDouble());
  }

  @Test
  void pageCountOverridesAnUnsupportedLayoutPass() {
    var result = review("Senior");
    ResumeScoring.apply(mapper, result, extraction(2), "Senior");
    assertEquals("missing", result.path("formatFindings").get(7).path("assessment").asText());
    assertEquals(9, result.path("formatScore").asDouble());
  }

  @Test
  void managementNeedsTwoSeparatelySupportedCredentials() {
    var result = review("Management");
    var cert = (ObjectNode) result.path("contentChecks").path("certifications");
    cert.putArray("sourceQuotes").add("Certification Alpha — Issuer A.");
    ResumeScoring.apply(mapper, result, extraction(1), "Management");
    assertEquals("unverified", result.path("contentFindings").get(0).path("assessment").asText());
    result = review("Management");
    cert = (ObjectNode) result.path("contentChecks").path("certifications");
    cert.putArray("sourceQuotes")
        .add("Certification Alpha — Issuer A.")
        .add("Certification Beta — Issuer B.");
    ResumeScoring.apply(mapper, result, extraction(1), "Management");
    assertEquals("pass", result.path("contentFindings").get(0).path("assessment").asText());
  }

  @Test
  void careerRubricsHaveTenDistinctChecksAndDifferentExpectations() {
    for (String level : ApiController.LEVELS) {
      assertEquals(10, ResumeScoring.format(level).size());
      assertEquals(10, ResumeScoring.content(level).size());
      assertEquals(
          10,
          ResumeScoring.content(level).stream()
              .map(ResumeScoring.Criterion::id)
              .distinct()
              .count());
    }
    assertTrue(
        ResumeScoring.content("Entry / Junior").get(0).requirement().contains("at least two"));
    assertTrue(ResumeScoring.format("Senior").get(9).requirement().contains("optional"));
    assertTrue(ResumeScoring.content("Management").get(0).requirement().contains("Two distinct"));
  }

  @Test
  void summarySentencesDoNotSplitTechnologyOrDegreeNames() {
    assertEquals(
        2,
        ResumeText.sentences(
            "Engineer using Node.js and Java 21.0. Holds a B.S. in computer science."));
    assertEquals(0, ResumeText.sentences("  "));
    assertEquals(3, ResumeText.sentences("First sentence. Second sentence! Third sentence?"));
  }
}
