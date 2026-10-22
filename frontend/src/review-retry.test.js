import test from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';

test('failed review preserves the file preview and exposes the error and retry', async () => {
  const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] }, server: { middlewareMode: true }, appType: 'custom' });
  try {
    const { default: Preview } = await server.ssrLoadModule('/src/UploadedResumePreview.jsx');
    const html = renderToStaticMarkup(React.createElement(Preview, {
      file: { name: 'my-resume.pdf' }, reviewError: 'The review service timed out.',
      onBack() {}, onReview() {},
    }));
    assert.match(html, /my-resume.pdf/);
    assert.match(html, /role="alert"/);
    assert.match(html, /The review service timed out\./);
    assert.match(html, /Your resume is still attached/);
    assert.match(html, /Try Review Again/);
  } finally { await server.close(); }
});
