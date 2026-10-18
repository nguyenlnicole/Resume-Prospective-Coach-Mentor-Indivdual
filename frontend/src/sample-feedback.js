const finding = ([id, title, evidence]) => ({
  id,
  title,
  assessment: "pass",
  status: "pass",
  points: "1 / 1",
  evidence,
  suggestion: "Keep this supported detail visible in the finished resume.",
});

export const sampleFeedback = {
  formatScore: 10,
  overallScore: 10,
  formatRating: {
    value: 10,
    minimum: 10,
    maximum: 10,
    display: "10",
    unverified: 0,
    allMet: true,
  },
  overallRating: {
    value: 10,
    minimum: 10,
    maximum: 10,
    display: "10",
    unverified: 0,
    allMet: true,
  },
  scoringVersion: "pnb-reference-example",
  scoringNote:
    "Jordan Avery is an illustrative reference, not a fresh measurement of an uploaded file. Its FitToGo and TrackMyCat technologies still need confirmation; do not copy unverified claims into a real resume.",
  formatFindings: [
    [
      "contact",
      "Complete contact information",
      "Jordan's header shows a name, location, phone number, and email.",
    ],
    [
      "header",
      "Centered name and contact header",
      "The approved reference uses a centered name above a compact contact row.",
    ],
    [
      "summary",
      "Summary length",
      "The adapted summary is 49 words in three sentences.",
    ],
    [
      "sections",
      "Sections and their order",
      "Experience, grouped skills, projects, education, and certification follow the summary.",
    ],
    [
      "typography",
      "Fonts, sizes and emphasis",
      "The reference uses restrained typography, bold employers and skill labels, and italic role titles.",
    ],
    [
      "margins",
      "Page margins",
      "The owner's approved one-page reference provides the layout benchmark.",
    ],
    [
      "spacing",
      "Line, bullet and section spacing",
      "Each role has concise bullets and clear separation from the next entry.",
    ],
    [
      "layout",
      "One-page professional layout",
      "The approved reference is a single-column, one-page resume.",
    ],
    [
      "skills",
      "Grouped technical skills",
      "Languages, frameworks and databases, and tools are grouped by category.",
    ],
    [
      "projects",
      "Project presentation",
      "Three projects name the application and specific features. PNB Paws includes its supplied website and repository; two other project stacks still need confirmation.",
    ],
  ].map(finding),
  contentFindings: [
    [
      "scope",
      "Senior ownership",
      "Example Commerce's order-processing service and Meta's reporting migration show substantial technical ownership.",
    ],
    [
      "architecture",
      "Architecture and design",
      "Access controls, retries, audit logging, and idempotent processing show service design decisions.",
    ],
    [
      "judgment",
      "Technical judgment",
      "The phased PostgreSQL cutover and production-like index validation show careful implementation choices.",
    ],
    [
      "reliability",
      "Reliability and performance",
      "Availability, latency, and repeat-incident results connect technical changes to outcomes.",
    ],
    [
      "quality",
      "Testing and release quality",
      "Contract tests, integration tests, monitoring, and release criteria show verification practices.",
    ],
    [
      "leadership",
      "Cross-team leadership",
      "Jordan led six engineers across three teams to launch an order-status feature.",
    ],
    [
      "mentoring",
      "Mentoring and knowledge sharing",
      "Design and code reviews supported three engineers; setup documentation helped four new engineers.",
    ],
    [
      "collaboration",
      "Collaborative delivery",
      "API contracts, product and QA coordination, and release planning demonstrate cross-team work.",
    ],
    [
      "outcomes",
      "Credible outcomes",
      "The reference ties work to 99.95% availability, 350 ms p95 latency, and fewer repeat incidents.",
    ],
    [
      "projects",
      "Specific project work",
      "The projects describe users, features, interfaces, services, and data stores rather than vague app titles.",
    ],
  ].map(finding),
};
