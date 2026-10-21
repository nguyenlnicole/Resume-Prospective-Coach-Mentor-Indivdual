const terms = ['java','javascript','typescript','python','react','angular','vue','spring','postgresql','sql','api','frontend','backend','full-stack','machine learning','automation','testing','data','mobile','android','ios','security','cloud','docker','kubernetes'];
const matches = (text, term) => new RegExp(`(^|[^a-z])${term}([^a-z]|$)`, 'i').test(text);
const nameKey = text => text.split('\n')[0].split('|')[0].split(/[—–]/)[0].toLowerCase().replace(/[^a-z0-9]/g,'');
export function suggestProjects(projects, repos, target) {
  const relevant = terms.filter(term => matches(target,term));
  const needsProjects = projects.length < 3;
  if (!needsProjects && !relevant.length) return [];
  const score = text => relevant.filter(term=>matches(text,term));
  const current = projects.map((project,index)=>({index,score:score(project).length})).sort((a,b)=>a.score-b.score)[0];
  return repos.filter(repo=>!repo.fork && !repo.archived && !repo.is_template && repo.size > 0
    && /^https:\/\/github\.com\/[^/]+\/[^/]+\/?$/.test(repo.html_url || '')
    && !projects.some(project=>nameKey(project)===nameKey(repo.name) || project.includes(repo.html_url)))
    .map(repo=>({name:repo.name,url:repo.html_url,description:repo.description || '',
      matched:score([repo.name,repo.description,repo.language,...(repo.topics || [])].filter(Boolean).join(' ')),replaceIndex:needsProjects ? null : current.index}))
    .filter(repo=>needsProjects || repo.matched.length > current.score)
    .sort((a,b)=>b.matched.length-a.matched.length).slice(0,3);
}
