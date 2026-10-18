import test from "node:test";
import assert from "node:assert/strict";
import { credentialParts, normalizeCredentials } from "./credential-format.js";
test("certification issue and expiration dates survive normalization", () => {
  const source = "Certification, Issuer | 2019 – Expires 2029";
  assert.equal(normalizeCredentials(source), source);
  assert.equal(credentialParts(source).date, "2019 – Expires 2029");
  assert.equal(
    normalizeCredentials("Certification | Expires 2029"),
    "Certification | Expires 2029",
  );
});
test("certifications and training preserve provider and one supplied date", () => {
  assert.deepEqual(
    credentialParts(
      "Professional Scrum Master (PSM I), _Scrum.org |&#x20;_**January 2024&#x20;**",
    ),
    {
      name: "Professional Scrum Master (PSM I)",
      issuer: "Scrum.org",
      date: "January 2024",
    },
  );
  const source =
    "Professional Scrum Master (PSM I), Scrum.org | January 2024\nAWS Cloud Bootcamp, Thinkcloudly | September 2026";
  assert.equal(normalizeCredentials(source), source);
  assert.equal(normalizeCredentials(normalizeCredentials(source)), source);
  assert.equal(
    normalizeCredentials("PSM I – Scrum.org | January 2024"),
    "PSM I, Scrum.org | January 2024",
  );
  assert.deepEqual(credentialParts("Training course"), {
    name: "Training course",
    issuer: "",
    date: "",
  });
  assert.deepEqual(credentialParts("Training course | 2024"), {
    name: "Training course",
    issuer: "",
    date: "2024",
  });
});
