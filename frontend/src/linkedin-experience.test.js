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
