package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

class ResumeScoring {
  static final String VERSION = "pnb-tech-4";
  static final String POLICY =
      "PNB rubric: 10 equally weighted checks per review. Overall = 40% presentation + 60%"
          + " career-level content. Met = 1 point, partly met = 0.5, not met = 0. Unverified checks"
          + " earn no points until verified; scores remain provisional while checks are unverified. A rating of 10 requires every"
          + " applicable check to be met. This is our rubric, not a hiring guarantee.";

  record Criterion(String id, String title, String requirement) {}

  static List<Criterion> format(String level) {
    if (!ResumeProfiles.technical(level)) return ResumeProfiles.format(level);
    return List.of(
        new Criterion(
            "contact",
            "Complete contact information",
            "First and last name, city/state or region, phone, and professional email."
                + " Recommend LinkedIn, GitHub for coding work, and a portfolio when available, but do not deduct contact points for missing profile links. Short clickable labels; header label exactly GitHub. No"
                + " street address. Evaluate presence and presentation, not employer or personal"
                + " prestige."),
        new Criterion(
            "header",
            "Centered name and contact header",
            "Bold centered full name above a centered contact row. Check line coordinates and the"
                + " rendered page."),
        new Criterion(
            "summary",
            "Summary length",
            "Present, 2–4 sentences and 35–45 words. Do not assess the"
                + " quality of its claims in this review."),
        new Criterion(
            "sections",
            "Sections and their order",
            "Contact, summary, experience, grouped technical skills, projects, education,"
                + " certifications and training when applicable. Education never at top. Projects"
                + " are optional outside software engineering/coding work and for Senior and Management. Management must have a certifications"
                + " section; other levels need it only if credentials are supplied."),
        new Criterion(
            "typography",
            "Fonts, sizes and emphasis",
            "Approved PNB-Resume-Prospective reference style: Calibri; body 10pt, centered name"
                + " 23pt bold, contact 10pt, section headings 10pt bold and underlined. Other"
                + " approved fonts: Arial, Aptos, Cambria, Georgia, Times New Roman. Bold job"
                + " headings and skill category labels. Consistent readable colors. Use measured"
                + " font runs; unknown font identification is unverified."),
        new Criterion(
            "margins",
            "Page margins",
            "Approved reference uses 0.6 inch side margins and 0.5 inch top/bottom margins."
                + " Visible content must stay inside those bounds; a short line or extra blank"
                + " space below the last section is not a margin fault. Cite measured coordinates"
                + " in points (72 per inch). PDF does not expose authoring margins; if geometry"
                + " cannot establish bounds, use unverified."),
        new Criterion(
            "spacing",
            "Line, bullet and section spacing",
            "Approved reference uses 12pt body line spacing, 0–2pt after bullets, 6pt before"
                + " sections and 3pt after section headings. Allow equivalent readable spacing"
                + " without overlaps. Indent bullet continuations. Use adjacent baselines and"
                + " font sizes; never infer exact spacing from screenshots alone. Missing usable"
                + " measurements means unverified."),
        new Criterion(
            "layout",
            "One-page professional layout",
            "Exactly one page. Single main column, no photo, sidebar, skill bars or timeline."
                + " Grouped skills in columns are allowed. Optional thin divider lines. No overlap"
                + " or clipped text. A photo is a format issue, not a judgment of the person."),
        new Criterion(
            "skills",
            "Grouped technical skills",
            "A clearly labeled technical skills section with organized hard-skill categories in"
                + " rows or columns. Check grouping and readability only, not technical strength or"
                + " keyword quantity."),
        new Criterion(
            "projects",
            "Project presentation and viewable links",
            List.of("Entry / Junior", "Mid-level").contains(level)
                ? "For software engineering or coding roles, at least three distinct project entries (more are welcome) with verified technologies. A clickable repository or demo link for each project strengthens this check; missing both lowers the rating but never blocks a draft. For other technical fields, projects and links are optional; omission meets this check."
                : "Projects are optional. Omission fully meets this check; if included for coding work, show verified technologies and a clickable repository or demo link where available. Missing both links lowers this check, but never blocks a draft. Other technical fields do not require project links."));
  }

  static List<Criterion> content(String level) {
    if (!ResumeProfiles.technical(level)) return ResumeProfiles.content(level);
    return switch (level) {
      case "Entry / Junior" ->
          List.of(
              new Criterion(
                  "roles",
                  "Junior experience requirements",
                  "At most three professional roles; at least two relevant roles have truthful"
                      + " Intern, Apprentice, Co-op, Student Engineer, Assistant, Junior,"
                      + " Entry-Level or semantically similar titles. A third role may be"
                      + " nontechnical. Assess actual role context, not words alone. Never suggest"
                      + " inventing or renaming an official title."),
              new Criterion(
                  "practice",
                  "Hands-on technical work",
                  "At least two relevant experience entries describe junior technical"
                      + " contributions: building features/prototypes, data preparation/models, or"
                      + " supervised security work. Equivalent responsibilities count; do not"
                      + " require all technical fields."),
              new Criterion(
                  "contribution",
                  "Clear personal contribution",
                  "Explains what the applicant actually built, tested, analyzed or assisted with,"
                      + " and its purpose."),
              new Criterion(
                  "foundations",
                  "Tools and technical foundations",
                  "Relevant technologies connected to specific tasks, with credible foundational"
                      + " understanding."),
              new Criterion(
                  "troubleshooting",
                  "Problem solving",
                  "Reproduces issues, investigates errors or data quality, and describes fixes or"
                      + " findings appropriate to a junior."),
              new Criterion(
                  "validation",
                  "Testing and verification",
                  "Tests features, separates training/test data, evaluates AI outputs, validates"
                      + " security fixes or equivalent task-appropriate verification."),
              new Criterion(
                  "collaboration",
                  "Learning and collaboration",
                  "Incorporates feedback, works under supervision where appropriate, contributes"
                      + " through reviews/tickets or communicates findings."),
              new Criterion(
                  "documentation",
                  "Documentation and reproducibility",
                  "Setup notes, experiment records, support tickets, guides, or other useful"
                      + " knowledge sharing."),
              new Criterion(
                  "outcomes",
                  "Specific outcomes",
                  "Clear purpose and result at junior scope. Numbers are welcome only if supported,"
                      + " never mandatory for every bullet."),
              new Criterion(
                  "projects",
                  "Relevant practical work",
                  "For software engineering/coding work, at least three genuine projects (more are welcome), identifying technical"
                      + " approach and applicant contribution; working links are not proof of"
                      + " repository quality or authorship. For other technical fields, relevant labs or practical work can demonstrate skills; projects and repositories are optional."));
      case "Mid-level" ->
          criteria(
              new String[][] {
                {
                  "ownership",
                  "Independent ownership",
                  "Owns a feature, workflow or technical deliverable with appropriate independence."
                },
                {
                  "delivery",
                  "Delivery through verification",
                  "Contributes from design or planning through implementation, release and"
                      + " validation."
                },
                {
                  "problems",
                  "Nonroutine problem solving",
                  "Resolves nontrivial issues through diagnosis or root-cause analysis and lasting"
                      + " fixes."
                },
                {
                  "judgment",
                  "Technical decisions",
                  "Explains appropriate design choices or tradeoffs for the relevant technical"
                      + " field."
                },
                {
                  "quality",
                  "Operational quality",
                  "Improves reliability, performance, security, accessibility, cost or efficiency."
                },
                {
                  "validation",
                  "Testing and monitoring",
                  "Uses tests, monitoring, model evaluation or security validation suited to the"
                      + " work."
                },
                {
                  "collaboration",
                  "Dependencies and collaboration",
                  "Coordinates technical tasks, cross-team dependencies or actionable reviews."
                },
                {
                  "knowledge",
                  "Knowledge sharing",
                  "Useful reviews, documentation, onboarding or informal mentoring; management is"
                      + " not required."
                },
                {
                  "outcomes",
                  "Scope and outcomes",
                  "Specific credible results and scope; quantitative or concrete qualitative"
                      + " outcomes count."
                },
                {
                  "projects",
                  "Relevant practical work",
                  "For software engineering/coding work, at least three genuine projects (more are welcome) with clear implementation"
                      + " contributions. For other technical fields, relevant practical work or labs may demonstrate skills; do not require coding projects or repository links."
                }
              });
      case "Senior" ->
          criteria(
              new String[][] {
                {
                  "ownership",
                  "Complex technical ownership",
                  "Owns complex systems or work spanning multiple components or teams."
                },
                {
                  "ambiguity",
                  "Handling ambiguity",
                  "Turns ambiguous problems into workable technical plans."
                },
                {
                  "architecture",
                  "Architecture and long-term decisions",
                  "Makes consequential design decisions with long-term maintainability in mind."
                },
                {
                  "tradeoffs",
                  "Technical and business tradeoffs",
                  "Balances scalability, reliability, security, cost, delivery and business needs"
                      + " as applicable."
                },
                {
                  "standards",
                  "Shared standards",
                  "Creates or improves practices adopted beyond a single task."
                },
                {
                  "systemic",
                  "Systemic improvements",
                  "Addresses recurring or structural issues, not only isolated bugs."
                },
                {
                  "leadership",
                  "Technical leadership",
                  "Leads complex initiatives, investigations or incidents; people management not"
                      + " required."
                },
                {
                  "mentoring",
                  "Mentoring and knowledge sharing",
                  "Develops others through mentoring, design reviews, guidance or reusable"
                      + " knowledge."
                },
                {
                  "influence",
                  "Cross-team influence",
                  "Aligns stakeholders or teams around technical decisions and dependencies."
                },
                {
                  "impact",
                  "Sustained broader impact",
                  "Demonstrates credible outcomes at broader scope; no fixed years threshold or"
                      + " mandatory projects."
                }
              });
      case "Management" ->
          criteria(
              new String[][] {
                {
                  "certifications",
                  "At least two certifications",
                  "Two distinct professional certifications with identifiable issuers, not"
                      + " bootcamps, attendance, training-only courses or duplicate credentials."
                      + " Cite each separately in sourceQuotes. Never invent a credential; do not"
                      + " claim external verification."
                },
                {
                  "people",
                  "People management",
                  "Manages teams with clear responsibility for people and team delivery, beyond a"
                      + " management title alone."
                },
                {
                  "roadmaps",
                  "Business-aligned priorities",
                  "Connects team roadmaps and priorities to organizational goals."
                },
                {
                  "capacity",
                  "Capacity and delivery planning",
                  "Plans staffing, capacity, milestones or resource allocation."
                },
                {
                  "coaching",
                  "Coaching and development",
                  "Provides coaching, feedback, career development or performance reviews."
                },
                {
                  "coordination",
                  "Cross-team delivery",
                  "Resolves dependencies and blockers across teams or partners."
                },
                {
                  "accountability",
                  "Quality and risk accountability",
                  "Establishes ownership for reliability, security, quality or incident"
                      + " follow-through."
                },
                {
                  "communication",
                  "Stakeholder communication",
                  "Communicates progress, risks, operational results and resource needs."
                },
                {
                  "resources",
                  "Sustainable team operations",
                  "Improves hiring, budgets, vendor relationships, onboarding or operating"
                      + " practices as relevant to the role; not all are mandatory."
                },
                {
                  "outcomes",
                  "Team outcomes",
                  "Shows specific team or organizational results with credible scope, not only"
                      + " individual implementation."
                }
              });
      default -> throw new IllegalArgumentException("Unknown career level");
    };
  }

  private static List<Criterion> criteria(String[][] rows) {
    return Arrays.stream(rows).map(r -> new Criterion(r[0], r[1], r[2])).toList();
  }

  static void apply(
      ObjectMapper mapper, ObjectNode result, PdfService.Extraction input, String level) {
    ArrayNode format = findings(mapper, result.path("formatChecks"), format(level), input, false);
    ArrayNode content = findings(mapper, result.path("contentChecks"), content(level), input, true);
    if (input.pages() > 0 && input.pages() != 1
        && !(input.pages() <= 3 && ResumeProfiles.physicianCv(level, result.path("resume"))))
      override(
          format,
          "layout",
          "missing",
          "The PDF contains " + input.pages() + " pages.",
          "Condense the resume to one readable page without inventing facts or shrinking below the"
              + " allowed sizes.");
    JsonNode resume = result.path("resume");
    String projects = resume.path("projects").asText();
    if (!projects.isBlank() && Arrays.stream(projects.split("\\n\\s*\\n"))
        .anyMatch(project -> project.matches("(?is).*\\b(?:Tech|Technologies|Stack|Languages)\\s*:.*")
            || project.lines().findFirst().orElse("").matches(".*\\s[—–]\\s+.+"))) {
      boolean missingProjectLink = Arrays.stream(projects.split("\\n\\s*\\n"))
          .anyMatch(project -> !Pattern.compile("https?://", Pattern.CASE_INSENSITIVE)
              .matcher(project.lines().findFirst().orElse("")).find());
      if (missingProjectLink)
        for (JsonNode finding : format)
          if (finding.path("id").asText().equals("projects")
              && finding.path("assessment").asText().equals("pass"))
            setStatus((ObjectNode) finding, "partial",
                "At least one coding project has no repository or demo link.",
                "Add a working project link when available, or keep the project without one.");
    }
    String summary = resume.path("summary").asText().strip();
    if (!summary.isBlank()
        && normalized(input.text()).contains(normalized(summary))) {
      int words = summary.split("\\s+").length;
      long sentences = ResumeText.sentences(summary);
      boolean meets = words >= 35 && words <= 45 && sentences >= 2 && sentences <= 4;
      override(
          format,
          "summary",
          meets ? "pass" : "missing",
          words + " words and " + sentences + " sentences in the extracted summary.",
          meets
              ? "Keep the summary within these limits."
              : "Use 2–4 sentences and 35–45 words.");
    }
    if (level.equals("Management")) {
      var cert = (ObjectNode) content.get(0);
      Set<String> distinctQuotes = new HashSet<>();
      cert.path("sourceQuotes").forEach(quote -> distinctQuotes.add(normalized(quote.asText())));
      long quotes = distinctQuotes.size();
      if (cert.path("assessment").asText().equals("pass") && quotes < 2)
        setStatus(
            cert,
            "unverified",
            "Two distinct certification entries were not supported by separate source evidence.",
            "Confirm the names and issuers of both certifications; training attendance does not"
                + " count.");
    }
    result.set("formatFindings", format);
    result.set("contentFindings", content);
    ObjectNode f = score(mapper, format, null);
    ObjectNode overall = score(mapper, format, content);
    result.set("formatRating", f);
    result.set("overallRating", overall);
    result.set("formatScore", f.path("value"));
    result.set("overallScore", overall.path("value"));
    result.put("scoringVersion", VERSION);
    result.put("scoringNote", POLICY);
    result.remove(List.of("formatChecks", "contentChecks"));
  }

  private static ArrayNode findings(
      ObjectMapper mapper,
      JsonNode checks,
      List<Criterion> criteria,
      PdfService.Extraction input,
      boolean content) {
    ArrayNode list = mapper.createArrayNode();
    String source = normalized(input.text());
    Set<Integer> knownLines = new HashSet<>();
    input.lines().forEach(l -> knownLines.add(((Number) l.get("id")).intValue()));
    for (Criterion criterion : criteria) {
      JsonNode assessment = checks.path(criterion.id());
      String status = assessment.path("status").asText();
      if (!Set.of("pass", "partial", "missing", "unverified").contains(status))
        status = "unverified";
      ObjectNode item =
          list.addObject()
              .put("id", criterion.id())
              .put("title", criterion.title())
              .put("requirement", criterion.requirement());
      item.set(
          "sourceQuotes",
          assessment.path("sourceQuotes").isArray()
              ? assessment.path("sourceQuotes")
              : mapper.createArrayNode());
      String evidence = assessment.path("evidence").asText("This check could not be verified.");
      String suggestion =
          assessment.path("suggestion").asText("Review the source document for this requirement.");
      boolean quotesValid = !item.path("sourceQuotes").isEmpty();
      for (JsonNode quote : item.path("sourceQuotes"))
        if (normalized(quote.asText()).isBlank() || !source.contains(normalized(quote.asText())))
          quotesValid = false;
      boolean linesValid = !assessment.path("lineIds").isEmpty();
      for (JsonNode id : assessment.path("lineIds"))
        if (!id.isIntegralNumber() || !knownLines.contains(id.asInt())) linesValid = false;
      if ((status.equals("pass") || status.equals("partial"))
          && !(content ? quotesValid : quotesValid || linesValid)) {
        status = "unverified";
        evidence = "The assessment did not include traceable evidence from the uploaded resume.";
        suggestion =
            "Check the original resume against this requirement before confirming a score.";
      }
      setStatus(item, status, evidence, suggestion);
    }
    return list;
  }

  static ObjectNode score(ObjectMapper mapper, ArrayNode format, ArrayNode content) {
    double lower = 0, upper = 0;
    int unverified = 0;
    boolean allMet = true;
    for (int group = 0; group < (content == null ? 1 : 2); group++) {
      double weight = content == null ? 1 : group == 0 ? .4 : .6;
      for (JsonNode item : group == 0 ? format : content) {
        String status = item.path("assessment").asText("unverified");
        double points = status.equals("pass") ? 1 : status.equals("partial") ? .5 : 0;
        lower += points * weight;
        upper += (status.equals("unverified") ? 1 : points) * weight;
        if (status.equals("unverified")) unverified++;
        if (!status.equals("pass")) allMet = false;
      }
    }
    var score = mapper.createObjectNode().put("unverified", unverified).put("allMet", allMet);
    if (unverified == 0) {
      double value = Math.min(allMet ? 10 : 9.9, Math.round(lower * 10) / 10.0);
      score
          .put("value", value)
          .put("minimum", value)
          .put("maximum", value)
          .put("display", number(value));
    } else {
      double min = Math.floor((lower + 1e-8) * 10) / 10.0;
      double max = Math.min(10, Math.ceil((upper - 1e-8) * 10) / 10.0);
      score.put("value", min);
      score.put("minimum", min).put("maximum", max).put("display", number(min));
    }
    return score;
  }

  private static String number(double n) {
    return n == Math.floor(n) ? Integer.toString((int) n) : String.format(Locale.ROOT, "%.1f", n);
  }

  private static void override(
      ArrayNode list, String id, String status, String evidence, String suggestion) {
    for (JsonNode item : list)
      if (item.path("id").asText().equals(id))
        setStatus((ObjectNode) item, status, evidence, suggestion);
  }

  private static void setStatus(
      ObjectNode item, String status, String evidence, String suggestion) {
    item.put("assessment", status)
        .put("status", status.equals("partial") || status.equals("missing") ? "improve" : status)
        .put(
            "points",
            status.equals("pass")
                ? "1 / 1"
                : status.equals("partial")
                    ? "0.5 / 1"
                    : status.equals("missing") ? "0 / 1" : "Unverified")
        .put("evidence", evidence)
        .put("suggestion", suggestion);
  }

  static String normalized(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFKC).replaceAll("\\s+", " ").strip();
  }
}
