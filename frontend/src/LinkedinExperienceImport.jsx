import React, { useState } from 'react';
import { post } from './api';
import { linkedinExperienceSource, parseLinkedinExperience } from './linkedin-experience';
export default function LinkedinExperienceImport({ url, onApply, disabled }) {
  const [text,setText]=useState(''),[roles,setRoles]=useState([]),[busy,setBusy]=useState(false),[message,setMessage]=useState('');
  async function load() {
    setBusy(true);setMessage('');setRoles([]);
    try {
      const source=await linkedinExperienceSource(text, url, link => post('/linkedin/experience-text',{url:link}));
      const found=parseLinkedinExperience(source);setRoles(found);
      setMessage(found.length?'Check the role, employer, dates, and description before using these details.':'No clear roles found. Paste each job title, employer, date range, and description on separate lines.');
    } catch(error){setMessage(error.message);}finally{setBusy(false);}
  }
  return <section className="card shortlist-card"><h3>Fill experience from LinkedIn</h3>
    <p>Read your public profile or paste its Experience section. Review up to two roles before filling the form.</p>
    <label htmlFor="linkedin-experience-text">LinkedIn profile link or Experience text</label>
    <textarea id="linkedin-experience-text" rows={4} maxLength={12000} value={text} onChange={e=>{setText(e.target.value);setRoles([]);}} placeholder="https://www.linkedin.com/in/your-name/ or paste your Experience section" />
    <button type="button" className="button secondary" disabled={disabled || busy} onClick={load}>{busy?'Reading experience…':'Find my roles'}</button>
    {message && <p role="status">{message}</p>}
    {roles.map((role,index)=><article key={index}><h4>{role.role} · {role.employer}</h4><p>{role.start || 'Start date needed'} – {role.current?'Present':role.end || 'End date needed'}</p><p>{role.description || 'Add your responsibilities and results.'}</p></article>)}
    {roles.length>0 && <button type="button" className="button secondary" onClick={()=>{onApply({...roles[0],secondRole:roles[1] || null});setRoles([]);setMessage('Filled the new-role fields below. Complete missing information and check for roles already on your resume.');}}>Use these roles in the new-role fields</button>}
  </section>;
}
