import React from 'react';
import { pastedExperience, cleanExperienceDescription } from './pasted-experience.js';
import MonthYearSelect from './MonthYearSelect.jsx';

function JobBox({ value, onChange, number, optional, hasExperience }) {
  const prefix = number === 1 ? 'new' : 'second';
  const update = (key, next) => onChange({ ...value, [key]: next });
  return <section className="card shortlist-card experience-entry">
    <h3>Job {number}{optional ? ' · Optional' : ''}</h3>
    {optional && <label className="experience-current"><input type="checkbox" checked={Boolean(value.enabled)} onChange={e => update('enabled', e.target.checked)} /> Add this second job to my resume</label>}
    <fieldset disabled={optional && !value.enabled} className="experience-job-fields">
      <div className="field">
        <label htmlFor={`${prefix}-description`}>Paste your LinkedIn experience or describe this job</label>
        <p>Use this box for one job. Paste its full LinkedIn Experience entry, or describe your responsibilities, tools, and results. Include the job title and employer. Check the fields below, choose your start and end dates, or select “I still work here.”</p>
        <textarea id={`${prefix}-description`} required rows={4} maxLength={10000} value={value.description || ''} onChange={e => update('description', e.target.value)} onBlur={() => {
          const parsed = pastedExperience(value.description || '');
          if (parsed) onChange({ ...value, ...parsed });
          else update('description', cleanExperienceDescription(value.description || ''));
        }} placeholder={'Machine Learning Engineer\nCompany or lab name\nMay 2026 – Present\nDescribe what you built, who you helped, and any results.'} />
        <small>Copied job titles and dates fill the fields below when recognized. No minimum word count. Part-time labels are left out.</small>
      </div>
      <div className="experience-entry-grid">
        {['role', 'employer'].map(key => <div className="field" key={key}>
          <label htmlFor={`${prefix}-${key}`}>{key === 'role' ? 'Role or job title' : 'Employer or organization'}</label>
          <input id={`${prefix}-${key}`} required maxLength={160} value={value[key] || ''} onChange={e => update(key, e.target.value)} />
        </div>)}
      </div>
      <label className="experience-current"><input type="checkbox" checked={Boolean(value.current)} onChange={e => update('current', e.target.checked)} /> I still work here</label>
      <div className="experience-entry-grid">
        <div className="field"><label htmlFor={`${prefix}-start`}>Start date</label><MonthYearSelect id={`${prefix}-start`} label="Start date" required value={value.start || ''} onChange={next => update('start', next)} /></div>
        {!value.current && <div className="field"><label htmlFor={`${prefix}-end`}>End date</label><MonthYearSelect id={`${prefix}-end`} label="End date" required value={value.end || ''} onChange={next => update('end', next)} /></div>}
      </div>
      {hasExperience && <label className="experience-current"><input type="checkbox" checked={value.replaceOldest !== false} onChange={e => update('replaceOldest', e.target.checked)} /> Remove my oldest completed experience to make room.</label>}
    </fieldset>
  </section>;
}

export default function NewExperienceFields({ value, onChange, hasExperience }) {
  return <section className="experience-jobs">
    <div className="field"><label htmlFor="add-experience">Do you have any new experience to add?</label>
      <select id="add-experience" value={value.enabled ? 'yes' : 'no'} onChange={e => onChange({ ...value, enabled: e.target.value === 'yes' })}><option value="no">No</option><option value="yes">Yes</option></select>
    </div>
    {value.enabled && <>
      <JobBox number={1} value={value} onChange={onChange} hasExperience={hasExperience} />
      <JobBox number={2} optional value={value.secondRole || { enabled: false }} onChange={secondRole => onChange({ ...value, secondRole })} hasExperience={hasExperience} />
    </>}
  </section>;
}
