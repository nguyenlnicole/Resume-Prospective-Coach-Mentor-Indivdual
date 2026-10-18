export function isLinkQuestion(q) {
  return /\b(url|link|github|portfolio|repository)\b/i.test(
    `${q.question || ""} ${q.id || ""}`,
  );
}
const key = (text) => text.toLowerCase().replace(/[^a-z0-9]/g, "");
const projectName = (text) =>
  text
    .split("\n")[0]
    .split("|")[0]
    .split(/[—–]/)[0]
    .replace(/\[([^\]]+)\]\([^)]*\)/g, "$1")
    .replace(/^[•*\-]\s+/, "")
    .trim();
const identity = (text) =>
  key(text).replace(
    /(?:webpage|webapplication|website|application|frontend|backend|project|app)$/g,
    "",
  );
const repoIdentity = (url) => {
  try {
    const parsed = new URL(url);
    return /^(www\.)?github\.com$/i.test(parsed.hostname)
      ? parsed.pathname
          .replace(/\.git\/?$/, "")
          .replace(/\/$/, "")
          .toLowerCase()
      : "";
  } catch {
    return "";
  }
};
export function matchProjectLinks(projects, repos, demoUrl = "") {
  const result = {};
  projects.forEach((project, index) => {
    const name = projectName(project),
      available = repos.filter((repo) => !repo.fork);
    const supplied = [...project.matchAll(/https?:\/\/[^\s|)]+/g)]
      .map((match) => repoIdentity(match[0]))
      .filter(Boolean);
    let matches = available.filter((repo) =>
      supplied.includes(repoIdentity(repo.html_url)),
    );
    if (!matches.length)
      matches = available.filter(
        (repo) => name && key(repo.name) === key(name),
      );
    if (!matches.length && identity(name).length >= 4)
      matches = available.filter(
        (repo) => identity(repo.name) === identity(name),
      );
    if (matches.length !== 1) return;
    const repo = matches[0];
    const links = [repo.html_url, repo.homepage].filter((url) =>
      /^https?:\/\//i.test(url || ""),
    );
    if (links.length) result[index] = links;
  });
  const demoUrls = demoUrl
    .split(/\r?\n/)
    .map((url) => url.trim())
    .filter(Boolean);
  if (demoUrls.length && projects.length === 1)
    result[0] = [...new Set([...(result[0] || []), ...demoUrls])];
  else if (demoUrl) {
    const matches = Object.keys(result).filter((index) =>
      result[index].includes(demoUrl),
    );
    if (matches.length === 1)
      result[matches[0]] = [...new Set(result[matches[0]])];
  }
  return result;
}
export async function discoverProjectLinks(projects, codeUrl, demoUrl) {
  let repos = [];
  if (codeUrl) {
    const parsed = new URL(
      /^https?:\/\//i.test(codeUrl) ? codeUrl : `https://${codeUrl}`,
    );
    const parts = parsed.pathname.split("/").filter(Boolean);
    if (
      /^(www\.)?github\.com$/i.test(parsed.hostname) &&
      parts.length >= 1 &&
      parts.length <= 2
    ) {
      const route =
        parts.length === 2
          ? `repos/${parts.map(encodeURIComponent).join("/")}`
          : `users/${encodeURIComponent(parts[0])}/repos?per_page=100&sort=updated`;
      const response = await fetch(`https://api.github.com/${route}`, {
        signal: AbortSignal.timeout(8000),
        headers: { Accept: "application/vnd.github+json" },
      });
      if (!response.ok) throw new Error("Public repositories unavailable");
      const data = await response.json();
      repos = Array.isArray(data) ? data : [data];

      if (parts.length === 2 && projects.length === 1 && !data.fork)
        repos = [{ ...data, name: projectName(projects[0]) }];
    }
  }
  const found = matchProjectLinks(projects, repos, demoUrl.trim());

  await Promise.allSettled(
    Object.entries(found).map(async ([index, links]) => {
      if (links.some((link) => !repoIdentity(link))) return;
      const repo = repos.find((candidate) =>
        links.includes(candidate.html_url),
      );
      const path = repoIdentity(repo?.html_url);
      if (!path) return;
      const response = await fetch(
        `https://api.github.com/repos${path}/readme`,
        {
          signal: AbortSignal.timeout(8000),
          headers: { Accept: "application/vnd.github+json" },
        },
      );
      if (!response.ok) return;
      const data = await response.json();
      if (data.size > 150000 || typeof data.content !== "string") return;
      const demos = readmeDemoLinks(atob(data.content.replace(/\s/g, "")));
      if (demos.length === 1) found[index] = [...links, demos[0]];
    }),
  );
  return found;
}

export function readmeDemoLinks(text) {
  const links = [];
  for (const line of text.split(/\r?\n/)) {
    if (
      !/\b(live\s+(?:demo|site|app|website)|demo|deployed\s+(?:app|site)|deployment)\b/i.test(
        line,
      )
    )
      continue;
    for (const match of line.matchAll(
      /(?<!!)\[(?:live\s+)?(?:demo|site|app|website|deployment|view demo|try it)[^\]]*\]\((https?:\/\/[^\s)]+)\)/gi,
    )) {
      const url = new URL(match[1]);
      if (
        !/(^|\.)(github\.com|githubusercontent\.com|youtube\.com|youtu\.be)$/i.test(
          url.hostname,
        )
      )
        links.push(url.href);
    }
  }
  return [...new Set(links)];
}
