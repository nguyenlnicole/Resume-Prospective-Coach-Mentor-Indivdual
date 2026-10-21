export function linkedinProfileUrl(value) {
  try {
    const url=new URL(value);
    return url.protocol==='https:' && ['linkedin.com','www.linkedin.com'].includes(url.hostname) && !url.port && !url.username && !url.password && /^\/in\/[\w%\-]+\/?$/.test(url.pathname) ? `https://www.linkedin.com${url.pathname}` : '';
  } catch { return ''; }
}
export function parseLinkedinExperience(text) {
  const lines=text.slice(0,12000).split(/\r?\n/).map(line=>line.trim()).filter(Boolean).filter((line,i,all)=>line!==all[i-1]);
  const months=['jan','feb','mar','apr','may','jun','jul','aug','sep','oct','nov','dec'];
  const date=/^((?:[A-Za-z]+\.?\s+)?(?:19|20)\d{2})\s*[–—-]\s*(Present|Current|(?:[A-Za-z]+\.?\s+)?(?:19|20)\d{2})(?:\s*[·•|].*)?$/i;
  const month=value=>{const match=value.match(/^([A-Za-z]+)\.?\s+(\d{4})$/);const index=match?months.indexOf(match[1].slice(0,3).toLowerCase()):-1;return index<0?'':`${match[2]}-${String(index+1).padStart(2,'0')}`;};
  const matches=lines.map((line,index)=>({index,match:line.match(date)})).filter(item=>item.match && item.index>=2);
  return matches.slice(0,2).map((item,n)=>{
    const end=matches[n+1]?matches[n+1].index-2:lines.length;
    return {role:lines[item.index-2],employer:lines[item.index-1].split(/\s+[·•]\s+/)[0],start:month(item.match[1]),end:month(item.match[2]),current:/^(present|current)$/i.test(item.match[2]),description:lines.slice(item.index+1,end).join('\n'),enabled:true,replaceOldest:false};
  });
}

export async function linkedinExperienceSource(text, profileUrl, readProfile) {
  const input = text.trim();
  const link = linkedinProfileUrl(input || profileUrl);
  if (link) return (await readProfile(link)).text || '';
  if (!input || /^https?:\/\/\S+$/i.test(input))
    throw Error('Enter a LinkedIn profile URL, or paste the job titles, employers, dates, and descriptions from Experience.');
  return input;
}
