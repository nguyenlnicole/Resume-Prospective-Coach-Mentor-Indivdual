import test from 'node:test';
import assert from 'node:assert/strict';
import {parseLinkedinExperience,linkedinProfileUrl} from './linkedin-experience.js';
test('keeps separate roles, responsibilities and dates without inventing missing months',()=>{
 const roles=parseLinkedinExperience('Engineer II\nExample Co · Full-time\nAugust 2020 – Present\nBuilt scheduling tools.\nEngineer\nExample Co\n2018 - 2020\nTested applications.');
 assert.equal(roles.length,2);assert.equal(roles[0].start,'2020-08');assert.equal(roles[0].current,true);
 assert.equal(roles[0].description,'Built scheduling tools.');assert.equal(roles[1].description,'Tested applications.');assert.equal(roles[1].start,'');
});
test('does not interpret login pages as employment or accept unrelated URLs',()=>{
 assert.deepEqual(parseLinkedinExperience('Sign in to LinkedIn'),[]);
 assert.equal(linkedinProfileUrl('https://linkedin.com.evil.test/in/person'),'');
 assert.equal(linkedinProfileUrl('https://www.linkedin.com/in/person'),'https://www.linkedin.com/in/person');
});

import { linkedinExperienceSource } from './linkedin-experience.js';
test('reads a pasted profile URL instead of parsing it as experience', async () => {
 const calls=[];
 const result=await linkedinExperienceSource('  https://www.linkedin.com/in/qifan-wen/  ', 'https://www.linkedin.com/in/other/', async url=>{calls.push(url);return {text:'Profile experience'};});
 assert.equal(result,'Profile experience');
 assert.deepEqual(calls,['https://www.linkedin.com/in/qifan-wen/']);
});
test('pasted experience avoids a profile request and blank input uses the saved URL', async () => {
 assert.equal(await linkedinExperienceSource('Engineer\nExample Co', '', ()=>{throw Error('Unexpected request');}), 'Engineer\nExample Co');
 assert.equal(await linkedinExperienceSource('', 'https://www.linkedin.com/in/person/', async()=>({text:'Saved profile'})), 'Saved profile');
 await assert.rejects(linkedinExperienceSource('https://example.com/in/person', '', ()=>{throw Error('Unexpected request');}), /LinkedIn profile URL/);
});
