import { githubRepository, inspectGithubRepository } from './github-repo.js';

export function safeProjectUrl(value = '') {
  try { const url = new URL(value); return ['https:', 'http:'].includes(url.protocol) ? url.href : ''; } catch { return ''; }
}

export function readmeOverview(markdown = '') {
  const clean = text => text
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]*)\]\[[^\]]*\]/g, '$1')
    .replace(/https?:\/\/\S+|mailto:\S+|[\w.+-]+@[\w.-]+\.[a-z]{2,}/gi, '')
    .replace(/<[^>]*>/g, ' ').replace(/[*_`\[\]]/g, '').replace(/\s+/g, ' ').trim();
  const candidates = [];
  let section = '', paragraph = [];
  const flush = () => {
    const text = clean(paragraph.join(' '));
    paragraph = [];
    if (/contact|connect|install|setup|getting started|quick start|usage|license|contribut|author|acknowledg|tech stack|requirements|deployment|social/i.test(section)) return;
    if (text.split(/\s+/).length < 8 || !/[a-z]{3}/i.test(text) || /^(npm |git |pip |yarn |docker |copyright|follow me|connect with|hi[, !]|hello[, !])/i.test(text)) return;
    candidates.push({text, score: /overview|about|description|purpose|what .*does/i.test(section) ? 3 : /features|capabilities/i.test(section) ? 2 : 1});
  };
  for (const line of markdown.slice(0,100000).replace(/```[\s\S]*?```|~~~[\s\S]*?~~~/g, '').replace(/<!--[\s\S]*?-->/g, '').split(/\r?\n/)) {
    const heading = line.match(/^\s{0,3}#{1,6}\s+(.+)/);
    if (heading) { flush(); section = clean(heading[1]); continue; }
    if (!line.trim()) { flush(); continue; }
    if (/^\s*(?:\[.*\]:|[|>]|[-=]{3,}\s*$)/.test(line)) continue;
    paragraph.push(line.replace(/^\s*[-+*]\s+/, ''));
  }
  flush();
  return candidates.sort((a,b) => b.score-a.score).slice(0,2).map(item => item.text).join('\n\n').slice(0,4000);
}

export function publicFileOverview(path, text) {
  if (/\.md$/i.test(path)) return readmeOverview(text);
  if (/package\.json$/i.test(path)) { try { return String(JSON.parse(text).description || '').slice(0,1000); } catch { return ''; } }
  if (/\.html?$/i.test(path)) {
    const page = text.replace(/<(script|style)[^>]*>[\s\S]*?<\/\1>/gi, '');
    const plain = value => value.replace(/<[^>]*>/g,' ').replace(/&(?:nbsp|amp|quot);/g,' ').replace(/\s+/g,' ').trim();
    const tag = page.match(/<meta\b(?=[^>]*\bname\s*=\s*["']description["'])[^>]*>/i)?.[0];
    const description = tag?.match(/\bcontent\s*=\s*["']([^"']+)["']/i)?.[1];
    if (description) return plain(description);
    const title = plain(page.match(/<title[^>]*>([\s\S]*?)<\/title>/i)?.[1] || '');
    const headings = [...page.matchAll(/<h[12][^>]*>([\s\S]*?)<\/h[12]>/gi)].map(match => plain(match[1])).filter(Boolean);
    return title || headings.length ? `Web page${title ? ` titled "${title}"` : ''}.${headings.length ? ` Page sections: ${[...new Set(headings)].slice(0,4).join('; ')}.` : ''}` : '';
  }
  if (/\.py$/i.test(path)) {
    const functions = [...text.matchAll(/^def ([a-zA-Z_]\w*)\(/gm)].map(match=>match[1]);
    return functions.length ? `Python program with functions for ${functions.slice(0,5).map(name=>name.replace(/_/g,' ')).join(', ')}. Review the source to confirm its purpose and your contribution.` : '';
  }
  return '';
}

export async function fetchProjectDetails(url) {
  const id = githubRepository(url);
  if (!id) throw new Error('Enter the project repository link, such as https://github.com/yourname/project.');
  if (id[0].toLowerCase() === id[1].toLowerCase()) throw new Error('This is a GitHub profile README, not a project. Choose a project repository from that account.');
  const root = `https://api.github.com/repos/${id.map(encodeURIComponent).join('/')}`;
  const response = await fetch(root, { signal: AbortSignal.timeout(8000), headers: { Accept: 'application/vnd.github+json' } });
  if (!response.ok) throw new Error('This repository could not be read. Check that it is public, try later, or enter the details yourself.');
  const repo = await response.json();
  const optionalJson = async path => {
    try {
      const response = await fetch(`${root}/${path}`, { signal: AbortSignal.timeout(8000), headers: { Accept: 'application/vnd.github+json' } });
      return response.ok ? await response.json() : null;
    } catch { return null; }
  };
  const [technologies, readme, commits] = await Promise.all([
    inspectGithubRepository(url).catch(() => []), optionalJson('readme'),
    optionalJson(`commits?author=${encodeURIComponent(id[0])}&per_page=20`),
  ]);
  let overview = '';
  if (readme?.encoding === 'base64' && readme.size <= 100000 && typeof readme.content === 'string') {
    try {
      const markdown = new TextDecoder().decode(Uint8Array.from(atob(readme.content.replace(/\s/g, '')), c => c.charCodeAt(0)));
      overview = readmeOverview(markdown);
    } catch { overview = ''; }
  }
  if (!overview) {
    const tree = await optionalJson('git/trees/HEAD?recursive=1');
    const candidates = (tree?.tree || []).filter(item => item.type === 'blob' && item.size <= 100000
      && !/(^|\/)(node_modules|vendor|dist|build|\.git|test|tests)\//i.test(item.path)
      && /(?:overview\.md|readme\.md|package\.json|index\.html?|\.py)$/i.test(item.path));
    const rank = path => /overview\.md|readme\.md/i.test(path) ? 0 : /package\.json/.test(path) ? 1 : /index\.html?/.test(path) ? 2 : 3;
    candidates.sort((a,b)=>rank(a.path)-rank(b.path) || a.path.length-b.path.length);
    const details = await Promise.all(candidates.slice(0,6).map(async item => {
      const data = await optionalJson(`contents/${item.path.split('/').map(encodeURIComponent).join('/')}`);
      if (data?.encoding !== 'base64' || data.size > 100000 || typeof data.content !== 'string') return '';
      try {
        const text = new TextDecoder().decode(Uint8Array.from(atob(data.content.replace(/\s/g,'')), c=>c.charCodeAt(0)));
        return publicFileOverview(item.path,text);
      } catch { return ''; }
    }));
    overview = details.find(Boolean) || '';
  }
  const contributions = Array.isArray(commits) ? [...new Set(commits
    .filter(commit => commit.author?.login?.toLowerCase() === id[0].toLowerCase())
    .map(commit => String(commit.commit?.message || '').split('\n')[0].trim())
    .filter(message => message.length >= 12 && !/^(merge|initial commit|update readme|bump|chore|dependabot)\b/i.test(message)))].slice(0, 3) : [];
  return {
    name: String(repo.name || id[1]).slice(0,160),
    description: String(overview || repo.description || '').slice(0,4000),
    contribution: contributions.length ? `Work recorded for ${id[0]}: ${contributions.join('; ')}.` : '',
    importNote: contributions.length ? `Contribution draft comes from commits by ${id[0]}. Keep only work you personally did, edit the wording as needed.` : 'No clear personal contribution was available from GitHub. Add what you personally built, review the other details.',
    stack: technologies.join(', '), demo: safeProjectUrl(repo.homepage), repo: `https://github.com/${id.join('/')}`, confirmed: false,
  };
}

export function newProjectsError(projects) {
  for (const project of projects) {
    if (!project.name?.trim() || !project.description?.trim()) return 'Add a name and short description for each new project, or remove projects you do not want to include.';
    if (project.onGithub === 'yes' && !githubRepository(project.repo)) return 'Enter a valid GitHub repository link or choose that it is not on GitHub.';
    if (project.demo?.trim() && !safeProjectUrl(project.demo)) return 'Enter a valid project demo URL or leave it blank.';
  }
  return '';
}

export function addNewProjects(existing, projects) {
  return [existing, ...projects.map(project => {
    const clean = (text = '') => text.trim().replace(/\s+/g, ' ');
    const links = [project.onGithub === 'yes' ? project.repo : '', safeProjectUrl(project.demo)].filter(Boolean);
    const heading = `${clean(project.name)}${links.length ? ' | ' + links.join(' | ') : ''}${project.stack?.trim() ? ' \u2014 ' + clean(project.stack) : ''}`;
    return [heading, ...[project.description, project.contribution].filter(text => text?.trim()).map(text => `\u2022 ${clean(text)}`)].join('\n');
  })].filter(Boolean).join('\n\n');
}
