import test from "node:test";
import assert from "node:assert/strict";
import { inspectGithubRepository } from "./github-repo.js";

test("a supplied project repo yields languages and manifest technologies even without a profile lookup", async (t) => {
  t.mock.method(globalThis, "fetch", async (url) => {
    assert.ok(url.startsWith("https://api.github.com/repos/example/project/"));
    if (url.endsWith("/languages"))
      return { ok: true, json: async () => ({ Java: 100, JavaScript: 80 }) };
    if (url.endsWith("/pom.xml"))
      return {
        ok: true,
        json: async () => ({
          size: 40,
          content: btoa("<artifactId>spring-boot</artifactId>"),
        }),
      };
    if (url.endsWith("/package.json"))
      return {
        ok: true,
        json: async () => ({
          size: 40,
          content: btoa('{"dependencies":{"react":"19"}}'),
        }),
      };
    return { ok: false };
  });
  assert.deepEqual(
    await inspectGithubRepository("https://github.com/example/project"),
    ["Java", "JavaScript", "React", "Spring Boot"],
  );
});

test("unreadable repos report a recoverable error instead of inventing technologies", async (t) => {
  t.mock.method(globalThis, "fetch", async () => ({ ok: false, status: 404 }));
  await assert.rejects(
    inspectGithubRepository("https://github.com/example/private"),
    /could not be read/,
  );
});
