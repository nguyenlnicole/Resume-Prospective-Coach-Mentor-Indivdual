import test from 'node:test';
import assert from 'node:assert/strict';
import { newExperienceError, removeOldestExperience, experienceWordCount, shouldReplaceOldestExperience } from './new-experience.js';

test('new experience requires ten words and ordered dates', () => {
  const value = { enabled: true, role: 'Teacher', employer: 'School', description: 'word '.repeat(50), start: '2024-08', current: true };
  assert.equal(newExperienceError(value), '');
  assert.match(newExperienceError({ ...value, description: 'Helped customers.' }), /at least 10 words/);
  assert.equal(newExperienceError({ ...value, description: 'I taught students reading daily and prepared their weekly lessons.' }), '');
  assert.match(newExperienceError({ ...value, description: 'word '.repeat(9) }), /at least 10 words/);
  assert.match(newExperienceError({ ...value, description: ' ' }), /at least 10 words/);
  assert.match(newExperienceError({ ...value, current: false, end: '2023-08' }), /end date/);
  assert.equal(newExperienceError({ enabled: false }), '');
});

test('removes the oldest completed role, retaining its newer promotion and current work', () => {
  const text = 'School\nTeacher | August 2020 – August 2022\n• Taught.\nAssistant | August 2018 – August 2020\n• Assisted.\n\nClinic — Coordinator | January 2023 – Present\n• Scheduled visits.';
  const result = removeOldestExperience(text);
  assert.match(result, /Teacher/);
  assert.match(result, /Coordinator/);
  assert.doesNotMatch(result, /Assistant|Assisted/);
});

test('does not guess at the age of undated or current experience', () => {
  const text = 'School — Teacher | Present\n• Taught.';
  assert.equal(removeOldestExperience(text), text);
});

test('each of two new roles needs its own complete details', () => {
  const role = {enabled:true,role:'Teacher',employer:'School',description:'word '.repeat(50),start:'2024-08',current:true};
  assert.equal(newExperienceError({...role,secondRole:{...role}}), '');
  assert.match(newExperienceError({...role,secondRole:{...role,description:'Scheduled appointments.'}}), /at least 10 words/);
  assert.match(newExperienceError({...role,secondRole:{...role,description:''}}), /at least 10 words/);
  assert.equal(newExperienceError({...role,secondRole:{enabled:false}}), '');
});

test('counts job descriptions without copied headings, dates or location', () => {
 assert.equal(experienceWordCount('Engineer\nCompany Name\nMay 2026 - Present\nColumbus, Ohio · On-site\nI trained models.'),3);
 assert.equal(experienceWordCount('   '),0);
});

test('adding roles preserves existing jobs unless replacement is explicitly selected', () => {
  for (const role of [undefined, {enabled:true}, {enabled:true,replaceOldest:false}, {enabled:false,replaceOldest:true}]) {
    assert.equal(shouldReplaceOldestExperience(role), false);
  }
  assert.equal(shouldReplaceOldestExperience({enabled:true,replaceOldest:true}), true);
});
