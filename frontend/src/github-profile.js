export function githubProfileFromProjects(projects = '') {
  const owners = new Map();
  for (const match of projects.matchAll(/https?:\/\/(?:www\.)?github\.com\/([A-Za-z0-9-]+)\/([A-Za-z0-9_.-]+)/gi)) {
    const owner = match[1];
    if (['features','topics','collections','orgs','users','settings','marketplace','sponsors'].includes(owner.toLowerCase())) continue;
    owners.set(owner.toLowerCase(), owner);
  }
  return owners.size === 1 ? `https://github.com/${[...owners.values()][0]}` : '';
}
