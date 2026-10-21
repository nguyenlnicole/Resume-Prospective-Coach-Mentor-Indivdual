import test from 'node:test';
import assert from 'node:assert/strict';
import { newExperienceError, removeOldestExperience } from './new-experience.js';

test('new experience requires fifty words and ordered dates', () => {
  const value = { enabled: true, role: 'Teacher', employer: 'School', description: 'word '.repeat(50), start: '2024-08', current: true };
  assert.equal(newExperienceError(value), '');
  assert.match(newExperienceError({ ...value, description: 'word '.repeat(49) }), /50 words/);
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
  assert.match(newExperienceError({...role,secondRole:{...role,description:'Short'}}), /50 words/);
  assert.equal(newExperienceError({...role,secondRole:{enabled:false}}), '');
});
