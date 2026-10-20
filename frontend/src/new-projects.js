import { githubRepository, inspectGithubRepository } from './github-repo.js';

export function safeProjectUrl(value = '') {
  try { const url = new URL(value); return ['https:', 'http:'].includes(url.protocol) ? url.href : ''; } catch { return ''; }
}

export async function fetchProjectDetails(url) {
  const id = githubRepository(url);
  if (!id) throw new Error('Enter the project repository link, such as https://github.com/yourname/project.');
  const root = `https://api.github.com/repos/${id.map(encodeURIComponent).join('/')}`;
  const response = await fetch(root, { signal: AbortSignal.timeout(8000), headers: { Accept: 'application/vnd.github+json' } });
  if (!response.ok) throw new Error('This repository could not be read. Check that it is public, try later, or enter the details yourself.');
  const repo = await response.json();
  const technologies = await inspectGithubRepository(url).catch(() => []);
  return { name: String(repo.name || id[1]).slice(0,160), description: String(repo.description || '').slice(0,4000), stack: technologies.join(', '), demo: safeProjectUrl(repo.homepage), repo: `https://github.com/${id.join('/')}`, confirmed: false };
}

export function newProjectsError(projects) {
  for (const project of projects) {
    if (!project.name?.trim() || !project.description?.trim() || !project.contribution?.trim() || !project.stack?.trim()) return 'Complete each new project’s name, description, your contribution, and technologies.';
    if (project.onGithub === 'yes' && !githubRepository(project.repo)) return 'Enter a valid GitHub repository link or choose that it is not on GitHub.';
    if (project.demo?.trim() && !safeProjectUrl(project.demo)) return 'Enter a valid project demo URL or leave it blank.';
    if (!project.confirmed) return 'Confirm the details of each new project before creating your draft.';
  }
  return '';
}

export function addNewProjects(existing, projects) {
  return [existing, ...projects.map(project => {
    const clean = text => text.trim().replace(/\s+/g, ' ');
    const links = [project.onGithub === 'yes' ? project.repo : '', safeProjectUrl(project.demo)].filter(Boolean);
    return `${clean(project.name)}${links.length ? ' | ' + links.join(' | ') : ''} — ${clean(project.stack)}\n• ${clean(project.description)}\n• ${clean(project.contribution)}`;
  })].filter(Boolean).join('\n\n');
}
