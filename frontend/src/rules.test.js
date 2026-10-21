import test from "node:test";
import assert from "node:assert/strict";
import {
  countWords,
  briefFeedback,
  githubUsername,
  resumeFilename,
  countSentences,
  safeLink,
  normalizeWebLink,
  validateResume,
  experienceList,
  suggestedIndexes,
  suggestedProjectIndexes,
  suggestedExperienceIndexes,
  experienceBullets,
  experienceBulletLimit,
  selectExperienceBullets,
  experienceYearsCovered,
  projectDescriptionWords,
  projectStack,
  formatProjectLinks,
  projectLinkError,
  replaceProjectLinks,
  replaceProjectStack,
  normalizeProjectHeadings,
  credibleProjectStack,
  isTechnical,
  compactEducation,
  parseEducation,
  formatEducation,
  educationEntryError,
} from "./rules.js";
import { sampleResume } from "./model.js";
import referenceResume from "./reference-resume.json" with { type: "json" };
import { githubRepository } from "./github-repo.js";

test("the public reference follows the longer project example without separate Tech lines", () => {
  assert.equal(referenceResume.contact.name, "Jordan Avery");
  assert.ok(countWords(referenceResume.summary) >= 35);
  for (const project of referenceResume.projects.split(/\n\s*\n/)) {
    assert.ok(project.split("\n")[0].includes(" — "));
    assert.ok(projectDescriptionWords(project) >= 25);
    assert.ok(!project.includes("\nTech:"));
  }
});

test("educator uses a nontechnical resume path", () => {
  assert.equal(isTechnical("Educator"), false);
  const educator = structuredClone(sampleResume);
  educator.projects = "";
  educator.softwareEngineering = false;
  assert.deepEqual(validateResume(educator, "Educator"), []);
});

test("confirmed repository technologies replace a broken Tech line on the heading", () => {
  const source =
    "Pet Shelter | https://github.com/owner/pet-shelter\nTech: Have have\nBuilt a pet care web application.";
  assert.equal(credibleProjectStack(projectStack(source)), false);
  const corrected = replaceProjectStack(
    source,
    "Java, Spring Boot, PostgreSQL",
  );
  assert.ok(
    corrected.startsWith(
      "Pet Shelter | https://github.com/owner/pet-shelter — Java, Spring Boot, PostgreSQL",
    ),
  );
  assert.ok(!corrected.includes("Tech:"));
  assert.equal(normalizeProjectHeadings(corrected), corrected);
  assert.deepEqual(githubRepository("https://github.com/owner/pet-shelter"), [
    "owner",
    "pet-shelter",
  ]);
  assert.equal(githubRepository("https://github.com/owner"), null);
});

test("short feedback stays within the mobile limit", () => {
  const short = briefFeedback(
    "This bullet needs a verified result. Add the outcome so the reader understands its impact.",
  );
  assert.ok(countWords(short) <= 25);
  const long = briefFeedback(
    "One. Two. Three. Four. " + "Specific feedback matters. ".repeat(20),
  );
  assert.ok(countWords(long) <= 25);
  assert.ok(countSentences(long) <= 3);
});

test("public GitHub profile links identify only account pages", () => {
  assert.equal(githubUsername("https://github.com/example"), "example");
  assert.equal(githubUsername("https://github.com/example/project"), "");
  assert.equal(githubUsername("https://evil.example/example"), "");
});
test("project suggestions favor relevant, evidenced work while letting people keep their own picks", () => {
  const projects = [
    "Weather app\nBuilt a forecast page.",
    "Care API | https://github.com/example/care\nTech: Java\nBuilt a healthcare API.",
    "Care dashboard\nTech: React\nBuilt a healthcare dashboard.",
    "Notes app\nBuilt a notes page.",
  ];
  assert.deepEqual(
    suggestedProjectIndexes(projects, "healthcare Java"),
    [0, 1, 2],
  );
});

test("resume downloads use first-last-PNBRP-month-year", () => {
  assert.equal(
    resumeFilename("Jordan Avery", "pdf", new Date(2026, 9, 13)),
    "Jordan-Avery-PNBRP-1026.pdf",
  );
  assert.equal(
    resumeFilename("Anne-Marie O'Neil", "docx", new Date(2027, 0, 1)),
    "Anne-Marie-O-Neil-PNBRP-0127.docx",
  );
});

test("career choices keep distinct jobs and favor a matching target role", () => {
  const jobs = experienceList(
    "Designer | 2016–2018\n• Designed pages.\n\nEngineer | 2018–2021\n• Built APIs.\n\nAnalyst | 2021–2023\n• Analyzed data.\n\nEngineer | 2023–2024\n• Built apps.\n\nManager | 2024–Present\n• Led a team.",
  );
  assert.equal(jobs.length, 5);
  assert.deepEqual(suggestedIndexes(jobs, "Engineer", 4), [0, 1, 2, 3]);
  assert.deepEqual(suggestedIndexes(jobs, "Manager", 4), [0, 1, 2, 4]);
});

test("job suggestions use three roles when they span eight non-overlapping years", () => {
  const jobs = [
    "Engineer | 2016–2020\n• Built APIs.",
    "Engineer | 2020–2024\n• Built apps.",
    "Designer | 2022–2025\n• Designed pages.",
    "Analyst | 2025–2026\n• Analyzed data.",
    "Manager | 2026–Present\n• Led a team.",
  ];
  assert.deepEqual(suggestedExperienceIndexes(jobs, "Engineer"), [0, 1, 2]);
  assert.equal(experienceYearsCovered([jobs[0], jobs[1], jobs[2]]), 9);
  assert.equal(experienceYearsCovered([jobs[1], jobs[2]]), 5);
  assert.equal(suggestedExperienceIndexes(jobs.slice(1), "Engineer").length, 4);
});

test("senior and management suggestions keep five relevant roles when available", () => {
  const jobs = [
    "Engineer | 2007–2010\n• Built tools.",
    "Designer | 2010–2013\n• Designed pages.",
    "Engineer | 2013–2016\n• Built APIs.",
    "Engineer | 2016–2019\n• Built services.",
    "Analyst | 2019–2022\n• Analyzed data.",
    "Engineer | 2022–2024\n• Improved reliability.",
    "Engineer | 2024–Present\n• Led delivery.",
  ];
  for (const level of ["Senior", "Management"]) {
    const selected = suggestedExperienceIndexes(jobs, "Engineer", level);
    assert.equal(selected.length, 5);
    assert.ok(selected.includes(0));
    assert.ok(
      experienceYearsCovered(selected.map((index) => jobs[index])) >= 13,
    );
  }
});

test("multiple roles at one employer retain the employer name and distinct dates", () => {
  const roles = experienceList(
    "Acme Labs\nEngineer | 2016–2020\n• Built APIs.\nSenior Engineer | 2020–2024\n• Led delivery.",
  );
  assert.equal(roles.length, 2);
  assert.ok(roles.every((role) => role.startsWith("Acme Labs")));
  assert.equal(experienceYearsCovered(roles), 8);
});
test("grouped roles follow recency bullet ranges", () => {
  const r = structuredClone(sampleResume);
  const bullet =
    "• Delivered supported API work with tests and release checks.";
  r.experience = `Example Company\nSenior Engineer | 2023–Present\n${bullet}\n${bullet}\n${bullet}\nEngineer | 2021–2023\n${bullet}\n${bullet}`;
  const jobs = experienceList(r.experience);
  assert.deepEqual(
    jobs.map((_, i) => experienceBulletLimit(jobs, i)),
    [4, 3],
  );
  assert.deepEqual(validateResume(r, "Senior"), []);
  r.experience += `\n${bullet}\n${bullet}`;
  assert.ok(
    validateResume(r, "Senior").some((message) => message.includes("oldest")),
  );
});

test("experience stays under one section at every career level", () => {
  for (const level of ["Entry / Junior", "Mid-level", "Senior", "Management"]) {
    const r = structuredClone(sampleResume);
    r.experience +=
      "\n\nEarlier Experience\nEngineer | 2016–2018\n• Built APIs.";
    assert.ok(
      !validateResume(r, level).some((error) =>
        error.includes("single Experience section"),
      ),
    );
    r.experience =
      sampleResume.experience +
      "\n\nAcme Labs\nEngineer | 2016–2018\n• Built APIs.";
    assert.ok(
      !validateResume(r, level).some((error) =>
        error.includes("single Experience section"),
      ),
    );
  }
});

test("jobs with fewer bullets do not block completion", () => {
  for (const count of [0, 1, 2]) {
    const r = structuredClone(sampleResume);
    r.experience =
      "Example Company — Engineer | 2023–Present" +
      "\n• Delivered supported API work and documented release checks.".repeat(
        count,
      );
    assert.deepEqual(validateResume(r, "Senior"), []);
  }
});

test("summary sentence counting preserves technology and degree names", () => {
  assert.equal(
    countSentences(
      "Engineer using Node.js and Java 21.0. Holds a B.S. in computer science.",
    ),
    2,
  );
  assert.equal(countSentences("   "), 0);
});
test("summary counts and valid sample", () => {
  assert.equal(countSentences(sampleResume.summary), 3);
  assert.equal(countWords(sampleResume.summary), 36);
  assert.deepEqual(validateResume(sampleResume, "Mid-level"), []);
});
test("education supports completed, expected, and undated unfinished programs", () => {
  const entries = [
    {
      details: "Example University — B.S. in Computer Science",
      status: "graduated",
      month: "May",
      year: "2021",
    },
    {
      details:
        "Example College — M.S. in Data Science\nRelevant coursework: databases",
      status: "progress",
      month: "December",
      year: "2099",
    },
    {
      details: "Example Institute — Coursework in Design",
      status: "progress",
      month: "",
      year: "",
    },
    {
      details: "Example School — Coursework in Accounting",
      status: "incomplete",
      month: "",
      year: "",
    },
  ];
  const text = formatEducation(entries);
  assert.deepEqual(
    parseEducation(text),
    entries.map((entry) => ({
      ...entry,
      details: entry.details.split("\n")[0],
    })),
  );
  const resume = structuredClone(sampleResume);
  resume.education = text;
  assert.deepEqual(validateResume(resume, "Mid-level"), []);
});
test("education status and graduation date are optional", () => {
  const resume = structuredClone(sampleResume);
  resume.education = "Example University — B.S. in Computer Science | 2021";
  assert.deepEqual(validateResume(resume, "Mid-level"), []);
  assert.equal(
    formatEducation(parseEducation(resume.education)),
    "Example University — B.S. in Computer Science | 2021",
  );
  resume.education = formatEducation([
    {
      details: "Example University — B.S.",
      status: "graduated",
      month: "",
      year: "",
    },
  ]);
  assert.equal(resume.education, "Example University — B.S.");
  assert.deepEqual(validateResume(resume, "Mid-level"), []);
  const entry = {
    details: "Example University — B.S.",
    status: "graduated",
    month: "",
    year: "2021",
  };
  assert.equal(educationEntryError(entry), "");
  resume.education = "Example University — B.S. | Expected May 2020";
  assert.ok(
    validateResume(resume, "Mid-level").some((message) =>
      message.includes("this month or later"),
    ),
  );
});
test("experience bullet selection keeps only the three chosen claims", () => {
  const entry =
    "Example — Engineer | 2021–Present\n• First documented contribution with a supported outcome.\n• Second documented contribution with a supported outcome.\n• Third documented contribution with a supported outcome.\n• Fourth documented contribution with a supported outcome.";
  assert.equal(experienceBullets(entry).length, 4);
  const selected = selectExperienceBullets(entry, [0, 2, 3]);
  assert.equal(experienceBullets(selected).length, 3);
  assert.ok(!selected.includes("Second documented"));
});
test("experience bullet count is checked without blocking on word length", () => {
  const resume = structuredClone(sampleResume);
  resume.experience += "\n• Short claim.";
  assert.ok(
    !validateResume(resume, "Mid-level").some((message) =>
      message.includes("15–30 words"),
    ),
  );
  assert.ok(
    validateResume(resume, "Mid-level").some((message) =>
      message.includes("bullets"),
    ),
  );
});
test("long project descriptions remain allowed and counts exclude the heading", () => {
  const project = "Care app | https://github.com/example/care\nTech: Java, React\n" + "built ".repeat(45);
  assert.equal(projectDescriptionWords(project), 45);
  const resume = structuredClone(sampleResume);
  resume.projects =
    project + "extra\n\n" + resume.projects.split("\n\n").slice(1).join("\n\n");
  assert.ok(
    !validateResume(resume, "Mid-level").some((message) =>
      message.includes("45 words"),
    ),
  );
});
test("project link styles keep the original demo and repository targets", () => {
  const project =
    "PNB Paws | https://pnbpaws.example/demo | https://github.com/example/pnbpaws\nBuilt a healthcare application.";
  const descriptive = formatProjectLinks(project, "descriptive");
  const compact = formatProjectLinks(project, "compact");
  assert.match(
    descriptive,
    /\[PNB Paws Web Application\]\(https:\/\/pnbpaws\.example\/demo\) \| \[PNB Paws GitHub\]\(https:\/\/github\.com\/example\/pnbpaws\)/,
  );
  assert.match(
    compact,
    /PNB Paws \| \[Demo\]\(https:\/\/pnbpaws\.example\/demo\) \| \[Repo\]\(https:\/\/github\.com\/example\/pnbpaws\)/,
  );
  assert.match(compact, /Built a healthcare application\./);
  assert.equal(projectDescriptionWords(compact), 4);
});
test("project links allow a shared URL within and across projects", () => {
  const links =
    "https://github.com/example/care | https://example.com/care-demo";
  assert.equal(projectLinkError(links), "");
  assert.equal(projectLinkError("https://github.com/example/care"), "");
  assert.equal(projectLinkError("https://example.com/care-demo"), "");
  assert.equal(projectLinkError(""), "");
  assert.equal(
    projectLinkError(
      "https://github.com/example/care | https://github.com/example/care/",
    ),
    "",
  );
  const updated = replaceProjectLinks(
    "Care app | https://github.com/example/old\nTech: Java\nBuilt care tools.",
    links,
  );
  assert.match(
    updated,
    /^Care app \| https:\/\/github.com\/example\/care \| https:\/\/example.com\/care-demo/,
  );
  assert.match(updated, /Tech: Java/);
  const resume = structuredClone(sampleResume);
  resume.projects = [updated, updated].join("\n\n");
  assert.ok(
    !validateResume(resume, "Senior").some((message) =>
      message.includes("different URL"),
    ),
  );
});
test("explicit demo and repository links survive a shared URL in both project styles", () => {
  const project =
    "FitToGo | [Demo](https://github.com) | [GitHub Repo](https://github.com) — React\nBuilt a workout organizer.";
  for (const style of ["compact", "descriptive"]) {
    const formatted = formatProjectLinks(project, style);
    assert.match(
      formatted,
      style === "compact"
        ? /\[Demo\]\(https:\/\/github\.com\)/
        : /\[FitToGo Web Application\]\(https:\/\/github\.com\)/,
    );
    assert.match(
      formatted,
      /\[(?:FitToGo GitHub|Repo)\]\(https:\/\/github\.com\)/,
    );
    assert.equal(formatProjectLinks(formatted, style), formatted);
    const normalized = normalizeProjectHeadings(formatted);
    assert.match(normalized, /\[Demo\]\(https:\/\/github\.com\)/);
    assert.match(normalized, /\[Repo\]\(https:\/\/github\.com\)/);
  }
});

test("project names ending in Web Page do not duplicate labels or misclassify repositories", () => {
  const demo = "https://pyk006.github.io/TEVue/";
  const repo = "https://github.com/example/PetShelterWebpage";
  const repo2 = "https://github.com/example/PetShelterPage";
  const source = `Pet Shelter Web Page | ${demo} | ${repo} | ${repo2} — Java, PostgreSQL\nBuilt a pet adoption application.`;
  const descriptive = formatProjectLinks(source, "descriptive");
  assert.ok(!descriptive.includes("Web Page Web Page"));
  assert.ok(descriptive.includes(`[Pet Shelter Web Application](${demo})`));
  assert.ok(descriptive.includes(`[Pet Shelter GitHub](${repo})`));
  assert.ok(!descriptive.includes(repo2));
  assert.equal(formatProjectLinks(descriptive, "descriptive"), descriptive);
  const legacy = `[Pet Shelter Web Page Web Page](${demo}) | [Pet Shelter Web Page GitHub Repo](${repo}) | [Pet Shelter Web Page GitHub Repo](${repo2}) — Java\nBuilt a pet adoption application.`;
  const repaired = formatProjectLinks(normalizeProjectHeadings(legacy));
  assert.ok(!repaired.includes("Web Page Web Page"));
  assert.ok(repaired.includes(`[Demo](${demo})`));
  assert.ok(repaired.includes(`[Repo](${repo})`));
  assert.ok(!repaired.includes(repo2));
});

test("a second repository or branch never becomes Repo 2", () => {
  const primary = "https://github.com/person/pets";
  const branch = primary + "/tree/someone-elses-work";
  const source = `Pets | https://pets.example | ${primary} | ${branch} — Java\nBuilt a pet directory.`;
  for (const style of ["compact", "descriptive"]) {
    const result = formatProjectLinks(source, style);
    assert.ok(result.includes(primary));
    assert.ok(!result.includes(branch));
    assert.ok(!result.includes("Repo 2"));
  }
});

test("project technologies stay with links and missing facts are requested", () => {
  const project =
    "Care app | https://github.com/example/care — Java, React\nBuilt a care organizer.";
  assert.equal(projectStack(project), "Java, React");
  assert.match(
    formatProjectLinks(project),
    /\[Repo\]\(https:\/\/github\.com\/example\/care\) — Java, React/,
  );
  const r = structuredClone(sampleResume);
  r.projects = r.projects.replace(/Tech:[^\n]+\n/, "");
  assert.ok(
    !validateResume(r, "Mid-level").some((message) =>
      message.includes("verified technologies"),
    ),
  );
});
test("junior coding resumes allow more than three projects", () => {
  let r = structuredClone(sampleResume);
  r.projects += "\n\nAnother app | https://example.com/demo\nBuilt an app.";
  assert.ok(
    !validateResume(r, "Entry / Junior").some((x) =>
      x.includes("at least 3 projects"),
    ),
  );
});
test("senior projects can have a demo or no links", () => {
  const resume = structuredClone(sampleResume);
  resume.projects =
    "Portfolio app | https://example.com/demo\nTech: Java\nBuilt a portfolio app.";
  assert.deepEqual(validateResume(resume, "Senior"), []);
  resume.projects = "Portfolio app\nTech: Java\nBuilt a portfolio app.";
  assert.deepEqual(validateResume(resume, "Senior"), []);
  resume.projects =
    "Portfolio app | https://github.com/example/portfolio\nTech: Java\nBuilt a portfolio app.";
  assert.deepEqual(validateResume(resume, "Senior"), []);
});
test("senior and management projects are optional", () => {
  let r = structuredClone(sampleResume);
  r.projects = "";
  assert.deepEqual(validateResume(r, "Senior"), []);
  assert.deepEqual(validateResume(r, "Management"), []);
});
test("junior and mid-level recommend at least three projects", () => {
  for (const level of ["Entry / Junior", "Mid-level"]) {
    const r = structuredClone(sampleResume);
    const projects = r.projects.split("\n\n");
    for (const count of [0, 1, 2]) {
      r.projects = [...projects, projects[0]].slice(0, count).join("\n\n");
      assert.ok(validateResume(r, level).some((e) => e.includes("at least 3")));
    }
    r.projects = projects.join("\n\n");
    assert.deepEqual(validateResume(r, level), []);
  }
});
test("three coding projects can be exported without project links", () => {
  const resume = structuredClone(sampleResume);
  resume.projects = resume.projects.replace(/\s*\|\s*https?:\/\/[^\s|)]+/g, "");
  assert.deepEqual(validateResume(resume, "Mid-level"), []);
});
test("the complete header is required at every career level", () => {
  for (const level of ["Entry / Junior", "Mid-level", "Senior", "Management"]) {
    for (const key of ["location"]) {
      const r = structuredClone(sampleResume);
      r.contact[key] = "";
      assert.ok(validateResume(r, level).length > 0);
    }
  }
  const withoutGithub = structuredClone(sampleResume);
  withoutGithub.contact.github = "";
  assert.deepEqual(validateResume(withoutGithub, "Senior"), []);
  withoutGithub.contact.linkedin = "";
  assert.deepEqual(validateResume(withoutGithub, "Senior"), []);
  const withoutPortfolio = structuredClone(sampleResume);
  withoutPortfolio.contact.portfolio = "";
  assert.deepEqual(validateResume(withoutPortfolio, "Senior"), []);
});
test("unsafe contact links cannot be exported", () => {
  let r = structuredClone(sampleResume);
  r.contact.linkedin = "javascript:alert(1)";
  assert.equal(safeLink(r.contact.linkedin), null);
  assert.ok(validateResume(r, "Senior").some((x) => x.includes("linkedin")));
});
test("the professional summary needs 35 to 45 words", () => {
  let r = structuredClone(sampleResume);
  r.summary = "I build applications. I test software.";
  assert.ok(
    validateResume(r, "Senior").some((x) => x.includes("35 and 45 words")),
  );
  r.summary = "I " + "build ".repeat(54) + ". I test software.";
  assert.ok(
    validateResume(r, "Senior").some((x) => x.includes("35 and 45 words")),
  );
});

test("coding requirements apply only when selected", () => {
  const resume = structuredClone(sampleResume);
  resume.softwareEngineering = false;
  resume.contact.github = "";
  resume.projects = "";
  assert.deepEqual(validateResume(resume, "Mid-level"), []);
  resume.softwareEngineering = true;
  assert.ok(
    validateResume(resume, "Mid-level").some((error) =>
      error.includes("3 projects"),
    ),
  );
  resume.softwareEngineering = false;
  resume.projects =
    "Security lab\n• Investigated authentication events in a training environment.";
  assert.deepEqual(validateResume(resume, "Mid-level"), []);
  assert.equal(projectLinkError("", false, false), "");
  assert.notEqual(projectLinkError("javascript:alert(1)", false, false), "");
});
test("optional profile links accept domains without https", () => {
  assert.equal(
    normalizeWebLink(" github.com/amen2x "),
    "https://github.com/amen2x",
  );
  assert.equal(
    normalizeWebLink("linkedin.com/in/person"),
    "https://linkedin.com/in/person",
  );
  assert.equal(
    normalizeWebLink("portfolio.example"),
    "https://portfolio.example",
  );
  assert.equal(normalizeWebLink(""), "");
  assert.equal(
    normalizeWebLink("https://github.com/person"),
    "https://github.com/person",
  );
  assert.equal(safeLink(normalizeWebLink("javascript:alert(1)")), null);
});

import { accessDurationLabel } from "./rules.js";
test("access labels distinguish usage trials from timed passes without epoch dates", () => {
  assert.equal(
    accessDurationLabel({
      registered: true,
      plan: "trial",
      trial: { expiresAt: 0 },
    }),
    "Free trial · One review and build",
  );
  assert.equal(
    accessDurationLabel({
      registered: true,
      plan: "subscribed",
      trial: { expiresAt: 0 },
    }),
    "Approved account",
  );
  assert.equal(
    accessDurationLabel({ accessKind: "permanent-coach" }),
    "Permanent",
  );
  for (const expiresAt of [0, null, undefined, -1, "invalid"])
    assert.equal(accessDurationLabel({ trial: { expiresAt } }), "");
  assert.match(
    accessDurationLabel({ trial: { expiresAt: 1800000000000 } }),
    /^Ends /,
  );
});

test("education cleanup removes descriptions and keeps separate credentials and optional dates", () => {
  const source =
    "Example University — Bachelor's in Software Engineering\nAccelerated software engineering program focused on programming.\n\nTraining Provider — Technical mentorship\nCompleted technical mentorship focused on development.\nBootcamp Provider — Full-stack coding bootcamp\nAttended a 14-week bootcamp.\nExample College\nAssociate of Science\nMay 2024";
  const expected =
    "Example University — Bachelor's in Software Engineering\n\nTraining Provider — Technical mentorship\n\nBootcamp Provider — Full-stack coding bootcamp\n\nExample College — Associate of Science | May 2024";
  assert.equal(compactEducation(source), expected);
  assert.equal(compactEducation(expected), expected);
  assert.equal(formatEducation(parseEducation(source)), expected);
});

test("formatted project labels do not duplicate and all original destinations survive", () => {
  const source =
    "Care app | [View Project](https://care.example/demo) | [GitHub Repo](https://github.com/example/care) — Java, React\nBuilt a care organizer.";
  for (const style of ["compact", "descriptive"]) {
    const once = formatProjectLinks(source, style);
    assert.equal(formatProjectLinks(once, style), once);
    assert.ok(!once.includes("[View Project]()"));
    assert.ok(once.includes("https://care.example/demo"));
    assert.ok(once.includes("https://github.com/example/care"));
    const normalized = normalizeProjectHeadings(once);
    assert.ok(normalized.startsWith("Care app"));
    assert.ok(normalized.includes("https://care.example/demo"));
  }
});

test("summary boundaries are 35–45 across professions", () => {
  for (const level of ["Senior", "General", "Educator", "Medical Field"]) {
    for (const words of [34, 35, 45, 46]) {
      const r = structuredClone(sampleResume);
      r.summary = "Experience ".repeat(words - 2) + "matters. Contributions.";
      assert.equal(countWords(r.summary), words);
      assert.equal(
        validateResume(r, level).some((error) => error.includes("35 and 45")),
        words < 35 || words > 45,
      );
    }
  }
});

test("project demo and repository separated by dashes become distinct piped links", () => {
  const project =
    "Trillr | https://example.github.io/trillr — https://github.com/example/trillr — HTML, TypeScript\nBuilt a messaging application.";
  const formatted = formatProjectLinks(normalizeProjectHeadings(project));
  assert.match(
    formatted,
    /\[Demo\]\(https:\/\/example.github.io\/trillr\) \| \[Repo\]\(https:\/\/github.com\/example\/trillr\) — HTML, TypeScript/,
  );
  const labeled =
    "Trillr | [Demo](https://github.com/example/demo) | [GitHub Repo](https://github.com/example/repo) — React\nBuilt a messaging application.";
  assert.match(
    formatProjectLinks(
      replaceProjectLinks(
        labeled,
        "https://github.com/example/demo | https://github.com/example/repo",
      ),
    ),
    /\[Demo\].* \| \[Repo\]/,
  );
});

test("education autofills graduation from abbreviated ranges and removes old date copies", () => {
  const source =
    "University Of Houston — Bachelor of Arts in Computer Science, Minor in Mathematics | Aug. 2020–May 2022\n\nSan Jacinto College — Associate’s in Computer Science | Jan. 2018–May 2020";
  const entries = parseEducation(source);
  assert.equal(entries[0].status, "graduated");
  assert.equal(entries[0].month, "May");
  assert.equal(entries[0].year, "2022");
  assert.equal(entries[1].year, "2020");
  assert.ok(!entries[0].details.includes("2020"));
  const output = formatEducation(entries);
  assert.equal((output.match(/2022/g) || []).length, 1);
  assert.equal(formatEducation(parseEducation(output)), output);
  assert.equal(
    compactEducation(
      "Example University — B.S. | Aug. 2020–May 2022 | Graduated May 2022",
    ),
    "Example University — B.S. | May 2022",
  );
  assert.equal(
    formatEducation([{ ...entries[0], year: "2023" }]).includes("2022"),
    false,
  );
  const expected = parseEducation(
    "Example University — B.S. | Expected Dec. 2099",
  )[0];
  assert.equal(expected.status, "progress");
  assert.equal(expected.month, "December");
  assert.equal(parseEducation("Example University — B.S.")[0].year, "");
  assert.equal(
    parseEducation("Example University — B.S. | 2020–Present")[0].year,
    "",
  );
});

test("graduation dates display without the Graduated label while fields still autofill", () => {
  const entries = parseEducation(
    "Example University — B.S. | Graduated May 2022",
  );
  assert.equal(entries[0].status, "graduated");
  assert.equal(entries[0].month, "May");
  assert.equal(entries[0].year, "2022");
  assert.equal(
    formatEducation(entries),
    "Example University — B.S. | May 2022",
  );
  assert.equal(
    compactEducation("Example University — B.S. | Graduated"),
    "Example University — B.S.",
  );
});

test("project link styles use the requested short and project-named labels", () => {
  const source =
    "EverPure Web Application | https://everpure.example | https://github.com/example/everpure — React\nBuilt a healthcare application.";
  const compact = formatProjectLinks(source, "compact");
  const named = formatProjectLinks(source, "descriptive");
  assert.ok(
    compact.includes(
      "[Demo](https://everpure.example) | [Repo](https://github.com/example/everpure)",
    ),
  );
  assert.ok(
    named.startsWith(
      "[EverPure Web Application](https://everpure.example) | [EverPure GitHub](https://github.com/example/everpure)",
    ),
  );
  assert.equal(formatProjectLinks(named, "descriptive"), named);
  assert.equal(formatProjectLinks(compact, "compact"), compact);
  const legacy =
    "EverPure | [Demo](https://everpure.example) | [GitHub Repo](https://github.com/example/everpure) — React\nBuilt a healthcare application.";
  assert.ok(
    formatProjectLinks(legacy).includes(
      "[Repo](https://github.com/example/everpure)",
    ),
  );
});
