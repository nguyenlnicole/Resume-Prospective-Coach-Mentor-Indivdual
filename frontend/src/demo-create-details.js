import { blankResumeDetails } from './create-resume.js';
import { demoResume } from './demo-sample-candidate.js';
export function demoCreateDetails() {
  return {
    ...blankResumeDetails(), first:'Alex', last:'Rivera', email:demoResume.contact.email,
    phone:demoResume.contact.phone, location:demoResume.contact.location,
    profession:'Technical', level:'Senior', skills:demoResume.skills, credentials:demoResume.certifications,
    experience:[{id:'sample-lead', employer:'Cedar Example Labs', role:'Lead Engineer', start:'2024-01', current:true,
      description:demoResume.experience.split('\n\n')[0].split('\n').slice(1).map(line=>line.replace(/^•\s*/, '')).join(' ')}],
    education:[{id:'sample-degree',school:'Example Institute',qualification:'B.S. in Computer Science',date:'2017-05'}]
  };
}