import test from 'node:test';
import assert from 'node:assert/strict';
import {newestExperienceFirst} from './rules.js';
test('both current roles precede completed roles and keep their bullets',()=>{
 const text='Frontend - Internship | February 2026 - Present\n• Built pages.\n\nDelivery - Driver | September 2013 - December 2015\n• Delivered freight.\n\nPostal - Compliance | December 2015 - Present\n• Reviewed procedures.';
 const sorted=newestExperienceFirst(text);
 assert.ok(sorted.indexOf('Frontend')<sorted.indexOf('Postal'));assert.ok(sorted.indexOf('Postal')<sorted.indexOf('Delivery'));
 assert.match(sorted,/Postal[^]*Reviewed procedures\.\n\nDelivery/);
});
test('completed roles sort by end date and grouped role bullets stay attached',()=>{
 const sorted=newestExperienceFirst('Company\nJunior | Jan 2018 - Jan 2020\n• Junior duties.\nSenior | Jan 2020 - Present\n• Senior duties.\n\nOther - Role | 2021 - 2023\n• Other duties.');
 assert.ok(sorted.indexOf('Senior')<sorted.indexOf('Other'));assert.ok(sorted.indexOf('Other')<sorted.indexOf('Junior'));
 assert.match(sorted,/Company\nJunior/);assert.match(sorted,/Senior[^]*Senior duties/);
});
