import React from 'react';
import MonthYearSelect from './MonthYearSelect.jsx';
export default function NewExperienceFields({ value, onChange, hasExperience }) {
  const update = (key, next) => onChange({ ...value, [key]: next });
  const words = value.description?.trim().split(/\s+/).filter(Boolean).length || 0;
  return <section className="card shortlist-card">
    <div className="field">
      <label htmlFor="add-experience">Do you have any new experience to add?</label>
      <select id="add-experience" value={value.enabled ? 'yes' : 'no'} onChange={e => update('enabled', e.target.value === 'yes')}>
        <option value="no">No</option><option value="yes">Yes</option>
      </select>
    </div>
    {value.enabled && <>
      {['role', 'employer'].map(key => <div className="field" key={key}>
        <label htmlFor={`new-${key}`}>{key === 'role' ? 'Role or job title' : 'Employer or organization'}</label>
        <input id={`new-${key}`} required maxLength={160} value={value[key] || ''} onChange={e => update(key,e.target.value)} />
      </div>)}
      <div className="field"><label htmlFor="new-description">Describe your work in at least 50 words</label>
        <p>Tell us what you did, who you helped, and any results. We’ll turn your real details into bullet points.</p>
        <textarea id="new-description" required rows={6} maxLength={10000} value={value.description || ''} onChange={e => update('description',e.target.value)} />
        <p aria-live="polite">{words} / 50 words minimum</p>
      </div>
      <div className="field"><label htmlFor="new-start">Start date</label><MonthYearSelect id="new-start" label="Start date" required value={value.start || ''} onChange={next => update('start',next)} /></div>
      <label><input type="checkbox" checked={Boolean(value.current)} onChange={e => update('current',e.target.checked)} /> I still work here</label>
      {!value.current && <div className="field"><label htmlFor="new-end">End date</label><MonthYearSelect id="new-end" label="End date" required value={value.end || ''} onChange={next => update('end',next)} /></div>}
      {hasExperience && <p><label><input type="checkbox" checked={value.replaceOldest !== false} onChange={e => update('replaceOldest',e.target.checked)} /> Remove my oldest completed experience to make room.</label> Current roles and entries with unknown dates stay.</p>}
    </>}
  </section>;
}
