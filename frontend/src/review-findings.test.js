import test from 'node:test';
import assert from 'node:assert/strict';
import { visibleReviewFindings } from './review-findings.js';

test('a fully passing second rating still shows its feedback', () => {
  const findings = [{ id: 'experience', status: 'pass', evidence: 'Relevant experience is documented.' }];
  assert.deepEqual(visibleReviewFindings(findings), findings);
});
test('mixed ratings show improvements first without hiding passing feedback', () => {
  const pass = { id: 'skills', status: 'pass' };
  const missing = { id: 'experience', status: 'missing' };
  assert.deepEqual(visibleReviewFindings([pass, missing]), [missing, pass]);
  assert.deepEqual(visibleReviewFindings([pass, missing], true), [pass, missing]);
});
test('older reviews without findings do not crash the review page', () => {
  for (const value of [undefined, null, {}, [null]]) assert.deepEqual(visibleReviewFindings(value), []);
});
