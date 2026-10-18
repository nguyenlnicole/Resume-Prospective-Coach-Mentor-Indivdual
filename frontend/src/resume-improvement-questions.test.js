import test from "node:test";
import assert from "node:assert/strict";
import { resumeImprovementQuestions } from "./resume-improvement-questions.js";
test("profession questions focus on actual work and only retain coding questions with section evidence", () => {
  const generated = {
    id: "stack",
    section: "projects",
    question: "Which programming frameworks did you use?",
    hint: "Name the frameworks.",
    required: true,
  };
  for (const level of ["General", "Medical Field", "Educator"]) {
    const questions = resumeImprovementQuestions(
      {
        questions: [generated],
        resume: {
          experience: "Office assistant",
          projects: "Community volunteer event",
        },
      },
      level,
    );
    assert.ok(!questions.some((q) => q.id === "stack"));
    assert.ok(
      !questions.some((q) => /real user need|adoption/.test(q.hint || "")),
    );
  }
  const educator = resumeImprovementQuestions(
    { questions: [], resume: { experience: "Tutor" } },
    "Educator",
  );
  assert.ok(educator.slice(0, 4).some((q) => q.id === "improve_learning"));
  const medical = resumeImprovementQuestions(
    { questions: [], resume: { experience: "Medical receptionist" } },
    "Medical Field",
  );
  assert.ok(medical.slice(0, 4).some((q) => q.id === "improve_care"));
  assert.ok(
    resumeImprovementQuestions(
      {
        questions: [generated],
        resume: { projects: "Built a Python teaching tool" },
      },
      "Educator",
    ).some((q) => q.id === "stack"),
  );
  assert.ok(
    !resumeImprovementQuestions(
      {
        questions: [generated],
        resume: { skills: "Python", projects: "Campus volunteering" },
      },
      "Educator",
    ).some((q) => q.id === "stack"),
  );
});
test("adds optional useful questions without duplicating an existing outcome question", () => {
  const questions = resumeImprovementQuestions(
    {
      questions: [
        {
          id: "impact",
          question: "What outcome did your work achieve?",
          hint: "Give real facts",
          required: false,
        },
      ],
      resume: { experience: "Example — Analyst", projects: "Report builder" },
    },
    "Junior",
  );
  assert.ok(questions.length > 3);
  assert.ok(questions.length <= 10);
  assert.equal(questions.filter((q) => q.id === "improve_impact").length, 0);
  assert.ok(
    questions
      .filter((q) => q.id.startsWith("improve_"))
      .every((q) => q.required === false),
  );
  assert.ok(questions.some((q) => q.id === "improve_project_problem"));
});
test("does not ask about employment or projects absent from the resume", () => {
  assert.ok(
    resumeImprovementQuestions({ questions: [], resume: {} }, "Junior").every(
      (q) => q.section === "certifications",
    ),
  );
  const questions = resumeImprovementQuestions(
    { questions: [], resume: { experience: "Teacher" } },
    "Educator",
  );
  assert.ok(questions.some((q) => q.id === "improve_learning"));
  assert.ok(!questions.some((q) => q.section === "projects"));
});

test("asks optional certification and course questions even without employment, without duplicating existing questions", () => {
  const questions = resumeImprovementQuestions(
    { questions: [], resume: {} },
    "General",
  );
  assert.deepEqual(
    questions.map((q) => q.id),
    ["improve_certifications", "improve_courses"],
  );
  assert.ok(questions.every((q) => q.required === false));
  assert.ok(questions.every((q) => q.hint.includes("Dates are optional")));
  assert.ok(questions.every((q) => q.hint.includes("in progress")));
  const existing = [
    {
      id: "credentials",
      question: "Which certifications and courses would you like to add?",
      required: false,
    },
  ];
  const deduplicated = resumeImprovementQuestions(
    { questions: existing, resume: {} },
    "General",
  );
  assert.deepEqual(deduplicated, existing);
  const many = Array.from({ length: 10 }, (_, index) => ({
    id: `original_${index}`,
    question: `Existing question ${index}`,
    required: false,
  }));
  const crowded = resumeImprovementQuestions(
    { questions: many, resume: {} },
    "General",
  );
  assert.ok(crowded.some((q) => q.id === "improve_certifications"));
  assert.ok(crowded.some((q) => q.id === "improve_courses"));
});
