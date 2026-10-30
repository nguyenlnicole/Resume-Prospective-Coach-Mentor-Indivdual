import { githubRepository, inspectGithubRepository } from './github-repo.js';

const key = value => value.trim().toLowerCase().replace(/^node\.?js$/, 'node.js').replace(/^react\.?js$/, 'react').replace(/^express\.?js$/, 'express').replace(/^tsql$/, 't-sql').replace(/^js$/, 'javascript').replace(/^ts$/, 'typescript');

export async function collectRepositorySkills(urls, inspect = inspectGithubRepository) {
  const repositories = [...new Map(urls.map(url => githubRepository(url)).filter(Boolean)
    .map(parts => [parts.join('/').toLowerCase(), `https://github.com/${parts.join('/')}`])).values()];
  const results = await Promise.allSettled(repositories.map(url => inspect(url)));
  return {
    technologies: [...new Set(results.flatMap(result => result.status === 'fulfilled' ? result.value : []))],
    failed: repositories.filter((_, index) => results[index].status === 'rejected'),
  };
}

export function retainSkills(original = '', rewritten = '') {
  return newlyListedSkills(rewritten, original).length ? original : rewritten;
}

export function newlyListedSkills(before = '', after = '') {
  const items = text => text.split('\n').flatMap(line => line.replace(/^[^:]+:/, '').split(/[,;|]/)).map(value => value.trim()).filter(Boolean);
  const known = new Set(items(before).map(key));
  return items(after).filter(value => { const normalized = key(value); if (known.has(normalized)) return false; known.add(normalized); return true; });
}

export function addRepositorySkills(skills = '', technologies = []) {
  const known = new Set(skills.split('\n').flatMap(line => line.replace(/^[^:]+:/, '').split(/[,;|]/)).map(key));
  const added = [];
  for (const technology of technologies) {
    const name = String(technology).trim();
    if (!name || known.has(key(name))) continue;
    known.add(key(name)); added.push(name);
  }
  return {skills: added.length ? [skills.trim(), `Project technologies: ${added.join(', ')}`].filter(Boolean).join('\n') : skills, added};
}
