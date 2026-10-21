import test from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';

test('editor renders project suggestions for long descriptions without crashing', async () => {
  const server = await createServer({ configFile: false, server: { middlewareMode: true }, appType: 'custom' });
  try {
    const { default: Suggestion } = await server.ssrLoadModule('/src/ProjectWordingSuggestion.jsx');
    const html = renderToStaticMarkup(React.createElement(Suggestion, {
      value: 'Project A\n' + 'Implemented documented software changes for users. '.repeat(10), onChange: () => {},
    }));
    assert.match(html, /Improve project wording/);
    assert.match(renderToStaticMarkup(React.createElement(Suggestion, { value: 'Project A\nBuilt a tool.', onChange: () => {} })), /Improve project wording/);
  } finally { await server.close(); }
});
