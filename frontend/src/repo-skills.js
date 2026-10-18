const key = (value) =>
  value
    .trim()
    .toLowerCase()
    .replace(/^nodejs$/, "node.js")
    .replace(/^reactjs$/, "react")
    .replace(/^js$/, "javascript")
    .replace(/^ts$/, "typescript");

export function newlyListedSkills(before = "", after = "") {
  const items = (text) =>
    text
      .split("\n")
      .flatMap((line) => line.replace(/^[^:]+:/, "").split(/[,;|]/))
      .map((value) => value.trim())
      .filter(Boolean);
  const known = new Set(items(before).map(key));
  return items(after).filter((value) => {
    const normalized = key(value);
    if (known.has(normalized)) return false;
    known.add(normalized);
    return true;
  });
}

export function addRepositorySkills(skills = "", technologies = []) {
  const known = new Set(
    skills
      .split("\n")
      .flatMap((line) => line.replace(/^[^:]+:/, "").split(/[,;|]/))
      .map(key),
  );
  const added = [];
  for (const technology of technologies) {
    const name = String(technology).trim();
    if (!name || known.has(key(name))) continue;
    known.add(key(name));
    added.push(name);
  }
  return {
    skills: added.length
      ? [skills.trim(), `Project technologies: ${added.join(", ")}`]
          .filter(Boolean)
          .join("\n")
      : skills,
    added,
  };
}
