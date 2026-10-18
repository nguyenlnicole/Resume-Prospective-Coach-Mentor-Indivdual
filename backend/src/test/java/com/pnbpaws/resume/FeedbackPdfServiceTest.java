package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class FeedbackPdfServiceTest {
  @Test void mergesPositiveEvidenceIntoFourToSixCards() {
    var findings = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
    for (int i = 0; i < 11; i++) findings.add(new ObjectMapper().createObjectNode()
        .put("title", "Strength " + i).put("evidence", "Supported contribution " + i + "."));
    var cards = FeedbackPdfService.mergedStrengths(findings);
    assertEquals(4, cards.size());
    for (var card : cards) {
      long sentences = card.path("evidence").asText().chars().filter(c -> c == '.').count();
      assertTrue(sentences >= 3 && sentences <= 4);
    }
    assertTrue(FeedbackPdfService.mergedStrengths(java.util.List.of()).isEmpty());
  }
  @Test
  void reportIncludesFullEvidenceAndNextStep() throws Exception {
    var review = new ObjectMapper().readTree("""
        {"formatRating":{"value":7.5},"overallRating":{"value":6.8},
         "formatFindings":[{"title":"Clear sections","status":"pass","evidence":"Experience and education have distinct headings."},{"title":"Summary length","status":"missing",
           "evidence":"The summary has 18 words, so it lacks context about the work and scope.",
           "suggestion":"Add verified responsibilities and outcomes in 35 to 45 words."}],
         "contentFindings":[{"title":"Scope and outcomes","status":"partial",
           "evidence":"The role names the work but not the scale of the systems supported.",
           "suggestion":"Add a real system count if known."}]}
        """);
    byte[] bytes = new FeedbackPdfService().create(review, "Senior");
    try (var pdf = Loader.loadPDF(bytes)) {
      String text = new PDFTextStripper().getText(pdf);
      assertTrue(text.contains("Presentation: 7.5 / 10"));
      assertTrue(text.contains("The summary has 18 words"));
      assertTrue(text.contains("Add verified responsibilities"));
      assertTrue(text.contains("Add a real system count"));
      assertTrue(text.indexOf("What’s working".replace('’', '\'')) < text.indexOf("Summary length"));
      assertTrue(text.contains("Experience and education have distinct headings."));
      assertTrue(text.contains("NEXT STEP"));
      assertTrue(text.contains("Page 1 of"));
    }
  }

  @Test
  void longFindingsContinueWithoutDroppingText() throws Exception {
    var mapper = new ObjectMapper();
    var review = mapper.createObjectNode();
    review.putArray("formatFindings").addObject().put("title", "Detailed review").put("status", "partial")
        .put("evidence", "Supported evidence for this finding. ".repeat(350) + " FINAL_EVIDENCE")
        .put("suggestion", "NEXT_STEP_END").put("requirement", "REQUIREMENT_END");
    try (var pdf = Loader.loadPDF(new FeedbackPdfService().create(review, "Senior"))) {
      String text = new PDFTextStripper().getText(pdf);
      assertTrue(pdf.getNumberOfPages() > 1);
      assertTrue(text.contains("FINAL_EVIDENCE"));
      assertTrue(text.contains("NEXT_STEP_END"));
      assertTrue(text.contains("REQUIREMENT_END"));
      assertTrue(text.contains("No passing checks were recorded"));
    }
  }

  @Test
  void renderOptionalPreviewFromSavedReview() throws Exception {
    String input = System.getProperty("feedback.preview.input");
    if (input == null) return;
    var review = new ObjectMapper().readTree(java.nio.file.Path.of(input).toFile());
    byte[] bytes = new FeedbackPdfService().create(review, "Senior");
    java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("feedback.preview.output")), bytes);
  }
}
