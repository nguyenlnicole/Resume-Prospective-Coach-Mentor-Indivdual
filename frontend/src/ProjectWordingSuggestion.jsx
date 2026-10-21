import { useState } from 'react';
import { post } from './api';
import { projectList, projectDescriptionWords } from './rules';

export default function ProjectWordingSuggestion({ value, onChange }) {
  const [suggestion, setSuggestion] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const projects = projectList(value);
  const longProjects = projects.filter(project => projectDescriptionWords(project) > 45);
  const current = suggestion?.source === value ? suggestion : null;
  if (!longProjects.length) return null;
  async function suggest() {
    setBusy(true); setError(''); setSuggestion(null);
    try {
      const updates = await Promise.all(longProjects.map(async project => {
        const lines = project.split(/\r?\n/);
        const indexes = lines.map((line, index) => index > 0 && line.trim() && !/^\s*(?:tech(?:nologies)?|stack|languages?|organization)\s*:/i.test(line) && !/https?:\/\//i.test(line) && !/\s\|\s[^|]*\b(?:19|20)\d{2}[^|]*$/.test(line) ? index : -1).filter(index => index >= 0);
        if (!indexes.length) throw new Error('Open Edit to review this project’s description. Your wording is unchanged.');
        const result = await post('/projects/shorten', { description: indexes.map(index => lines[index]).join(' ') });
        if (!result.text?.trim() || result.text.trim().split(/\s+/).length > 45) throw new Error('A shorter suggestion was not available. Your wording is unchanged.');
        const next = lines.flatMap((line, index) => index === indexes[0] ? [result.text.trim()] : indexes.includes(index) ? [] : [line]).join('\n');
        return { original: project, next, title: lines[0], text: result.text.trim() };
      }));
      setSuggestion({ source: value, updates });
    } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }
  return <div className="project-wording-suggestion">
    <button type="button" className="secondary button" onClick={suggest} disabled={busy}>{busy ? 'Writing suggestions…' : 'Suggest shorter wording'}</button>
    {error && <p role="alert">{error}</p>}
    {current && <div className="notice">
      {current.updates.map(item => <div key={item.original}><strong>{item.title}</strong><p>{item.text}</p></div>)}
      <button type="button" className="primary button" onClick={() => { onChange(projects.map(project => current.updates.find(item => item.original === project)?.next || project).join('\n\n')); setSuggestion(null); }}>Use suggested wording</button>
      <button type="button" className="secondary button" onClick={() => setSuggestion(null)}>Keep my wording</button>
    </div>}
  </div>;
}
