import test from "node:test";
import assert from "node:assert/strict";
import { addRepositorySkills, newlyListedSkills } from "./repo-skills.js";
import { suggestProjects } from "./project-suggestions.js";
test("newly listed skills are highlighted across professions without duplicate aliases", () => {
  assert.deepEqual(
    newlyListedSkills(
      "Skills: Scheduling, Excel",
      "Office Skills: Scheduling, Excel, Insurance verification\nSystems: Epic",
    ),
    ["Insurance verification", "Epic"],
  );
  assert.deepEqual(
    newlyListedSkills("Languages: JS", "Languages: JavaScript, Python, Python"),
    ["Python"],
  );
});
test("repo skills add only new technologies and preserve original skills", () => {
  const result = addRepositorySkills("Languages: Java, JS\nFrameworks: React", [
    "Java",
    "JavaScript",
    "React",
    "Spring Boot",
    "PostgreSQL",
    "Spring Boot",
  ]);
  assert.deepEqual(result.added, ["Spring Boot", "PostgreSQL"]);
  assert.equal(
    result.skills,
    "Languages: Java, JS\nFrameworks: React\nProject technologies: Spring Boot, PostgreSQL",
  );
  assert.deepEqual(
    addRepositorySkills(result.skills, ["Spring Boot"]).added,
    [],
  );
});
test("suggestions require stronger documented relevance and exclude forks and already-listed projects", () => {
  const candidate = {
    name: "portal",
    description: "React frontend and Java API",
    language: "Java",
    size: 12,
    html_url: "https://github.com/person/portal",
  };
  const projects = ["Calculator — Java\nBuilt a calculator."];
  const result = suggestProjects(
    projects,
    [
      candidate,
      { ...candidate, name: "fork", fork: true },
      { ...candidate, name: "empty", size: 0 },
    ],
    "Java React API",
  );
  assert.equal(result.length, 1);
  assert.equal(result[0].name, "portal");
  assert.equal(result[0].replaceIndex, 0);
  assert.deepEqual(suggestProjects(projects, [candidate], "Java"), []);
  assert.deepEqual(
    suggestProjects(["Portal — Java"], [candidate], "React API"),
    [],
  );
});
