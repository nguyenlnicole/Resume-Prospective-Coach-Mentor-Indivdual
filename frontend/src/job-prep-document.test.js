import test from "node:test";
import assert from "node:assert/strict";
import {
  jobPrepDocumentText,
  readableJobPrepText,
} from "./job-prep-document.js";

test("prep PDF text retains all study topics, preparation steps, and source URLs", () => {
  const sheet = {
    study: Array.from({ length: 6 }, (_, i) => `Topic ${i + 1}`),
    prepare: ["Prepare one", "Prepare two"],
    facts: [
      {
        fact: "Verified fact",
        sourceTitle: "Employer",
        sourceUrl: "https://example.com/source",
      },
    ],
  };
  const text = jobPrepDocumentText(sheet);
  for (const value of [
    ...sheet.study,
    ...sheet.prepare,
    "Verified fact",
    "Employer",
    "https://example.com/source",
  ])
    assert.ok(text.includes(value));
});
test("prep PDF text never invents unavailable source facts", () => {
  assert.equal(jobPrepDocumentText(null), "");
  assert.ok(
    jobPrepDocumentText({ study: [], prepare: [], facts: [] }).includes(
      "No outside facts were verified",
    ),
  );
});

test("prep facts remove citation clutter without deleting the underlying explanation", () => {
  const fact =
    "Limrun runs `xcodebuild` remotely and streams logs. ([docs.limrun.com](https://docs.limrun.com/docs/ios/build-with-xcode?utm_source=openai))";
  assert.equal(
    readableJobPrepText(fact),
    "Limrun runs xcodebuild remotely and streams logs.",
  );
});

test("deep-dive PDF keeps company history, headcount, four questions and research gaps", () => {
  const fact = {
    text: "2025; month not verified",
    sourceUrl: "https://example.com/about",
    sourceTitle: "About",
  };
  const sheet = {
    study: [],
    prepare: [],
    facts: [],
    company: {
      name: "Example",
      founders: { ...fact, text: "Example founder" },
      founded: fact,
      bio: { ...fact, text: "A short company bio" },
      employees: { ...fact, text: "10–20 employees; date not stated" },
    },
    interviewQuestions: Array.from({ length: 4 }, (_, i) => ({
      question: `Question ${i + 1}`,
      whyLikely: "Related to the role",
      howToPrepare: "Use a real example",
      basis: "Inferred from posting",
      sources: [],
    })),
    researchLimits: "No public role reviews found",
  };
  const text = jobPrepDocumentText(sheet);
  for (const value of [
    "Example founder",
    "2025; month not verified",
    "A short company bio",
    "10–20 employees",
    "Question 1",
    "Question 4",
    "No public role reviews found",
    "https://example.com/about",
  ])
    assert.ok(text.includes(value), value);
});
