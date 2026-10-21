import { formatExperienceMonth } from './pasted-experience.js';
import React, { useState } from 'react';
import { post } from './api';
import { linkedinProfileUrl, linkedinExperienceSource, parseLinkedinExperience } from './linkedin-experience';
export default function LinkedinExperienceImport({ url, onApply, disabled }) {
  const [text,setText]=useState(''),[roles,setRoles]=useState([]),[busy,setBusy]=useState(false),[message,setMessage]=useState('');
  const [manual, setManual] = useState(false);
  const savedLink = linkedinProfileUrl(url);
  async function load(input = '') {
    setBusy(true);setMessage('');setRoles([]);
    try {
      const source=await linkedinExperienceSource(input, savedLink, link => post('/linkedin/experience-text',{url:link}));
      const found=parseLinkedinExperience(source);setRoles(found);
      if (!found.length) setManual(true);
      setMessage(found.length?'Check the role, employer, dates, and description before using these details.':'No clear roles found. Paste each job title, employer, date range, and description on separate lines.');
    } catch(error){setMessage(error.message);setManual(true);}finally{setBusy(false);}
  }
  return <section className="card shortlist-card"><h3>Fill experience from LinkedIn</h3>
    <p>Use your LinkedIn link from the previous page to find up to two roles.</p>
    {!savedLink && <p className="muted">Add your LinkedIn profile link on the previous page, or paste your experience below.</p>}
    <button type="button" className="button secondary" disabled={disabled || busy || !savedLink} onClick={() => load('')}>{busy ? 'Reading experience…' : 'Pull from LinkedIn'}</button>
    <details open={manual} onToggle={event => setManual(event.currentTarget.open)}>
      <summary>Paste experience instead</summary>
      <label htmlFor="linkedin-experience-text">LinkedIn Experience text</label>
      <textarea id="linkedin-experience-text" rows={4} maxLength={12000} value={text} onChange={e=>{setText(e.target.value);setRoles([]);}} placeholder="Paste job titles, employers, dates, and descriptions" />
      <button type="button" className="button secondary" disabled={disabled || busy || !text.trim()} onClick={() => load(text)}>Find roles in pasted text</button>
    </details>
    {message && <p role="status">{message}</p>}
    {roles.map((role,index)=><article key={index}><h4>{role.role} · {role.employer}</h4><p>{formatExperienceMonth(role.start) || 'Start date needed'} – {role.current?'Present':formatExperienceMonth(role.end) || 'End date needed'}</p><p>{role.description || 'Add your responsibilities and results.'}</p></article>)}
    {roles.length>0 && <button type="button" className="button secondary" onClick={()=>{onApply({...roles[0],secondRole:roles[1] || null});setRoles([]);setMessage('Filled the new-role fields below. Complete missing information and check for roles already on your resume.');}}>Use these roles in the new-role fields</button>}
  </section>;
}
