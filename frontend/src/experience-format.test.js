import test from 'node:test';
import assert from 'node:assert/strict';
import { formatExperience } from './experience-format.js';

test('promotions use full months, newest role first, and keep each role and its bullets together', () => {
  const source = 'Example Company\nSoftware Engineer | Aug 2018 – Aug 2020\n• Implemented updates.\n• Wrote unit tests.\nSoftware Engineer II | Aug 2020 – Aug 2022\n• Owned eight API features.\n• Reduced tickets by 35%.';
  assert.equal(formatExperience(source), 'Example Company\nSoftware Engineer II | August 2020 – August 2022\n• Owned eight API features.\n• Reduced tickets by 35%.\nSoftware Engineer | August 2018 – August 2020\n• Implemented updates.\n• Wrote unit tests.');
});

test('separate blocks of the same employer merge without changing job titles', () => {
  assert.equal(formatExperience('Example Company\nEngineer I | 2018 – 2020\n• First.\n\nExample Company\nEngineer II | 2020 – Present\n• Second.'), 'Example Company\nEngineer II | 2020 – Present\n• Second.\nEngineer I | 2018 – 2020\n• First.');
});

test('formatting does not invent dates or alter dates mentioned in bullets', () => {
  const text = 'Example Company — Engineer | 2020 – Present\n• Shipped in Aug 2021.\n\nVolunteer role, dates not supplied';
  assert.equal(formatExperience(text), text);
});