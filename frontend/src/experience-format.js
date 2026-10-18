const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const isRole = line => !/^\s*[•*\-]\s/.test(line) && /\s\|\s.*\b(?:19|20)\d{2}\b/.test(line);
const fullMonths = line => line.replace(/\b(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\.?\s+((?:19|20)\d{2})\b/gi, (_, month, year) => `${months.find(name => name.slice(0, 3).toLowerCase() === month.slice(0, 3).toLowerCase())} ${year}`);
function recency(line) {
  const dates = line.slice(line.lastIndexOf('|') + 1);
  if (/\b(present|current)\b/i.test(dates)) return 999999;
  const values = [...dates.matchAll(/(?:(January|February|March|April|May|June|July|August|September|October|November|December)\s+)?((?:19|20)\d{2})/gi)];
  const last = values.at(-1);
  return last ? Number(last[2]) * 12 + (last[1] ? months.findIndex(m => m.toLowerCase() === last[1].toLowerCase()) : 11) : 0;
}
export function formatExperience(source = '') {
  const groups = [];
  for (const block of source.split(/\n\s*\n/).filter(b => b.trim())) {
    const lines = block.split(/\r?\n/).map(line => isRole(line) ? fullMonths(line) : line);
    const grouped = lines.length > 1 && !isRole(lines[0]) && isRole(lines[1]);
    const previous = groups.at(-1);
    if (grouped && previous?.[0] === lines[0] && isRole(previous[1] || '')) previous.push(...lines.slice(1));
    else groups.push(lines);
  }
  return groups.map(lines => {
    if (lines.length < 2 || isRole(lines[0]) || !isRole(lines[1])) return lines.join('\n');
    const roles = [];
    for (const line of lines.slice(1)) {
      if (isRole(line)) roles.push([line]);
      else roles.at(-1)?.push(line);
    }
    roles.sort((a, b) => recency(b[0]) - recency(a[0]));
    return [lines[0], ...roles.flat()].join('\n');
  }).join('\n\n');
}