const completeMonth = value => /^\d{4}-(0[1-9]|1[0-2])$/.test(value || '');
export const blankResumeDetails = () => ({ step: 0, first: '', last: '', email: '', phone: '', location: '', experience: [], education: [], profession: '', level: '', skills: '', credentials: '', additionalLanguages: '', languages: '', consent: false });
export function createResumeError(value, step = value.step) {
  if (step === 0) {
    if (!value.first.trim() || !value.last.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.email)) return 'Enter your first name, last name, and a valid email.';
    if (!value.profession || (value.profession === 'Technical' && !value.level)) return 'Choose your profession and career level.';
  }
  if (step === 1) {
    for (const job of value.experience) {
      if (!job.role?.trim() || !job.employer?.trim() || !job.description?.trim() || !completeMonth(job.start) || (!job.current && !completeMonth(job.end))) return 'Complete each experience, including its dates and what you did, or remove the empty entry.';
      const today = new Date();
      const currentMonth = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}`;
      if (job.start > currentMonth || (!job.current && job.end > currentMonth)) return 'Experience dates cannot be in the future.';
      if (!job.current && job.end < job.start) return 'An experience end date cannot be before its start date.';
    }
  }
  if (step === 2 && value.education.some(item => !item.school?.trim() || !item.qualification?.trim())) return 'Enter the school and qualification or program, or remove the empty entry.';
  if (step === 2 && value.education.some(item => item.date && !completeMonth(item.date))) return 'Choose both a month and year for education, or leave both blank.';
  if (step === 2 && value.additionalLanguages === 'yes' && !value.languages?.trim()) return 'Enter the languages you would like to include.';
  if (step === 2 && !value.consent) return 'Confirm the processing notice to continue.';
  return '';
}
export function resumeFromDetails(value) {
  const month = date => date ? new Date(`${date}-01T12:00:00`).toLocaleDateString('en-US',{month:'long',year:'numeric'}) : '';
  const clean = text => (text || '').trim().replace(/\s+/g,' ');
  const experience = [...value.experience].sort((a,b) => (b.current ? '9999' : b.end).localeCompare(a.current ? '9999' : a.end));
  return {
    contact: { name: `${clean(value.first)} ${clean(value.last)}`, email: value.email.trim(), phone: clean(value.phone), location: clean(value.location), linkedin:'',github:'',portfolio:'' },
    summary:'', experience: experience.map(job => `${clean(job.employer)} — ${clean(job.role)} | ${month(job.start)} – ${job.current ? 'Present' : month(job.end)}\n• ${clean(job.description)}`).join('\n\n'),
    education: value.education.map(item => `${clean(item.school)} — ${clean(item.qualification)}${item.date ? ' | ' + (item.current ? 'Expected ' : '') + month(item.date) : item.current ? ' | In progress' : ''}`).join('\n'),
    skills:[value.skills.trim(), value.additionalLanguages === 'yes' && clean(value.languages) ? `Languages: ${clean(value.languages)}` : ''].filter(Boolean).join('\n'), certifications:value.credentials.trim(),projects:'',softwareEngineering:false,
  };
}
