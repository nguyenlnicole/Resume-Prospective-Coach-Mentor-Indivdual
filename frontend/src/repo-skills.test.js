import test from 'node:test';
import assert from 'node:assert/strict';
import { collectRepositorySkills, addRepositorySkills, retainSkills } from './repo-skills.js';

test('building waits for repository languages even before background lookup finishes', async () => {
  let release;
  const gate = new Promise(resolve => { release = resolve; });
  let finished = false;
  const result = collectRepositorySkills(['https://github.com/example/shopping'], async () => {
    await gate;
    return ['JavaScript', 'HTML', 'React'];
  }).then(value => { finished = true; return value; });
  await Promise.resolve();
  assert.equal(finished, false);
  release();
  assert.deepEqual((await result).technologies, ['JavaScript', 'HTML', 'React']);
});

test('fresh or restored builds inspect supplied repositories and report partial failures', async () => {
  const calls = [];
  const result = await collectRepositorySkills([
    'https://github.com/example/app', 'https://github.com/EXAMPLE/app.git',
    'https://github.com/example/new-project', 'https://github.com/example/unavailable',
    'https://github.com/example', 'https://example.com/demo',
  ], async url => {
    calls.push(url);
    if (url.endsWith('/unavailable')) throw Error('Rate limited');
    return url.endsWith('/new-project') ? ['Swift'] : ['JavaScript'];
  });
  assert.equal(calls.length, 3);
  assert.deepEqual(result.technologies, ['JavaScript', 'Swift']);
  assert.deepEqual(result.failed, ['https://github.com/example/unavailable']);
});

test('existing language aliases are not duplicated by repository evidence', () => {
  assert.deepEqual(addRepositorySkills('Frameworks: React.js, Express.js, Node.js\nLanguages: T-SQL',
    ['React', 'Express', 'Nodejs', 'TSQL', 'HTML']).added, ['HTML']);
});

test('build and alternative wording keep supplied and discovered skill details', () => {
  const original = 'Languages: JavaScript, HTML\nTools: Xcode, GitHub Copilot\nProject technologies: Swift';
  assert.equal(retainSkills(original, 'Languages: JavaScript'), original);
  assert.equal(retainSkills(original, undefined), original);
  const reorganized = 'Languages: JavaScript, HTML, Swift\nTools: GitHub Copilot, Xcode';
  assert.equal(retainSkills(original, reorganized), reorganized);
});
