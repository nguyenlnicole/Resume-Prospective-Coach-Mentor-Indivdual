import test from "node:test";
import assert from "node:assert/strict";
import {
  projectLinkFieldValues,
  combineProjectLinkFields,
  mergeProjectLinkAutofill,
  preserveProjectHeadings,
  projectLinksFromEdits,
} from "./project-link-fields.js";
test("separates repositories and demos and preserves multiple destinations", () => {
  const value =
    "https://github.com/person/client | https://person.github.io/client | https://github.com/person/server";
  const fields = projectLinkFieldValues(value);
  assert.equal(
    fields.repo,
    "https://github.com/person/client | https://github.com/person/server",
  );
  assert.equal(fields.demo, "https://person.github.io/client");
  assert.deepEqual(
    new Set(combineProjectLinkFields(fields).split(" | ")),
    new Set(value.split(" | ")),
  );
});
test("both optional project link fields may be blank or cleared", () => {
  assert.equal(combineProjectLinkFields({ repo: "", demo: "" }), "");
  assert.deepEqual(projectLinkFieldValues(""), { repo: "", demo: "" });
  assert.equal(
    combineProjectLinkFields({ repo: "", demo: "https://demo.example" }),
    "https://demo.example",
  );
});

test("GitHub autofill fills gaps without replacing a user link or refilling a cleared field", () => {
  assert.equal(
    mergeProjectLinkAutofill("https://my-demo.example", [
      "https://github.com/me/app",
      "https://other-demo.example",
    ]),
    "https://github.com/me/app | https://my-demo.example",
  );
  assert.equal(
    mergeProjectLinkAutofill("", ["https://github.com/me/app"], true),
    "",
  );
  assert.equal(
    mergeProjectLinkAutofill("", [
      "https://github.com/me/app",
      "https://app.example",
    ]),
    "https://github.com/me/app | https://app.example",
  );
});

test("customization and alternative wording keep supplied project links beside matching names", () => {
  const original =
    "Virus Blasters | https://game.example | https://github.com/me/virus — C#\nOriginal description.\n\nTriller | https://github.com/me/triller — Angular\nOriginal description.";
  const rewritten =
    "Virus Blasters — C#\nReworded description.\n\nTriller — Angular\nOther rewording.\n\nUnrelated\nDo not attach another project’s links.";
  const result = preserveProjectHeadings(original, rewritten);
  assert.ok(
    result.includes(
      "Virus Blasters | https://game.example | https://github.com/me/virus — C#\nReworded description.",
    ),
  );
  assert.ok(
    result.includes(
      "Triller | https://github.com/me/triller — Angular\nOther rewording.",
    ),
  );
  assert.ok(!result.split("Unrelated")[1].includes("https://"));
});

test("edited links stay with project names when returning to customization", () => {
  const original = "First project\nOriginal.\n\nSecond project\nOriginal.";
  const edited =
    "Second project | https://second.example\nEdited.\n\nFirst project | https://github.com/me/first\nEdited.";
  assert.deepEqual(projectLinksFromEdits(original, edited), {
    0: "https://github.com/me/first",
    1: "https://second.example",
  });
  assert.deepEqual(
    projectLinksFromEdits(original, "First project\nNo links."),
    { 0: "" },
  );
});
