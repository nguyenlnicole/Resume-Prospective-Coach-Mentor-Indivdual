import test from "node:test";
import assert from "node:assert/strict";
import { saveLatestWorkflow } from "./workflow-save.js";

test("saves edits made while the previous version is being saved", async () => {
  let current = { snapshot: "original", file: {} };
  const saved = [];
  await saveLatestWorkflow(
    () => current,
    async (item) => {
      if (item.snapshot === "original")
        current = { ...current, snapshot: "updated" };
      return item.snapshot;
    },
    (item, result) => saved.push(result),
  );
  assert.deepEqual(saved, ["original", "updated"]);
});

test("does not report a failed update as saved", async () => {
  const saved = [];
  await assert.rejects(
    saveLatestWorkflow(
      () => ({ snapshot: "updated", file: null }),
      async () => {
        throw new Error("Save failed");
      },
      (item) => saved.push(item),
    ),
    /Save failed/,
  );
  assert.deepEqual(saved, []);
});

test("saves a replaced file even when the snapshot is unchanged", async () => {
  const first = {},
    second = {};
  let current = { snapshot: "same", file: first };
  const saved = [];
  await saveLatestWorkflow(
    () => current,
    async (item) => {
      current = { snapshot: "same", file: second };
    },
    (item) => saved.push(item.file),
  );
  assert.deepEqual(saved, [first, second]);
});
