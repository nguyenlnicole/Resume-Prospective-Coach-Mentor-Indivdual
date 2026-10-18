import { sampleFeedback } from "./sample-feedback.js";
import referenceResume from "./reference-resume.json" with { type: "json" };

export const levels = ["Entry / Junior", "Mid-level", "Senior", "Management"];
export const fonts = [
  "Calibri",
  "Arial",
  "Aptos",
  "Cambria",
  "Georgia",
  "Times New Roman",
];
export const levelGuidelines = {
  General: {
    name: "General",
    focus:
      "Highlight relevant work from the last 10 years with 3–4 bullets for the newest job, three for other jobs, and 2–3 for the oldest. Include contact details and a short summary. Coding projects are not required.",
  },
  "Medical Field": {
    name: "Medical Field",
    focus:
      "Highlight healthcare experience, skills, and credentials you hold. Protect patient privacy. Coding projects are not required.",
  },
  Educator: {
    name: "Educator",
    focus:
      "Highlight teaching experience, methods, and outcomes. List only credentials you hold. Coding projects are not required.",
  },
  "Entry / Junior": {
    name: "Junior",
    focus:
      "Highlight hands-on skills and real contributions. Include up to 3 roles and, for coding resumes, 3 real projects. Project links are optional.",
  },
  "Mid-level": {
    name: "Mid-level",
    focus:
      "Highlight independent delivery, problem solving, and collaboration. For coding resumes, include 3 real projects; links are optional.",
  },
  Senior: {
    name: "Senior",
    focus:
      "Highlight technical ownership, architecture, mentoring, and impact. Projects are optional.",
  },
  Management: {
    name: "Management",
    focus:
      "Highlight leadership, coaching, and team outcomes. Two professional certifications are needed for 10/10. Projects are optional.",
  },
};
Object.defineProperty(levelGuidelines, "Normal", {
  value: levelGuidelines.General,
});
export const sections = [
  ["contact", "Contact Information"],
  ["summary", "Professional Summary"],
  ["experience", "Experience"],
  ["clinicalRotations", "Clinical Rotations"],
  ["clinicalTraining", "Clinical Training"],
  ["proceduresSpecialty", "Procedures & Specialty Experience"],
  ["residencyFellowship", "Residency / Fellowship"],
  ["licensure", "Licensure"],
  ["boardCertification", "Board Certification"],
  ["research", "Research"],
  ["publications", "Publications"],
  ["teaching", "Teaching"],
  ["presentations", "Presentations"],
  ["skills", "Technical Skills"],
  ["projects", "Technical Projects"],
  ["education", "Education"],
  ["certifications", "Certifications"],
];
export function resumeSections(level) {
  return sections
    .filter(
      ([key]) =>
        ![
          "clinicalRotations",
          "clinicalTraining",
          "proceduresSpecialty",
          "residencyFellowship",
          "licensure",
          "boardCertification",
          "research",
          "publications",
          "teaching",
          "presentations",
        ].includes(key) || level === "Medical Field",
    )
    .map(([key, label]) => [
      key,
      key === "skills" && ["General", "Normal"].includes(level)
        ? "Professional Skills"
        : key === "skills" && level === "Educator"
          ? "Teaching & Professional Skills"
          : key === "skills" && level === "Medical Field"
            ? "Clinical & Professional Skills"
            : key === "projects" &&
                ["General", "Normal", "Medical Field", "Educator"].includes(
                  level,
                )
              ? "Projects & Volunteer Experience"
              : key === "certifications" && level === "Educator"
                ? "Teaching Credentials & Certifications"
                : key === "certifications" && level === "Medical Field"
                  ? "Licensure and Certifications"
                  : label,
    ]);
}
export const sampleResume = {
  softwareEngineering: true,
  contact: {
    name: "Alex Rivera",
    location: "San Francisco, CA",
    email: "alex@example.com",
    phone: "(555) 010-0142",
    linkedin: "https://www.linkedin.com/in/example",
    github: "https://github.com/example",
    portfolio: "https://example.com/portfolio",
  },
  summary:
    "Software engineer building reliable applications with Java, Spring Boot, React, and PostgreSQL. Owns features from planning through release, investigates production issues, and improves testing practices. Brings a thoughtful approach to collaboration, documentation, and accessible user experiences.",
  experience:
    "Northstar Studio — Software Engineer | 2023–Present\n• Delivered a customer account feature using Java and React, owning API design, implementation, testing, and release with the product team.\n• Investigated recurring synchronization failures and added regression tests to prevent repeat issues in the customer account workflow.\n• Reviewed pull requests and documented setup steps so new teammates could reproduce the development environment and contribute to the codebase.\n\nJuniper Digital — Associate Software Engineer | 2021–2023\n• Built REST endpoints with input validation and consistent error handling for assigned application features in collaboration with the engineering team.\n• Improved mobile navigation and added integration tests for the checkout workflow to verify behavior across supported screen sizes.\n• Used Git and issue tracking to organize assigned work, incorporate code review feedback, and document changes before release.",
  skills:
    "Languages: Java, JavaScript, TypeScript, SQL\nFrameworks: Spring Boot, React\nDatabases: PostgreSQL\nTools & Practices: Git, JUnit, Integration Testing, API Design",
  projects:
    "CareTrack | https://github.com/example/caretrack\nTech: Java, Spring Boot, React, PostgreSQL, REST APIs\nDeveloped a full-stack pet care web application where owners manage profiles, schedule reminders, and organize health records, connecting a React interface to Spring Boot REST APIs and a PostgreSQL database.\n\nReleaseBoard | https://github.com/example/releaseboard\nTech: TypeScript, React, Node.js, Express, PostgreSQL\nBuilt a release-tracking web application with deployment checklists, status updates, and searchable history, connecting a React dashboard to Express APIs and PostgreSQL so teams can review each release in one place.\n\nQueryLab | https://github.com/example/querylab\nTech: Java, Spring Boot, PostgreSQL, JUnit\nCreated a SQL practice software application where users save queries, compare results, and revisit exercises, adding input validation and JUnit tests for the Spring Boot endpoints that manage saved work.",
  education:
    "Example University — B.S. in Computer Science | Graduated May 2021",
  certifications: "Professional Scrum Master I — Scrum.org | 2024",
};
export const sampleReview = {
  level: "Senior",
  ...sampleFeedback,
  sample: true,
  questions: [],
  resume: referenceResume,
};
export const preferenceGroups = [
  {
    title: "What should stand out?",
    items: [
      "My most relevant work or projects",
      "My strongest results",
      "Skills for the target job",
      "Add another option",
    ],
  },
];
export const rewriteOptions = [
  "Polish my wording within the resume rules",
  "Suggest stronger wording using my facts",
  "Add another option",
];
export const howSteps = [
  [
    "Upload and preview",
    "Add a PDF, Word document, or resume image. Preview what you uploaded before the review begins.",
  ],
  [
    "See your format score",
    "Check the layout, typography, and organization score, with quick feedback you can act on.",
  ],
  [
    "See your overall score",
    "Review how clearly your experience and accomplishments come through. Open more detail only when you want it.",
  ],
  [
    "Choose your focus",
    "Add a target job posting if you have one, then choose the experience and projects to emphasize.",
  ],
  [
    "Build it together",
    "Answer missing-detail questions and edit your sections while a second version is prepared for comparison.",
  ],
  [
    "Choose and share",
    "Compare both versions, choose one, then download a PDF or Word document or email yourself the PDF.",
  ],
];

export const defaultResumeFont = (level) =>
  ["General", "Normal", "Medical Field", "Educator"].includes(level)
    ? "Arial"
    : "Calibri";
