import test from "node:test";
import assert from "node:assert/strict";
import { PDFDocument } from "pdf-lib";
import {
  demoResume,
  demoFinishedResume,
  demoReview,
  demoRating,
  demoProjectLinks,
} from "./demo-sample-candidate.js";
import { demoRequest } from "./demo-mode.js";
import { validateResume, projectList, projectLinkUrls } from "./rules.js";
import { createDemoResumePdf } from "./demo-resume-pdf.js";

test("Fictional sample keeps source facts and exports the requested one-page layout with six project links", async () => {
  assert.equal(demoFinishedResume.contact.email, "alex.rivera@example.com");
  assert.equal(demoFinishedResume.experience, demoResume.experience);
  assert.match(demoFinishedResume.education, /Example Institute.*May 2017/);
  assert.deepEqual(validateResume(demoFinishedResume, "Senior"), []);
  assert.deepEqual(
    projectList(demoFinishedResume.projects).map(
      (p) => projectLinkUrls(p).length,
    ),
    [2, 2, 2],
  );
  const blob = await createDemoResumePdf(demoFinishedResume);
  assert.equal(
    (await PDFDocument.load(await blob.arrayBuffer())).getPageCount(),
    1,
  );
});
test("demo uses the requested job and cover letter without allowing account or email mutations", async () => {
  assert.match(
    (await demoRequest("/job-description/link")).text,
    /Founding Product Engineer/,
  );
  assert.match((await demoRequest("/cover-letter")).text, /Alex Rivera/);
  for (const path of [
    "/access/signup",
    "/access/trial",
    "/resume/email",
    "/review/owner-copy",
  ])
    await assert.rejects(demoRequest(path), /does not send emails/);
});
test("demo ratings use all twenty rubric checks and retain uncertainty rather than fixed scores", () => {
  assert.equal(demoReview.formatFindings.length, 10);
  assert.equal(demoReview.contentFindings.length, 10);
  assert.equal(demoReview.formatRating.unverified, 3);
  assert.equal(
    demoReview.formatScore,
    demoRating(demoReview.formatFindings).value,
  );
  assert.equal(
    demoReview.overallScore,
    Math.round(
      (demoReview.formatScore * 0.4 +
        demoRating(demoReview.contentFindings).value * 0.6) *
        10,
    ) / 10,
  );
  for (const f of [
    ...demoReview.formatFindings,
    ...demoReview.contentFindings,
  ]) {
    assert.ok(f.evidence.length > 80);
    assert.ok(f.suggestion.length > 40);
  }
  assert.equal(demoProjectLinks.length, 3);
  assert.ok(demoProjectLinks.every((links) => links.split(" | ").length === 2));
});
