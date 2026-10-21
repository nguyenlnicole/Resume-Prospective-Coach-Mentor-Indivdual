import React from 'react';
import MonthYearSelect from './MonthYearSelect.jsx';
export default function NewExperienceFields({ value, onChange, hasExperience, second = false }) {
  const update = (key, next) => onChange({ ...value, [key]: next });
  const prefix = second ? "second" : "new";
  const words = value.description?.trim().split(/\s+/).filter(Boolean).length || 0;
  return <section className="card shortlist-card">
    <div className="field">
      <label htmlFor={second ? "add-second-experience" : "add-experience"}>{second ? "Add a second role?" : "Do you have any new experience to add?"}</label>
      <select id={second ? "add-second-experience" : "add-experience"} value={value.enabled ? 'yes' : 'no'} onChange={e => update('enabled', e.target.value === 'yes')}>
        <option value="no">No</option><option value="yes">Yes</option>
      </select>
    </div>
    {value.enabled && <>
      {['role', 'employer'].map(key => <div className="field" key={key}>
        <label htmlFor={`${prefix}-${key}`}>{key === 'role' ? 'Role or job title' : 'Employer or organization'}</label>
        <input id={`${prefix}-${key}`} required maxLength={160} value={value[key] || ''} onChange={e => update(key,e.target.value)} />
      </div>)}
      <div className="field"><label htmlFor={`${prefix}-description`}>Tell us briefly what you did</label>
        <p>Tell us what you did, who you helped, and any results. We’ll turn your real details into bullet points.</p>
        <textarea id={`${prefix}-description`} required rows={6} maxLength={10000} value={value.description || ''} onChange={e => update('description',e.target.value)} />
        <p aria-live="polite">{words} words · No minimum</p>
      </div>
      <div className="field"><label htmlFor={`${prefix}-start`}>Start date</label><MonthYearSelect id={`${prefix}-start`} label="Start date" required value={value.start || ''} onChange={next => update('start',next)} /></div>
      <label><input type="checkbox" checked={Boolean(value.current)} onChange={e => update('current',e.target.checked)} /> I still work here</label>
      {!value.current && <div className="field"><label htmlFor={`${prefix}-end`}>End date</label><MonthYearSelect id={`${prefix}-end`} label="End date" required value={value.end || ''} onChange={next => update('end',next)} /></div>}
      {hasExperience && <p><label><input type="checkbox" checked={value.replaceOldest !== false} onChange={e => update('replaceOldest',e.target.checked)} /> Remove my oldest completed experience to make room.</label> Current roles and entries with unknown dates stay.</p>}
      {!second && !value.secondRole?.enabled && <button type="button" className="button secondary" onClick={() => update("secondRole", {enabled:true})}>Add a second role</button>}
      {!second && value.secondRole?.enabled && <><NewExperienceFields second value={value.secondRole} onChange={next => update("secondRole", next)} hasExperience={hasExperience} /><button type="button" className="text-button" onClick={() => update("secondRole", null)}>Remove second role</button></>}
    </>}
  </section>;
}
