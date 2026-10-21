import test from 'node:test';
import assert from 'node:assert/strict';
import { fetchProjectDetails, newProjectsError, addNewProjects, readmeOverview, publicFileOverview } from './new-projects.js';

const project = { name:'Planner', description:'Plans appointments.', contribution:'Built calendar views.', stack:'React', onGithub:'no', confirmed:true };
test('projects accept unconfirmed details and optional contributions and technologies', () => {
  assert.equal(newProjectsError([project]), '');
  assert.equal(newProjectsError([{...project,confirmed:false}]), '');
  assert.equal(newProjectsError([{...project,confirmed:undefined}]), '');
  assert.equal(newProjectsError([{...project,contribution:'',stack:''}]), '');
  assert.doesNotMatch(addNewProjects('', [{...project,contribution:undefined,stack:undefined}]), /undefined/);
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

test('README description ignores nested badges, contact links, and setup', () => {
  const text = '# Project\n\n[![Email](badge.svg)](mailto:person@example.com)\n[![GitHub](badge.svg)](https://github.com/person)\n\n## Overview\n\nThis application helps local teams schedule bookings and share available appointments.\n\n## Installation\n\nRun these commands to install and configure the application on your computer.';
  assert.equal(readmeOverview(text), 'This application helps local teams schedule bookings and share available appointments.');
  assert.equal(readmeOverview('[![Email](badge.svg)](mailto:person@example.com)'), '');
});
test('profile README repositories are not imported as projects', async () => {
  await assert.rejects(fetchProjectDetails('https://github.com/person/person'), /profile README/);
});

test('public HTML fallback describes the page without inventing checkout functionality', () => {
  const result=publicFileOverview('index.html','<title>Library</title><h1>Find your dream book</h1><h2>Featured Books</h2><script>checkout()</script>');
  assert.match(result,/Library/); assert.match(result,/Featured Books/); assert.doesNotMatch(result,/checkout/);
});
test('missing README falls back to bounded public file inspection', async t => {
  t.mock.method(globalThis,'fetch',async url=>{
    if(url.endsWith('/repos/person/books')) return {ok:true,json:async()=>({name:'Books'})};
    if(url.endsWith('/git/trees/HEAD?recursive=1')) return {ok:true,json:async()=>({tree:[{type:'blob',path:'index.html',size:90}]})};
    if(url.endsWith('/contents/index.html')) return {ok:true,json:async()=>({encoding:'base64',size:90,content:btoa('<title>Library</title><h1>Browse books</h1>')})};
    return {ok:false};
  });
  const result=await fetchProjectDetails('https://github.com/person/books');
  assert.match(result.description,/Library/);assert.equal(result.confirmed,false);
});
