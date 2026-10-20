import React, { useRef, useState } from 'react';
import { fetchProjectDetails } from './new-projects.js';

function ProjectFields({ value, onChange, onRemove, index }) {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const request = useRef(0);
  const update = (key, next) => { request.current++; setLoading(false); onChange({ ...value, [key]: next, confirmed: false }); };
  async function lookup() {
    const token = ++request.current;
    setLoading(true); setMessage('');
    onChange({ ...value, confirmed: false });
    try {
      const details = await fetchProjectDetails(value.repo);
      if (request.current !== token) return;
      onChange({ ...value, ...details });
      setMessage('Available details filled in. Check them, add your own contribution, and confirm. GitHub does not prove which work you did.');
    } catch (error) { if (request.current === token) setMessage(error.message); }
    finally { if (request.current === token) setLoading(false); }
  }
  const prefix = `new-project-${index}`;
  return <section className="card shortlist-card">
    <h3>New project {index + 1}</h3>
    <div className="field"><label htmlFor={`${prefix}-github`}>Is this project on your GitHub?</label>
      <select id={`${prefix}-github`} value={value.onGithub || 'no'} onChange={e => update('onGithub',e.target.value)}><option value="no">No</option><option value="yes">Yes</option></select>
    </div>
    {value.onGithub === 'yes' ? <div className="field">
      <label htmlFor={`${prefix}-repo`}>Public repository URL</label>
      <input id={`${prefix}-repo`} type="url" value={value.repo || ''} onChange={e => update('repo',e.target.value)} onBlur={() => { if (value.repo?.trim()) lookup(); }} placeholder="https://github.com/yourname/project" />
      <button type="button" className="text-button" disabled={loading} onClick={lookup}>{loading ? 'Reading GitHub…' : 'Fill details from GitHub'}</button>
    </div> : <p>Please add this project to your GitHub when you can, then include its repository link. You can still add it to your resume now.</p>}
    {message && <p role="status">{message}</p>}
    {[
      ['name','Project name',false], ['description','What does the project do?',true],
      ['contribution','What did you personally build or contribute? Include real results if known.',true],
      ['stack','Technologies you actually used',false], ['demo','Demo URL · Optional',false],
    ].map(([key,label,multiline]) => <div className="field" key={key}>
      <label htmlFor={`${prefix}-${key}`}>{label}</label>
      {multiline ? <textarea id={`${prefix}-${key}`} rows={3} maxLength={4000} value={value[key] || ''} onChange={e => update(key,e.target.value)} />
        : <input id={`${prefix}-${key}`} maxLength={key === 'name' ? 160 : 1000} value={value[key] || ''} onChange={e => update(key,e.target.value)} />}
    </div>)}
    <label className="consent"><input type="checkbox" disabled={loading} checked={Boolean(value.confirmed)} onChange={e => onChange({...value, confirmed:e.target.checked})} /> I checked these details and confirm they describe my project and my own work.</label>
    <button type="button" className="text-button" onClick={onRemove}>Remove this new project</button>
  </section>;
}

export default function NewProjectFields({ value, onChange }) {
  return <section>
    <div className="field"><label htmlFor="has-new-projects">Do you have any new projects to add?</label>
      <select id="has-new-projects" value={value.length ? 'yes' : 'no'} onChange={e => onChange(e.target.value === 'yes' ? [{id: crypto.randomUUID(), onGithub:'no'}] : [])}><option value="no">No</option><option value="yes">Yes</option></select>
    </div>
    {value.map((project,index) => <ProjectFields key={project.id} value={project} index={index} onChange={next => onChange(current => current.map(item => item.id === project.id ? next : item))} onRemove={() => onChange(current => current.filter(item => item.id !== project.id))} />)}
    {value.length > 0 && value.length < 5 && <button type="button" className="text-button" onClick={() => onChange([...value,{id:crypto.randomUUID(),onGithub:'no'}])}>Add another new project</button>}
  </section>;
}
