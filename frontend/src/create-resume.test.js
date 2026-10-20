import test from 'node:test';
import assert from 'node:assert/strict';
import { blankResumeDetails, createResumeError, resumeFromDetails } from './create-resume.js';
const details = () => ({...blankResumeDetails(),first:'Alex',last:'Rivera',email:'alex@example.com',profession:'Educator'});
test('people without employment or education entries can start a resume',()=>{
  assert.equal(createResumeError(details(),0),'');
  assert.equal(createResumeError(details(),1),'');
  assert.equal(resumeFromDetails(details()).experience,'');
});
test('requires complete roles, valid date order, profession, and consent',()=>{
  const value=details();
  value.experience=[{role:'Teacher',employer:'School',description:'Helped students',start:'2025-01',end:'2024-01'}];
  assert.match(createResumeError(value,1),/before/);
  value.experience[0].current=true;
  assert.equal(createResumeError(value,1),'');
  value.profession='';
  assert.match(createResumeError(value,0),/profession/);
  value.profession='Technical';value.level='Entry / Junior';
  assert.match(createResumeError(value,2),/processing/);
});
test('retains ongoing employment and expected education dates without inventing experience',()=>{
  const value=details();
  value.experience=[{employer:'School',role:'Volunteer tutor',start:'2024-08',current:true,description:'Helped students practice reading.'}];
  value.education=[{school:'College',qualification:'Education degree',current:true,date:'2027-05'}];
  const resume=resumeFromDetails(value);
  assert.match(resume.experience,/August 2024 – Present/);
  assert.match(resume.education,/Expected May 2027/);
  assert.equal(resume.contact.name,'Alex Rivera');
  assert.equal(resume.summary,'');
});

test('partial dropdown dates must be completed or cleared',()=>{
  const value={...details(),consent:true,experience:[{role:'Teacher',employer:'School',description:'Teaching',start:'2025-',current:true}]};
  assert.match(createResumeError(value,1),/Complete/);
  value.experience[0].start='2025-08';
  assert.equal(createResumeError(value,1),'');
  value.education=[{school:'College',qualification:'Degree',date:'-05'}];
  assert.match(createResumeError(value,2),/month and year/);
  value.education[0].date='';
  assert.equal(createResumeError(value,2),'');
});

test('additional languages apply to every profession without assuming fluency',()=>{
  for (const profession of ['Technical','General','Medical Field','Educator']) {
    const value={...details(),profession,consent:true,additionalLanguages:'yes',languages:''};
    assert.match(createResumeError(value,2),/languages/);
    value.languages='Spanish, Vietnamese';
    assert.equal(createResumeError(value,2),'');
    assert.equal(resumeFromDetails(value).skills,'Languages: Spanish, Vietnamese');
    value.additionalLanguages='no';
    assert.equal(resumeFromDetails(value).skills,'');
  }
});
