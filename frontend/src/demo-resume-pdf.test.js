import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { PDFDocument, PDFName } from "pdf-lib";
import { createDemoResumePdf } from "./demo-resume-pdf.js";

test("offline demo export stays one page and preserves every contact and project link", async () => {
  const resume = JSON.parse(
    await readFile(
      new URL(
        "../../backend/src/test/resources/approved-reference.json",
        import.meta.url,
      ),
      "utf8",
    ),
  );
  const blob = await createDemoResumePdf(resume);
  const doc = await PDFDocument.load(await blob.arrayBuffer());
  assert.equal(doc.getPageCount(), 1);
  const links = doc
    .getPage(0)
    .node.Annots()
    .asArray()
    .map((ref) =>
      doc.context
        .lookup(ref)
        .lookup(PDFName.of("A"))
        .lookup(PDFName.of("URI"))
        .decodeText(),
    );
  for (const url of [
    "mailto:jordan@example.com",
    "https://www.linkedin.com/in/example",
    "https://github.com/example",
    "https://example.com",
    "https://github.com/example/schedule",
    "https://github.com/example/reports",
    "https://github.com/example/library",
  ])
    assert.ok(links.includes(url), url);
});
