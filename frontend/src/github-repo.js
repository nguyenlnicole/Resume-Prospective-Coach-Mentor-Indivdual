export function githubRepository(url) {
  try {
    const parsed = new URL(url);
    if (parsed.protocol !== "https:" || parsed.hostname.toLowerCase() !== "github.com") return null;
    const parts = parsed.pathname.replace(/\/$/, "").split("/").filter(Boolean);
    if (parts.length !== 2 || !parts.every((part) => /^[\w.-]+$/.test(part))) return null;
    return parts.map((part) => part.replace(/\.git$/i, ""));
  } catch { return null; }
}

const frameworks = [
  ["react", "React"], ["next", "Next.js"], ["vue", "Vue.js"], ["@angular/core", "Angular"],
  ["express", "Express"], ["typescript", "TypeScript"], ["vite", "Vite"],
  ["tailwindcss", "Tailwind CSS"], ["prisma", "Prisma"], ["firebase", "Firebase"],
  ["socket.io", "Socket.IO"], ["django", "Django"], ["flask", "Flask"],
  ["fastapi", "FastAPI"], ["streamlit", "Streamlit"], ["pandas", "pandas"],
  ["postgresql", "PostgreSQL"], ["spring-boot", "Spring Boot"],
];
function namedDependencies(text) {
  const lower = text.toLowerCase();
  return frameworks.filter(([key]) => lower.includes(key)).map(([, name]) => name);
}
async function apiJson(url) {
  const response = await fetch(url, { signal: AbortSignal.timeout(8000), headers: { Accept: "application/vnd.github+json" } });
  if (!response.ok) throw new Error(response.status === 403 ? "GitHub is limiting public lookups. Try again later." : "This public repository could not be read.");
  return response.json();
}
const repositoryCache = new Map();
export async function inspectGithubRepository(url) {
  const id = githubRepository(url);
  if (!id) throw new Error("Add a public GitHub repository URL for this project first.");
  const key = id.join('/').toLowerCase();
  const cached = repositoryCache.get(key);
  if (cached && Date.now() - cached.started < 300000) return [...await cached.promise];
  const entry = {started:Date.now(),promise:loadGithubRepository(url)};
  repositoryCache.set(key,entry);
  if (repositoryCache.size > 100) repositoryCache.delete(repositoryCache.keys().next().value);
  try { return [...await entry.promise]; }
  catch (error) { if(repositoryCache.get(key) === entry) repositoryCache.delete(key); throw error; }
}
async function loadGithubRepository(url) {
  const id = githubRepository(url);
  if (!id) throw new Error("Add a public GitHub repository URL for this project first.");
  const root = `https://api.github.com/repos/${encodeURIComponent(id[0])}/${encodeURIComponent(id[1])}`;
  const manifests = ["package.json", "pom.xml", "build.gradle", "requirements.txt", "pyproject.toml"];
  const fetched = await Promise.allSettled([apiJson(`${root}/languages`), ...manifests.map(async (name) => {
    const response = await fetch(`${root}/contents/${name}`, { signal: AbortSignal.timeout(8000), headers: { Accept: "application/vnd.github+json" } });
    if (!response.ok) return "";
    const data = await response.json();
    if (data.size > 100_000 || typeof data.content !== "string") return "";
    return atob(data.content.replace(/\s/g, ""));
  })]);
  if (fetched[0].status === 'rejected') throw fetched[0].reason;
  const languages = fetched[0].value;
  const choices = Object.keys(languages).sort((a,b) => languages[b]-languages[a]).slice(0,5);
  const results = fetched.slice(1);
  for (const result of results) if (result.status === "fulfilled") choices.push(...namedDependencies(result.value));
  return [...new Set(choices)].slice(0, 15);
}
