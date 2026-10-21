import test from 'node:test';
import assert from 'node:assert/strict';
import {suggestProjects} from './project-suggestions.js';
const repo=(name,extra={})=>({name,html_url:`https://github.com/example/${name}`,description:'A public project',size:10,...extra});
test('discovers projects with no existing projects or target job',()=>{
  assert.equal(suggestProjects([], [repo('first'),repo('second')], '').length,2);
});
test('fills missing projects without requiring a better keyword score',()=>{
  const result=suggestProjects(['Existing | https://github.com/example/existing'],[repo('existing'),repo('new')],'');
  assert.deepEqual(result.map(item=>item.name),['new']);
  assert.equal(result[0].replaceIndex,null);
});
test('excludes archived, empty, template and fork repos and unsafe links',()=>{
  assert.deepEqual(suggestProjects([], [repo('fork',{fork:true}),repo('old',{archived:true}),repo('template',{is_template:true}),repo('empty',{size:0}),repo('unsafe',{html_url:'javascript:alert(1)'})],''),[]);
});
test('complete project lists only get stronger job matches',()=>{
  const projects=['First Java','Second Python','Third Java'];
  const result=suggestProjects(projects,[repo('react',{description:'React API'}),repo('other')],'React API');
  assert.equal(result.length,1);
  assert.equal(result[0].name,'react');
  assert.notEqual(result[0].replaceIndex,null);
});