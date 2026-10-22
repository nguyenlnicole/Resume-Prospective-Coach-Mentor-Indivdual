import { pastedExperience, cleanExperienceDescription } from './pasted-experience.js';
const completeMonth = value => /^\d{4}-(0[1-9]|1[0-2])$/.test(value || '');
import { experienceList } from './rules.js';

export function experienceWordCount(text = '') {
  const description = pastedExperience(text)?.description ?? cleanExperienceDescription(text);
  return description.trim().split(/\s+/).filter(word => /[\p{L}\p{N}]/u.test(word)).length;
}

export function newExperienceError(value) {
  if (!value.enabled) return '';
  if (!value.role?.trim() || !value.employer?.trim()) return 'Include your role and company so we can identify this experience.';
  if (/^https?:\/\/\S+$/i.test(value.description?.trim() || '') || experienceWordCount(value.description) < 10) return 'Describe what you did in at least 10 words. Paste the experience text, not a profile link.';
  if (!completeMonth(value.start) || (!value.current && !completeMonth(value.end))) return 'Include start and end months and years in the text, such as May 2024 - August 2025, or May 2024 - Present.';
  const today = new Date();
  const currentMonth = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}`;
  if (value.start > currentMonth || (!value.current && value.end > currentMonth)) return 'Experience dates cannot be in the future.';
  if (!value.current && value.end < value.start) return 'The end date must be on or after the start date.';
  return value.secondRole?.enabled ? newExperienceError(value.secondRole) : '';
}

export function removeOldestExperience(text) {
  const entries = experienceList(text);
  const months = ['jan','feb','mar','apr','may','jun','jul','aug','sep','oct','nov','dec'];
  const dated = entries.map((entry, index) => {
    const heading = entry.split('\n').filter(line => !/^\s*[•*\-]/.test(line)).join(' ');
    const dates = [...heading.matchAll(/(?:(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\.?\s+)?((?:19|20)\d{2})/gi)];
    const date = dates.at(-1);
    return { index, date: /\b(present|current)\b/i.test(heading) ? Infinity : date ? Number(date[2]) * 12 + (date[1] ? months.indexOf(date[1].slice(0,3).toLowerCase()) : 11) : Infinity };
  }).filter(item => Number.isFinite(item.date)).sort((a,b) => a.date - b.date);
  return dated.length ? entries.filter((_,index) => index !== dated[0].index).join('\n\n') : text;
}

export const shouldReplaceOldestExperience = role => role?.enabled === true && role.replaceOldest === true;
