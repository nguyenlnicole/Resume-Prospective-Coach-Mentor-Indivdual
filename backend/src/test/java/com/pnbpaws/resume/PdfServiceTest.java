package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.*;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.junit.jupiter.api.*;

class PdfServiceTest {
  @Test
  void publicJordanReferenceFitsOnePage() throws Exception {
    var reference = new ObjectMapper().readTree(java.nio.file.Path.of("../frontend/src/reference-resume.json").toFile());
    var exported = service().extract(service().create(reference, "Senior", "Calibri", "right"));
    assertEquals(1, exported.pages());
    assertTrue(exported.text().contains("Jordan Avery"));
    assertTrue(exported.text().contains("Schedule Example"));
  }
  @Test
  void syntheticResumeFitsWithCurrentTypographyAndAllSections() throws Exception {
    var reference = new ObjectMapper().readTree(getClass().getResourceAsStream("/approved-reference.json"));
    byte[] bytes = service().create(reference, "Senior", "Calibri", "right");
    var extraction = service().extract(bytes);
    assertEquals(1, extraction.pages());
    for (String label : java.util.List.of("Reference Candidate", "EXPERIENCE", "TECHNICAL PROJECTS", "AI Tools and Techniques", "CERTIFICATIONS", "March 2023"))
      assertTrue(extraction.text().contains(label), label);
    assertTrue(extraction.typography().stream().anyMatch(t -> t.get("fontAndSize").toString().contains("23")));
    assertTrue(extraction.lines().stream().allMatch(l -> ((Number) l.get("leftPt")).floatValue() >= 43f && ((Number) l.get("rightPt")).floatValue() <= 569f));
    String output = System.getProperty("reference.pdf.output");
    if (output != null) java.nio.file.Files.write(java.nio.file.Path.of(output), bytes);
  }

  @Test
  void extractsMeasurableGeometryAndRenderedPageForFormatReview() throws Exception {
    var input = service().extract(service().create(resume(), "Mid-level"));
    assertEquals(1, input.pageImages().size());
    assertTrue(input.pageImages().get(0).startsWith("data:image/png;base64,"));
    assertEquals(612f, input.pageSizes().get(0).get("widthPt"));
    assertEquals(792f, input.pageSizes().get(0).get("heightPt"));
    assertFalse(input.lines().isEmpty());
    for (var line : input.lines()) {
      assertTrue(((Number) line.get("leftPt")).floatValue() >= 0);
      assertTrue(((Number) line.get("rightPt")).floatValue() <= 612);
      assertTrue(
          ((Number) line.get("baselinePt")).floatValue()
              > ((Number) line.get("topPt")).floatValue());
    }
  }

  @Test
  void extractsShortLinkTargetsAndDatePlacement() throws Exception {
    for (String placement : java.util.List.of("inline", "right")) {
      var input = service().extract(service().create(resume(), "Mid-level", "Calibri", placement));
      assertEquals(placement, input.datePlacement());
      assertTrue(
          input.links().stream()
              .anyMatch(link -> link.get("url").equals("https://github.com/example/caretrack")));
    }
  }

  ObjectNode resume() throws Exception {
    return (ObjectNode)
        new ObjectMapper().readTree(getClass().getResourceAsStream("/sample-resume.json"));
  }

  PdfService service() {
    return new PdfService(
        System.getProperty("pdf.font", "C:/Windows/Fonts/calibri.ttf"),
        System.getProperty("pdf.bold", "C:/Windows/Fonts/calibrib.ttf"));
  }

  @Test
  void producesOnePageWithClickableShortLinks() throws Exception {
    try (var pdf = Loader.loadPDF(service().create(resume(), "Mid-level"))) {
      assertEquals(1, pdf.getNumberOfPages());
      assertTrue(
          pdf.getPage(0).getAnnotations().stream()
              .anyMatch(
                  a ->
                      a instanceof PDAnnotationLink l
                          && l.getAction() instanceof PDActionURI u
                          && u.getURI().contains("github.com/example/caretrack")));
    }
  }

  @Test
  void projectLinkLabelsKeepBothClickableDestinations() throws Exception {
    ObjectNode r = resume();
    String[] projects = r.path("projects").asText().split("\\n\\s*\\n");
    projects[0] = "CareTrack | [Demo](https://example.com/caretrack) | [GitHub](https://github.com/example/caretrack)\n"
        + projects[0].substring(projects[0].indexOf('\n') + 1);
    r.put("projects", String.join("\n\n", projects));
    var extraction = service().extract(service().create(r, "Mid-level"));
    assertTrue(extraction.text().contains("CareTrack | Demo | GitHub"));
    assertTrue(extraction.links().stream().anyMatch(link -> link.get("url").equals("https://example.com/caretrack")));
    assertTrue(extraction.links().stream().anyMatch(link -> link.get("url").equals("https://github.com/example/caretrack")));
    assertTrue(extraction.links().stream().anyMatch(link -> link.get("context").toString().contains("CareTrack")));
  }

  @Test
  void overflowingContentContinuesWithoutDroppingText() throws Exception {
    ObjectNode r = resume();
    r.put("experience", r.path("experience").asText().repeat(8));
    try (var document = Loader.loadPDF(service().create(r, "Mid-level"))) {
      assertTrue(document.getNumberOfPages() > 1);
      String text = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
      assertTrue(text.contains("EDUCATION"));
    }
  }

  @Test
  void juniorProjectCountIsOnlyRecommended() throws Exception {
    ObjectNode r = resume();
    r.put("projects", "An app without a repo link");
    assertDoesNotThrow(() -> PdfService.validate(r, "Entry / Junior"));
  }

  @Test
  void seniorProjectLinksAreOptional() throws Exception {
    ObjectNode r = resume();
    r.put("projects", "Portfolio app | https://example.com/demo\nTech: Java\nBuilt a portfolio app.");
    assertDoesNotThrow(() -> PdfService.validate(r, "Senior"));
    r.put("projects", "Portfolio app\nTech: Java\nBuilt a portfolio app.");
    assertDoesNotThrow(() -> PdfService.validate(r, "Senior"));
    r.put("projects", "Portfolio app | https://github.com/example/portfolio\nTech: Java\nBuilt a portfolio app.");
    assertDoesNotThrow(() -> PdfService.validate(r, "Senior"));
  }

  @Test
  void singleRoleUsesOneHeadingWhileMultipleRolesKeepCompanyAboveThem() throws Exception {
    String source = "Flex\nSoftware Engineer, Inference | Jul 2024–Present\n• Built one service."
        + "\n\nGenix\nFull Stack Engineer Intern | Aug 2023–Feb 2024\n• Built another service.";
    String shown = PdfService.formattedExperience(source);
    assertTrue(shown.contains("Flex — Software Engineer, Inference | Jul 2024–Present"));
    assertFalse(shown.contains("Flex\nSoftware Engineer"));
    String multi = "Example Consulting\nEngineer II | 2020–2022\n• First role.\nEngineer | 2018–2020\n• Second role.";
    assertTrue(PdfService.formattedExperience(multi).startsWith("Example Consulting\nEngineer II"));
  }

  @Test
  void educationDatesFollowGraduationStatus() throws Exception {
    ObjectNode r = resume();
    r.put("education", "Example University — B.S. | Graduated May 2021\n\n"
        + "Example College — M.S. | Expected December 2099\n\n"
        + "Example Institute — Coursework | In progress\n\n"
        + "Example School — Coursework | Not completed");
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
    r.put("education", "Example University — B.S. | Graduated\n\nExample College — M.S.");
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
    r.put("education", "Example University — B.S. | Graduated 2021");
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
    r.put("education", "Example University — B.S. | Graduated December 2099");
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
  }

  @Test
  void summaryLengthDoesNotBlockExport() throws Exception {
    ObjectNode r = resume();
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
    r.put("summary", "I build applications. I test software.");
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
  }

  @Test
  void longProjectDescriptionsDoNotBlockExport() throws Exception {
    ObjectNode r = resume();
    String project = "CareTrack | https://github.com/example/caretrack\nTech: Java, React\n"
        + "built ".repeat(46);
    r.put("projects", project + "\n\n" + String.join("\n\n",
        java.util.Arrays.stream(r.path("projects").asText().split("\\n\\s*\\n")).skip(1).toList()));
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
  }

  @Test
  void experienceBulletsFollowNewestAndOldestRanges() throws Exception {
    ObjectNode shortBullet = resume();
    shortBullet.put("experience", shortBullet.path("experience").asText() + "\n• Too short.");
    assertDoesNotThrow(() -> PdfService.validate(shortBullet, "Mid-level"));
    ObjectNode extraBullet = resume();
    String valid = "• Coordinated release review with the product team and documented implementation decisions before the planned deployment of the customer account feature.";
    extraBullet.put("experience", extraBullet.path("experience").asText().replace("\n\nJuniper", "\n" + valid + "\n\nJuniper"));
    assertDoesNotThrow(() -> PdfService.validate(extraBullet, "Mid-level"));
    extraBullet.put("experience", extraBullet.path("experience").asText().replace("\n\nJuniper", "\n" + valid + "\n\nJuniper"));
    assertDoesNotThrow(() -> PdfService.validate(extraBullet, "Mid-level"));
  }

  @Test
  void fewerExperienceBulletsDoNotBlockExport() throws Exception {
    for (int count = 0; count < 3; count++) {
      ObjectNode shortExperience = resume();
      shortExperience.put("experience", "Example Company — Engineer | 2023–Present"
          + "\n• Built supported API features with the product team and documented release checks for the planned customer account deployment.".repeat(count));
      assertDoesNotThrow(() -> PdfService.validate(shortExperience, "Mid-level"));
    }
  }

  @Test
  void projectLabelsRetainSeparateDemoAndRepositoryDestinations() throws Exception {
    ObjectNode input = resume();
    input.put("projects", "[Pet Shelter Web Page](https://pyk006.github.io/TEVue/) | [Pet Shelter GitHub Repo](https://github.com/example/PetShelterWebpage) | [Pet Shelter GitHub Repo 2](https://github.com/example/PetShelterPage) — Java, PostgreSQL\nBuilt a pet adoption application.");
    var extracted = service().extract(service().create(input, "Senior", "Calibri", "right"));
    assertTrue(extracted.text().contains("Pet Shelter Web Page"));
    assertFalse(extracted.text().contains("Web Page Web Page"));
    assertTrue(extracted.text().contains("GitHub Repo 2"));
    for (String url : java.util.List.of("https://pyk006.github.io/TEVue/", "https://github.com/example/PetShelterWebpage", "https://github.com/example/PetShelterPage"))
      assertTrue(extracted.links().stream().anyMatch(link -> url.equals(link.get("url"))), url);
  }

  @Test
  void cannotExportUnsafeLinks() throws Exception {
    ObjectNode r = resume();
    ((ObjectNode) r.path("contact")).put("linkedin", "javascript:alert(1)");
    assertDoesNotThrow(() -> PdfService.validate(r, "Senior"));
  }

  @Test
  void requiresThreeProjectsForJuniorAndMid() throws Exception {
    for (String level : java.util.List.of("Entry / Junior", "Mid-level")) {
      ObjectNode r = resume();
      String[] projects = r.path("projects").asText().split("\n\n");
      for (int count : new int[] {0, 1, 2, 4}) {
        String text =
            java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> projects[i % 3])
                .collect(java.util.stream.Collectors.joining("\n\n"));
        r.put("projects", text);
        assertDoesNotThrow(() -> PdfService.validate(r, level));
      }
      assertDoesNotThrow(() -> PdfService.validate(resume(), level));
    }
  }

  @Test
  void keepsReviewAndBuildAtTheSameLevel() {
    for (String level : ApiController.LEVELS)
      assertEquals(level, ApiController.buildLevel(level, level));
    assertThrows(Exception.class, () -> ApiController.buildLevel("Senior", "Entry / Junior"));
    assertThrows(Exception.class, () -> ApiController.buildLevel("Entry / Junior", null));
  }

  @Test
  void incompleteContactHeaderDoesNotBlockExport() throws Exception {
    for (String key : java.util.List.of("location")) {
      ObjectNode r = resume();
      ((ObjectNode) r.path("contact")).put(key, "");
      assertDoesNotThrow(() -> PdfService.validate(r, "Senior"));
    }
    ObjectNode withoutPortfolio = resume();
    ((ObjectNode) withoutPortfolio.path("contact")).put("portfolio", "");
    assertDoesNotThrow(() -> PdfService.validate(withoutPortfolio, "Senior"));
    ((ObjectNode) withoutPortfolio.path("contact")).put("github", "");
    assertDoesNotThrow(() -> PdfService.validate(withoutPortfolio, "Senior"));
    ((ObjectNode) withoutPortfolio.path("contact")).put("linkedin", "");
    assertDoesNotThrow(() -> PdfService.validate(withoutPortfolio, "Senior"));
  }

  @TestFactory
  java.util.List<DynamicTest> availableFontsProduceAOnePagePdf() {
    return PdfService.FONTS.stream().map(font -> DynamicTest.dynamicTest(font, () -> {
      byte[] bytes;
      try { bytes = service().create(resume(), "Mid-level", font, "right"); }
      catch (ResponseStatusException e) {
        if (e.getStatusCode().value() == 503 && e.getReason().contains("font needs configuring")) {
          Assumptions.abort("Install the licensed " + font + " font to test this option.");
          return;
        }
        throw e;
      }
      try (var doc = Loader.loadPDF(bytes)) {
        assertEquals(1, doc.getNumberOfPages(), font);
        assertTrue(new org.apache.pdfbox.text.PDFTextStripper().getText(doc).contains("San Francisco, CA"));
      }
    })).toList();
  }

  @Test
  void rejectsUnapprovedFontsAndDatePlacements() throws Exception {
    assertThrows(
        Exception.class, () -> service().create(resume(), "Mid-level", "Comic Sans", "inline"));
    assertThrows(Exception.class, () -> service().create(resume(), "Mid-level", "Arial", "middle"));
    assertThrows(Exception.class, () -> service().create(resume(), "Mid-level", "Arial", "right", "red"));
    assertDoesNotThrow(() -> service().create(resume(), "Mid-level", "Arial", "right", "dark"));
  }
  byte[] document(int count, boolean locked) throws Exception {
    try (var pdf = new PDDocument(); var out = new ByteArrayOutputStream()) {
      for (int i = 0; i < count; i++) {
        var page = new PDPage(i % 2 == 0 ? PDRectangle.LETTER : new PDRectangle(792, 612));
        pdf.addPage(page);
        try (var content = new PDPageContentStream(pdf, page)) {
          content.setNonStrokingColor(new java.awt.Color((i + 1) * 20, 80, 150));
          content.addRect(20, 20, 50, 50);
          content.fill();
        }
      }
      if (locked) pdf.protect(new StandardProtectionPolicy("owner", "password", new AccessPermission()));
      pdf.save(out);
      return out.toByteArray();
    }
  }

  @Test void previewsEveryPageInOrderAndPreservesOrientation() throws Exception {
    for (int count : new int[]{1, 2, 3, 10}) {
      var pages = service().uploadPreview(document(count, false));
      assertEquals(count, pages.size());
      for (int i = 0; i < count; i++) {
        var image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(pages.get(i).split(",", 2)[1])));
        assertTrue(Math.max(image.getWidth(), image.getHeight()) <= 1100);
        assertEquals(i % 2 == 0, image.getHeight() > image.getWidth());
        if (i > 0) assertNotEquals(pages.get(i - 1), pages.get(i));
      }
    }
  }

  @Test void rejectsLockedMalformedAndOverlongPdfs() throws Exception {
    assertThrows(ResponseStatusException.class, () -> service().uploadPreview(document(1, true)));
    assertThrows(ResponseStatusException.class, () -> service().uploadPreview(document(11, false)));
    assertThrows(ResponseStatusException.class, () -> service().uploadPreview("%PDF-broken".getBytes()));
    assertThrows(ResponseStatusException.class, () -> service().uploadPreview(new byte[0]));
  }

  @Test
  void codingChoiceControlsExportRequirements() throws Exception {
    ObjectNode r = resume();
    r.put("softwareEngineering", false);
    ((ObjectNode) r.path("contact")).put("github", "");
    r.put("projects", "");
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
    r.put("softwareEngineering", true);
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
    r.put("softwareEngineering", false);
    r.put("projects", "Security lab\n• Investigated authentication events in a training environment.");
    assertDoesNotThrow(() -> PdfService.validate(r, "Mid-level"));
  }
}
