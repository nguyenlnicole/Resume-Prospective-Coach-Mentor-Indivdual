import test from "node:test";
import assert from "node:assert/strict";
import { showFeatures } from "./features-access.js";

test("features are available to public individual, coach and embedded editions", () => {
  for (const hostname of [
    "resumep.pnbpaws.com",
    "crp.pnbpaws.com",
    "localhost",
  ]) {
    assert.equal(showFeatures({ publicEdition: true, hostname }), true);
  }
});
test("features exclude admin, private builds", () => {
  assert.equal(
    showFeatures({ publicEdition: true, hostname: "adminrp.pnbpaws.com" }),
    false,
  );
  assert.equal(showFeatures({ publicEdition: true, role: "admin" }), false);
  assert.equal(showFeatures({ publicEdition: false }), false);
});
