import test from 'node:test';
import assert from 'node:assert/strict';
import { githubProfileFromProjects } from './github-profile.js';
test('suggests the common repository owner', () => {
 assert.equal(githubProfileFromProjects('Qwen | https://github.com/Albatross679/qwen3-text-to-sql'), 'https://github.com/Albatross679');
 assert.equal(githubProfileFromProjects('https://github.com/person/one https://github.com/person/two'), 'https://github.com/person');
});
test('does not guess between owners or use unrelated hosts', () => {
 assert.equal(githubProfileFromProjects('https://github.com/one/repo https://github.com/two/repo'), '');
 assert.equal(githubProfileFromProjects('https://github.com.evil.test/person/repo'), '');
 assert.equal(githubProfileFromProjects('https://github.com/topics/python'), '');
});
