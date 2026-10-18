package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
class FeedbackPdfService {
  private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
  private static final Color INK = new Color(35, 53, 85), MUTED = new Color(91, 114, 148),
      LINE = new Color(209, 222, 239), BLUE = new Color(239, 245, 252), GREEN = new Color(240, 248, 245),
      LIGHT_RED = new Color(255, 240, 240);

  byte[] create(JsonNode review, String level) {
    try (var document = new PDDocument()) {
      var page = new PageWriter(document);
      page.write("Your resume feedback", 18, true, 24);
      page.write("Career level: " + level, 9, false, 12);
      page.scores(score(review.path("formatRating")), score(review.path("overallRating")));
      page.write("The scores reflect the uploaded resume. Unverified details have not earned points.",
          9, false, 19);
      if (!review.path("scoringNote").asText().isBlank()) page.write(review.path("scoringNote").asText(), 9, false, 13);
      page.section("POSITIVE FEEDBACK", "What’s working", "Strengths supported by your resume review.");
      var strengths = new ArrayList<JsonNode>();
      for (String group : List.of("formatFindings", "contentFindings")) {
        for (JsonNode finding : review.path(group)) {
          if (!"pass".equals(finding.path("status").asText())) continue;
          strengths.add(finding);
        }
      }
      for (JsonNode strength : mergedStrengths(strengths)) page.card("POSITIVE FEEDBACK", strength, true);
      if (strengths.isEmpty()) page.write("No passing checks were recorded. The next sections explain where to start.", 10, false, 15);
      section(page, "YOUR FIRST REVIEW", "A closer look at your presentation", "Your layout and readability.", review.path("formatFindings"));
      section(page, "YOUR SECOND REVIEW", "Your experience, in perspective", "Your " + level + " resume review.", review.path("contentFindings"));
      page.finish();
      var output = new ByteArrayOutputStream();
      document.save(output);
      return output.toByteArray();
    } catch (IOException e) {
      throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "The feedback PDF could not be created.");
    }
  }

  private static void section(PageWriter page, String eyebrow, String heading, String subtitle, JsonNode findings) throws IOException {
    page.section(eyebrow, heading, subtitle);
    boolean any = false;
    for (JsonNode finding : findings) {
      if ("pass".equals(finding.path("status").asText())) continue;
      any = true;
      page.card("unverified".equals(finding.path("status").asText()) ? "NEEDS VERIFICATION" : "ROOM TO IMPROVE", finding, false);
    }
    if (!any) page.write("No improvements were flagged in this section.", 9, false, 14);
  }

  static List<JsonNode> mergedStrengths(List<JsonNode> strengths) {
    var merged = new ArrayList<JsonNode>();
    int count = Math.min(strengths.size(), Math.max(4, Math.min(6, (strengths.size() + 2) / 3)));
    for (int group = 0; group < count; group++) {
      int start = group * strengths.size() / count, end = (group + 1) * strengths.size() / count;
      var sentences = new ArrayList<String>();
      var titles = new ArrayList<String>();
      for (int i = start; i < end; i++) {
        JsonNode finding = strengths.get(i);
        titles.add(finding.path("title").asText());
        String evidence = finding.path("evidence").asText().strip();
        if (!evidence.isEmpty()) sentences.add(evidence);
      }

      var text = new ArrayList<String>();
      for (String evidence : sentences) {
        var boundary = java.text.BreakIterator.getSentenceInstance(java.util.Locale.US);
        boundary.setText(evidence);
        int firstEnd = boundary.next();
        String sentence = (firstEnd == java.text.BreakIterator.DONE ? evidence : evidence.substring(0, firstEnd)).strip();
        if (!sentence.matches(".*[.!?]$")) sentence += ".";
        if (text.size() < 4) text.add(sentence);
      }
      if (text.size() == 2) text.add("The review marked both " + String.join(" and ", titles).toLowerCase(java.util.Locale.ROOT) + " as supported.");
      var card = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
      card.put("title", titles.size() == 1 ? titles.get(0) : titles.get(0) + " & related strengths");
      card.put("evidence", String.join(" ", text));
      merged.add(card);
    }
    return merged;
  }

  private static String score(JsonNode rating) {
    if (rating.path("value").isNumber()) return rating.path("value").asText();
    return "unavailable";
  }

  private static String printable(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replaceAll("\\p{M}", "")
        .replace('\u2018', '\'').replace('\u2019', '\'')
        .replace('\u201c', '"').replace('\u201d', '"')
        .replace('\u2014', '–')
        .replaceAll("[^\\x20-\\x7E–]", " ").replaceAll("\\s+", " ").strip();
  }

  private static final class PageWriter implements AutoCloseable {
    private static final float SCALE = .7f;
    private final PDDocument document;
    private PDPageContentStream stream;
    private float y;
    private String sectionName = "Your resume feedback";

    PageWriter(PDDocument document) throws IOException {
      this.document = document;
      newPage();
    }

    private void newPage() throws IOException {
      if (stream != null) stream.close();
      var page = new PDPage(PDRectangle.LETTER);
      document.addPage(page);
      stream = new PDPageContentStream(document, page);
      text("RESUME PROSPECTIVE", 48, 757, 10, true, MUTED);
      y = 724;
    }

    void space(float amount) { y -= amount * SCALE; }

    void text(String value, float x, float baseline, int size, boolean bold, Color color) throws IOException {
      stream.beginText(); stream.setFont(bold ? BOLD : REGULAR, size * SCALE); stream.setNonStrokingColor(color);
      stream.newLineAtOffset(x, baseline); stream.showText(printable(value)); stream.endText();
    }

    void box(float x, float top, float width, float height, Color fill) throws IOException {
      stream.setNonStrokingColor(fill); stream.setStrokingColor(LINE); stream.setLineWidth(.6f);
      stream.addRect(x, top - height, width, height); stream.fillAndStroke();
    }

    void scores(String format, String overall) throws IOException {
      y -= 8 * SCALE;
      box(48, y, 252, 35 * SCALE, BLUE); box(312, y, 252, 35 * SCALE, BLUE);
      text("FORMAT & PRESENTATION", 54, y - 11 * SCALE, 7, true, MUTED);
      text("OVERALL RESUME", 318, y - 11 * SCALE, 7, true, MUTED);
      text("Presentation: " + format + " / 10", 54, y - 26 * SCALE, 11, true, INK);
      text("Overall: " + overall + " / 10", 318, y - 26 * SCALE, 11, true, INK);
      y -= 47 * SCALE;
    }

    void section(String eyebrow, String heading, String subtitle) throws IOException {
      flushCards();
      if (y < 140) newPage();
      sectionName = heading;
      space(3); write(eyebrow, 7, true, 15); write(heading, 16, true, 20);
      write(subtitle, 9, false, 12); space(3);
    }

    private record Row(String value, int size, boolean bold, int leading) {}
    void rows(List<Row> rows, String value, int size, boolean bold, int leading) throws IOException {
      for (String line : wrap(printable(value), bold ? BOLD : REGULAR, size, 236)) rows.add(new Row(line, size, bold, leading));
    }

    private record Card(List<Row> lines, boolean positive) {}
    private Card pending;

    void flushCards() throws IOException {
      if (pending != null) { drawCards(pending, null); pending = null; }
    }

    void card(String label, JsonNode finding, boolean positive) throws IOException {
      var lines = new ArrayList<Row>();
      rows(lines, label, 7, true, 11);
      rows(lines, finding.path("title").asText(), 10, true, 14);
      rows(lines, finding.path("evidence").asText(), 9, false, 11);
      if (!positive && !finding.path("suggestion").asText().isBlank()) {
        lines.add(new Row("", 9, false, 2)); rows(lines, "NEXT STEP", 7, true, 11);
        rows(lines, finding.path("suggestion").asText(), 9, false, 11);
      }
      if (!positive && !finding.path("requirement").asText().isBlank()) {
        lines.add(new Row("", 9, false, 2)); rows(lines, "WHAT THIS CHECK LOOKS FOR", 7, true, 11);
        rows(lines, finding.path("requirement").asText(), 8, false, 10);
      }
      var card = new Card(lines, positive);
      if (pending == null) pending = card;
      else { drawCards(pending, card); pending = null; }
    }

    void drawCards(Card leftCard, Card rightCard) throws IOException {
      Card[] cards = {leftCard, rightCard};
      int[] offsets = {0, 0};
      float total = 0;
      for (Card card : cards) if (card != null)
        total = Math.max(total, 12 + (float) card.lines().stream().mapToDouble(r -> r.leading() * SCALE).sum());
      if (y - total < 58 && total < 620 && y < 690) continuedPage();
      while (offsets[0] < leftCard.lines().size() || (rightCard != null && offsets[1] < rightCard.lines().size())) {
        if (y < 88) continuedPage();
        int[] ends = offsets.clone();
        float height = 12;
        for (int c = 0; c < 2; c++) {
          if (cards[c] == null) continue;
          float used = 12;
          var rows = cards[c].lines();
          while (ends[c] < rows.size() && y - used - rows.get(ends[c]).leading() * SCALE >= 58)
            used += rows.get(ends[c]++).leading() * SCALE;
          height = Math.max(height, used);
        }
        for (int c = 0; c < 2; c++) {
          if (cards[c] == null || ends[c] == offsets[c]) continue;
          float x = 48 + c * 264;
          box(x, y, 252, height, cards[c].positive() ? GREEN : LIGHT_RED);
          float baseline = y - 12;
          for (int i = offsets[c]; i < ends[c]; i++) {
            Row row = cards[c].lines().get(i);
            text(row.value(), x + 8, baseline, row.size(), row.bold(), INK);
            baseline -= row.leading() * SCALE;
          }
          offsets[c] = ends[c];
        }
        y -= height + 9;
      }
    }

    void continuedPage() throws IOException {
      newPage(); write(sectionName + " (continued)", 11, true, 20);
    }

    void finish() throws IOException {
      flushCards();
      close();
      for (int i = 0; i < document.getNumberOfPages(); i++) {
        try (var footer = new PDPageContentStream(document, document.getPage(i), PDPageContentStream.AppendMode.APPEND, true, true)) {
          footer.setStrokingColor(LINE); footer.moveTo(48, 42); footer.lineTo(564, 42); footer.stroke();
          footer.beginText(); footer.setFont(REGULAR, 8 * SCALE); footer.setNonStrokingColor(MUTED); footer.newLineAtOffset(48, 28);
          footer.showText("Resume Prospective  |  Your uploaded resume review"); footer.endText();
          footer.beginText(); footer.setFont(REGULAR, 8 * SCALE); footer.newLineAtOffset(510, 28);
          footer.showText("Page " + (i + 1) + " of " + document.getNumberOfPages()); footer.endText();
        }
      }
    }

    void write(String text, int size, boolean bold, int leading) throws IOException {
      var font = bold ? BOLD : REGULAR;
      for (String line : wrap(printable(text), font, size, 516)) {
        if (y < 58) newPage();
        text(line, 48, y, size, bold, bold ? INK : MUTED);
        y -= leading * SCALE;
      }
    }

    private static List<String> wrap(String text, PDType1Font font, int size, float width)
        throws IOException {
      var lines = new ArrayList<String>();
      var line = new StringBuilder();
      for (String word : text.split(" ")) {
        if (word.isEmpty()) continue;
        String candidate = line.isEmpty() ? word : line + " " + word;
        if (!line.isEmpty() && font.getStringWidth(candidate) * size * SCALE / 1000 > width) {
          lines.add(line.toString());
          line.setLength(0);
        }
        if (!line.isEmpty()) line.append(' ');
        for (char character : word.toCharArray()) {
          if (font.getStringWidth(line.toString() + character) * size * SCALE / 1000 > width) {
            lines.add(line.toString()); line.setLength(0);
          }
          line.append(character);
        }
      }
      if (!line.isEmpty()) lines.add(line.toString());
      if (lines.isEmpty()) lines.add("");
      return lines;
    }

    @Override public void close() throws IOException { if (stream != null) { stream.close(); stream = null; } }
  }
}
