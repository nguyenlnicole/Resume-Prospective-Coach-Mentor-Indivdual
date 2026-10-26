import test from "node:test";
import assert from "node:assert/strict";
import { demoRequest } from "./demo-mode.js";

test("demo roast and glow-up have distinct complete responses", async () => {
  const roast = await demoRequest("/feedback/roast");
  const glow = await demoRequest("/feedback/glow-up-roast");
  assert.ok(roast.roast.split("\n\n").length >= 2);
  assert.equal(glow.jokes.length, 3);
  assert.ok(glow.jokes.every((joke) => joke.trim().length > 30));
  assert.ok(glow.jokes.every((joke) => !roast.roast.includes(joke)));
});

test("demo still blocks email and account mutations", async () => {
  for (const route of ["/delivery", "/accounts/signup", "/coach/verify"]) {
    await assert.rejects(demoRequest(route), /demo uses sample data/);
  }
});
