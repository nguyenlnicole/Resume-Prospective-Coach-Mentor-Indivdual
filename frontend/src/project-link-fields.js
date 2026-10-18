export function projectLinkFieldValues(value = "") {
  const links = value
    .split(/[\s|,]+/)
    .map((link) => link.trim())
    .filter(Boolean);
  const repo = links.filter((link) =>
    /^(?:https?:\/\/)?(?:www\.)?(?:github\.com|gitlab\.com|bitbucket\.org)\//i.test(
      link,
    ),
  );
  return {
    repo: repo.join(" | "),
    demo: links.filter((link) => !repo.includes(link)).join(" | "),
  };
}
export function combineProjectLinkFields({ repo = "", demo = "" }) {
  return [repo.trim(), demo.trim()].filter(Boolean).join(" | ");
}

export function mergeProjectLinkAutofill(
  existing = "",
  discovered = [],
  edited = false,
) {
  if (edited) return existing;
  const current = projectLinkFieldValues(existing),
    found = projectLinkFieldValues(discovered.join(" | "));
  return combineProjectLinkFields({
    repo: current.repo || found.repo.split(" | ")[0],
    demo: current.demo || found.demo,
  });
}

function projectKey(heading) {
  return heading
    .replace(/\[([^\]]+)\]\([^)]*\)/g, "$1")
    .split("|")[0]
    .split(/\s+[—–]\s+/)[0]
    .replace(/^[•*\-]\s+/, "")
    .replace(/\s+Web\s*(?:Page|Application)$/i, "")
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]/gu, "");
}
export function preserveProjectHeadings(original = "", rewritten = "") {
  const supplied = new Map();
  for (const block of original.split(/\n\s*\n/)) {
    const heading = block.split("\n")[0],
      key = projectKey(heading);
    if (key) supplied.set(key, [...(supplied.get(key) || []), heading]);
  }
  return rewritten
    .split(/\n\s*\n/)
    .map((block) => {
      const heading = block.split("\n")[0],
        matches = supplied.get(projectKey(heading));
      return matches?.length === 1
        ? matches[0] + block.slice(heading.length)
        : block;
    })
    .join("\n\n");
}

export function projectLinksFromEdits(original = "", edited = "") {
  const blocks = edited.split(/\n\s*\n/),
    result = {};
  original.split(/\n\s*\n/).forEach((block, index) => {
    const matches = blocks.filter(
      (candidate) =>
        projectKey(candidate.split("\n")[0]) ===
        projectKey(block.split("\n")[0]),
    );
    if (matches.length === 1)
      result[index] = [
        ...matches[0].split("\n")[0].matchAll(/https?:\/\/[^\s|)]+/g),
      ]
        .map((match) => match[0])
        .join(" | ");
  });
  return result;
}
