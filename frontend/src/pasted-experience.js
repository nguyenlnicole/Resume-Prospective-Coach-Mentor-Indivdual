export function formatExperienceMonth(value = '') {
  const match = /^(\d{4})-(0[1-9]|1[0-2])$/.exec(value);
  return match ? new Date(Number(match[1]), Number(match[2])-1, 1).toLocaleDateString('en-US', {month:'long', year:'numeric'}) : value;
}
export function cleanExperienceDescription(text = '') {
  return text.split(/\r?\n/).map(line => line
    .replace(/^\s*[^\n]*?\s+[·•]\s*(?:on-site|hybrid|remote)\b(?:\s*[-–—•]\s*)?/i, '')
    .replace(/^\s*Location\s*:[^\n]*$/i, '')
  ).filter(line => line.trim() && !/^\s*(?:remote|on-site|hybrid|present|current)\s*$/i.test(line) && !/^\s*(?:Thumbnail for |.* logo\s*$)/i.test(line)).join('\n').trim();
}
export function pastedExperience(text = '') {
  const lines = text.slice(0, 12000).replace(/\s*\|\s*/g, '\n').replace(/\s*;\s*(?=(?:role|title|job title|company|employer|organization|dates?|description)\s*:)/gi, '\n').split(/\r?\n/).map(line => line.trim().replace(/\\$/g, '').replace(/^\\(?=[-•])/, '').replace(/\*\*/g, '').trim()).filter(Boolean);
  if (lines.length > 1 && /\s+at\s+/i.test(lines[0])) {
    const heading = lines[0].split(/\s+at\s+/i);
    if (heading.length === 2) lines.splice(0, 1, ...heading);
  }
  const monthNames = ['jan','feb','mar','apr','may','jun','jul','aug','sep','oct','nov','dec'];
  const datePattern = /^(?:dates?\s*:\s*)?((?:[A-Za-z]+\.?\s+)?(?:19|20)\d{2}|\d{1,2}\/(?:19|20)\d{2})\s*(?:[-–—]|to)\s*(Present|Current|(?:[A-Za-z]+\.?\s+)?(?:19|20)\d{2}|\d{1,2}\/(?:19|20)\d{2})(?:\s*[·•|].*)?$/i;
  const ranges = lines.map((line,index) => ({index,match:line.match(datePattern)})).filter(item => item.match);
  if (ranges.length !== 1 || ranges[0].index < 2) return null;
  const {index,match} = ranges[0];
  const month = value => {
    const numeric = value.match(/^(0?[1-9]|1[0-2])\/(\d{4})$/);
    if (numeric) return `${numeric[2]}-${numeric[1].padStart(2, '0')}`;
    const parts = value.match(/^([A-Za-z]+)\.?\s+(\d{4})$/);
    const number = parts ? monthNames.indexOf(parts[1].slice(0,3).toLowerCase()) : -1;
    return number < 0 ? '' : `${parts[2]}-${String(number+1).padStart(2,'0')}`;
  };
  const role = lines[index-2].replace(/^(?:role|job title|title)\s*:\s*/i,'');
  const employer = lines[index-1].replace(/^(?:employer|company|organization)\s*:\s*/i,'').replace(/\s+[·•]\s+(?:full-time|part-time|contract|internship|self-employed|freelance|seasonal|temporary).*$/i,'');
  let descriptionLines = lines.slice(index+1);
  if (/^(?:location\s*:|.*\s[·•]\s(?:on-site|hybrid|remote)\s*$)/i.test(descriptionLines[0] || '')) descriptionLines = descriptionLines.slice(1);
  return {role, employer, start:month(match[1]), end:month(match[2]), current:/^(present|current)$/i.test(match[2]), description:cleanExperienceDescription(descriptionLines.join('\n'))};
}
