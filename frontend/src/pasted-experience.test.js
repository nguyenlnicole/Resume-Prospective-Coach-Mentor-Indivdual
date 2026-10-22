import test from 'node:test';
import assert from 'node:assert/strict';
import { pastedExperience, cleanExperienceDescription, formatExperienceMonth } from './pasted-experience.js';
test('reads copied LinkedIn formatting, dates and all bullets', () => {
 const result = pastedExperience('**Machine Learning Engineer**\nRisk Assessment and Management of Structural and Infrastructure System Lab · Part-time\nMay 2026 - Aug 2026 · 4 mos\nColumbus, Ohio Metropolitan Area · On-site\n\\- Trained MLP, CNN, and U-Net models, achieving F1 of 0.90.\\\n\n\\- Generated 5,000 signals.\\\n\n\\- Screened 12,423 signals and added CI/CD.');
 assert.equal(result.role,'Machine Learning Engineer');
 assert.equal(result.employer,'Risk Assessment and Management of Structural and Infrastructure System Lab');
 assert.equal(result.start,'2026-05'); assert.equal(result.end,'2026-08'); assert.equal(result.current,false);
 assert.equal(result.description,'- Trained MLP, CNN, and U-Net models, achieving F1 of 0.90.\n- Generated 5,000 signals.\n- Screened 12,423 signals and added CI/CD.');
});
test('supports present, labels and missing months without guessing', () => {
 const result=pastedExperience('Title: Engineer\nCompany: Lab\nDates: 2025 to Present\nBuilt tools.');
 assert.equal(result.role,'Engineer'); assert.equal(result.employer,'Lab'); assert.equal(result.current,true); assert.equal(result.start,'');
});
test('does not merge two jobs into one', () => {
 assert.equal(pastedExperience('Engineer\nLab\nMay 2025 – Present\nWork\nAnalyst\nOther\nMay 2020 – May 2024\nOther work'),null);
 assert.equal(pastedExperience('I worked on models.'),null);
});

test('formats dates and removes copied city metadata without losing achievements', () => {
 assert.equal(formatExperienceMonth('2026-05'), 'May 2026');
 assert.equal(formatExperienceMonth('2026-08'), 'August 2026');
 assert.equal(cleanExperienceDescription('Columbus, Ohio Metropolitan Area · On-site - Trained models with F1 of 0.90.\nBuilt 5,000 signals.'), 'Trained models with F1 of 0.90.\nBuilt 5,000 signals.');
 assert.equal(cleanExperienceDescription('Improved on-site testing in Columbus.'), 'Improved on-site testing in Columbus.');
});

 test('removes standalone LinkedIn work mode and repeated Present metadata', () => {
  const result = pastedExperience('AI Engineer\nMatching Donors · Part-time\nSep 2026 - Present · 2 mos\nRemote\nPresent\nBuild AI-integrated software for MatchingDonors.com outreach and marketing programs daily.');
  assert.equal(result.description, 'Build AI-integrated software for MatchingDonors.com outreach and marketing programs daily.');
 });

 test('accepts compact role and employer headings with numeric dates', () => {
  const result=pastedExperience('Lab Assistant at Hospital | 5/2024 - 5/2025\nSupported laboratory work by transporting samples and managing inventory for the team.');
  assert.equal(result.role,'Lab Assistant'); assert.equal(result.employer,'Hospital');
  assert.equal(result.start,'2024-05'); assert.equal(result.end,'2025-05');
 });
