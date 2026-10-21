import test from 'node:test';
import assert from 'node:assert/strict';
import { fetchProjectDetails, newProjectsError, addNewProjects } from './new-projects.js';

const project = { name:'Planner', description:'Plans appointments.', contribution:'Built calendar views.', stack:'React', onGithub:'no', confirmed:true };
test('manual projects require contribution and confirmation, not GitHub', () => {
  assert.equal(newProjectsError([project]), '');
  assert.match(newProjectsError([{...project,confirmed:false}]), /Confirm/);
  assert.match(newProjectsError([{...project,contribution:''}]), /contribution/);
  assert.match(newProjectsError([{...project,onGithub:'yes',repo:'https://example.com/repo'}]), /repository/);
});
test('new projects are appended without removing existing work', () => {
  assert.equal(addNewProjects('Original', [project]), 'Original\n\nPlanner — React\n• Plans appointments.\n• Built calendar views.');
});
test('GitHub fills real metadata and technologies but never confirms ownership', async t => {
  t.mock.method(globalThis,'fetch',async url => {
    if (url.endsWith('/repos/person/planner')) return {ok:true,json:async()=>({name:'Planner',description:'Plans appointments.',homepage:'javascript:alert(1)'})};
    if (url.endsWith('/languages')) return {ok:true,json:async()=>({JavaScript:100})};
    return {ok:false};
  });
  const result = await fetchProjectDetails('https://github.com/person/planner');
  assert.equal(result.name, 'Planner');
  assert.equal(result.stack, 'JavaScript');
  assert.equal(result.demo, '');
  assert.equal(result.confirmed, false);
  assert.equal(result.contribution, '');
});
test('private repositories allow a clear manual fallback',async t => {
  t.mock.method(globalThis,'fetch',async()=>({ok:false}));
  await assert.rejects(fetchProjectDetails('https://github.com/person/private'),/enter the details yourself/);
});

test('README and verified author commits fill editable project details', async t => {
  t.mock.method(globalThis, 'fetch', async url => {
    if (url.endsWith('/repos/person/planner')) return {ok:true,json:async()=>({name:'Planner'})};
    if (url.endsWith('/readme')) return {ok:true,json:async()=>({encoding:'base64',size:100,content:btoa('# Planner\n\nA calendar application that helps teams schedule appointments and manage bookings.')})};
    if (url.includes('/commits?')) return {ok:true,json:async()=>[
      {author:{login:'person'},commit:{message:'Added calendar booking views'}},
      {author:{login:'someone-else'},commit:{message:'Built payment processing'}},
    ]};
    return {ok:false};
  });
  const result = await fetchProjectDetails('https://github.com/person/planner');
  assert.match(result.description, /calendar application/);
  assert.match(result.contribution, /Added calendar booking views/);
  assert.doesNotMatch(result.contribution, /payment/);
  assert.equal(result.confirmed, false);
  assert.match(result.importNote, /personally/);
});
