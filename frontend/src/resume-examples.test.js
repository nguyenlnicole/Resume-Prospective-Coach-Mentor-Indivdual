import test from "node:test";
import assert from "node:assert/strict";
import { resumeExampleFor } from "./resume-examples.js";
test("each profession and technical level has a distinct example", () => {
  const categories = [
    "General",
    "Medical Field",
    "Educator",
    "Entry / Junior",
    "Mid-level",
    "Senior",
    "Management",
  ];
  const examples = categories.map((category, index) =>
    resumeExampleFor(index < 3 ? category : "Technical", category),
  );
  assert.equal(
    new Set(examples.map((example) => example.resume.summary)).size,
    categories.length,
  );
  examples.forEach((example, index) => {
    assert.equal(example.level, categories[index]);
    assert.equal(example.resume.softwareEngineering, index >= 3);
    assert.ok(
      example.resume.summary &&
        example.resume.experience &&
        example.resume.skills,
    );
  });
  for (const example of examples.slice(0, 3)) {
    assert.equal(example.resume.contact.github, "");
    assert.equal(example.resume.projects, "");
  }
  assert.match(examples[1].resume.experience, /patient/);
  assert.match(examples[2].resume.experience, /instruction/);
  assert.equal(resumeExampleFor("Normal").resume, examples[0].resume);
  assert.match(examples[0].resume.experience, /Clearwell Customer Services/);
  assert.match(examples[1].resume.experience, /Medical Assistant/);
  assert.match(examples[2].resume.certifications, /Expires June 2029/);
});
