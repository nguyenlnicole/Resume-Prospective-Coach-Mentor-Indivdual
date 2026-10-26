import React, { useState, useRef, useEffect } from 'react';
import { post } from './api.js';
import { pastedExperience, formatExperienceMonth } from './pasted-experience.js';
import { experienceWordCount, newExperienceError } from './new-experience.js';

function JobBox({ value, onChange, number, optional, hasExperience, demo, level }) {
  const prefix = number === 1 ? 'new' : 'second';
  const [showPreview, setShowPreview] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [generationError, setGenerationError] = useState('');
  const generation = useRef(0);
  useEffect(() => () => { generation.current++; }, []);
  async function previewRole() {
    setShowPreview(true);
    if (error || generating || value.previewBullets?.length === (["Senior", "Management"].includes(level) ? 4 : 3)) return;
    const requestId = ++generation.current;
    setGenerating(true); setGenerationError('');
    try {
      const result = await post('/resume/experience-bullets', { ...value, level, enabled: true, secondRole: undefined });
      if (requestId === generation.current) onChange({ ...value, previewBullets: result.bullets });
    } catch (failure) { if (requestId === generation.current) setGenerationError(failure.message); }
    finally { if (requestId === generation.current) setGenerating(false); }
  }
  const update = (key, next) => { generation.current++; setGenerating(false); onChange({ ...value, [key]: next }); };
  const restoredText = value.role && value.employer
    ? [value.role, value.employer, `${formatExperienceMonth(value.start)} - ${value.current ? 'Present' : formatExperienceMonth(value.end)}`, value.description].filter(Boolean).join('\n')
    : value.description || '';
  const text = value.pastedText ?? restoredText;
  const error = newExperienceError({ ...value, level, enabled: true, secondRole: undefined });
  return <section className="card shortlist-card experience-entry">
    <h3>Fill experience from LinkedIn</h3>
    <p><strong>Experience {number}{optional ? ' · Optional' : ''}</strong></p>
    {optional && <div className="field"><label htmlFor="second-add-experience">Add a second experience?</label><select id="second-add-experience" value={value.enabled ? 'yes' : 'no'} onChange={e => update('enabled', e.target.value === 'yes')}><option value="no">No</option><option value="yes">Yes</option></select></div>}
    <fieldset disabled={optional && !value.enabled} className="experience-job-fields">
      <div className="field">
        <label htmlFor={`${prefix}-description`}>LinkedIn Experience text or your job description</label>
        <p>Paste one LinkedIn job, or enter your role, company, dates (or Present), and two sentences about your work—at least 10 words. We’ll turn your facts into bullet points.</p>
        <textarea id={`${prefix}-description`} required rows={7} maxLength={10000} value={text} onChange={e => {
          const pastedText = e.target.value;
          const parsed = pastedExperience(pastedText);
          generation.current++; setGenerating(false); setGenerationError('');
          onChange({ ...value, previewBullets: undefined, pastedText, ...(parsed || {role:'', employer:'', start:'', end:'', current:false, description:''}) });
          setShowPreview(false);
        }} placeholder={'Copy and paste the entire LinkedIn Experience entry for one job, or provide your own details: role, company, dates (or Present), and at least two sentences about your work.'} />
        <small>{experienceWordCount(value.description)} / 10 words minimum. Paste text, not a profile link.</small>
      </div>
      <button type="button" className="button secondary" style={{alignSelf:'flex-start'}} disabled={generating || demo} onClick={previewRole}>{generating ? 'Writing resume bullets…' : 'Find this role'}</button>
      {showPreview && (error ? <p role="alert">{error}</p> : <div className="experience-preview">
        <p role="status">Check these details. Edit the text above to make changes.</p>
        <h4>{value.role} · {value.employer}</h4>
        <p>{formatExperienceMonth(value.start)} – {value.current ? 'Present' : formatExperienceMonth(value.end)}</p>
        {generating ? <p role="status">Turning your details into resume bullets…</p> : value.previewBullets?.length ? <ul>{value.previewBullets.map((bullet, index) => <li key={index}>{bullet}</li>)}</ul> : null}
        {generationError && <p role="alert">{generationError} Your original details are kept. Select “Find this role” to retry.</p>}
      </div>)}
      {hasExperience && <label className="experience-current"><input type="checkbox" checked={value.replaceOldest === true} onChange={e => update('replaceOldest', e.target.checked)} /> Replace my oldest completed experience with this role (optional).</label>}
    </fieldset>
  </section>;
}

export default function NewExperienceFields({ value, onChange, hasExperience, demo = false, level = "Junior" }) {
  return <section className="experience-jobs">
    <div className="field"><label htmlFor="add-experience">Add additional experience?</label>
      <select id="add-experience" value={value.enabled ? 'yes' : 'no'} onChange={e => onChange({ ...value, enabled: e.target.value === 'yes' })}><option value="no">No</option><option value="yes">Yes</option></select>
    </div>
    {value.enabled && <>
      <JobBox number={1} value={value} onChange={onChange} hasExperience={hasExperience} demo={demo} level={level} />
      <JobBox number={2} optional value={value.secondRole || { enabled: false }} onChange={secondRole => onChange({ ...value, secondRole })} hasExperience={hasExperience} demo={demo} level={level} />
    </>}
  </section>;
}
