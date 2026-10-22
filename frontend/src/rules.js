import { formatExperience } from "./experience-format.js";
import { normalizeCredentials } from "./credential-format.js";
export const isTechnical = (level) =>
  !["General", "Normal", "Medical Field", "Educator"].includes(level);
export const codingRequired = (resume) => resume?.softwareEngineering === true;
export const countWords = (s) => (s.trim().match(/\S+/g) || []).length;
export function normalizeWebLink(value = "") {
  const text = value.trim();
  if (!text || /^[a-z][a-z0-9+.-]*:/i.test(text)) return text;
  return "https://" + text.replace(/^\/\//, "");
}
export function githubUsername(value) {
  try {
    const url = new URL(normalizeWebLink(value));
    if (
      url.protocol !== "https:" ||
      !["github.com", "www.github.com"].includes(url.hostname.toLowerCase())
    )
      return "";
    const parts = url.pathname.split("/").filter(Boolean);
    return parts.length === 1 &&
      /^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$/.test(parts[0])
      ? parts[0]
      : "";
  } catch {
    return "";
  }
}
export function briefFeedback(value) {
  const text = String(value || "").replace(/\s+/g, " ").trim();
  if (!text) return "This check needs another look. Open More details for guidance.";
  if (countWords(text) <= 25 && countSentences(text) <= 3) return text;
  const capped = text.split(/\s+/).slice(0, 25).join(" ");
  const sentences = capped.match(/[^.!?]+[.!?]*/g) || [];
  const short = sentences.slice(0, 3).join(" ").trim().replace(/[,:;—-]+$/, "");
  return short === text ? short : short.replace(/[.!?]+$/, "") + "…";
}
export function briefNextStep(value) {
  const text = String(value || "").replace(/\s+/g, " ").trim();
  if (!text) return "";
  const sentences = [...new Intl.Segmenter("en", { granularity: "sentence" }).segment(text)].map(part => part.segment.trim());
  const selected = sentences.slice(0, 2).join(" ");
  const words = selected.split(/\s+/);
  return words.length <= 23 ? selected : words.slice(0, 23).join(" ").replace(/[,:;.!?—-]+$/, "") + "…";
}
export function resumeFilename(
  name = "",
  extension = "pdf",
  date = new Date(),
) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const safe = (part, fallback) =>
    part
      .normalize("NFKD")
      .replace(/\p{M}/gu, "")
      .replace(/[^A-Za-z0-9]+/g, "-")
      .replace(/^-|-$/g, "") || fallback;
  const first = safe(parts[0] || "", "Resume");
  const last = safe(parts.length > 1 ? parts.at(-1) : "", "Owner");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = String(date.getFullYear() % 100).padStart(2, "0");
  return `${first}-${last}-PNBRP-${month}${year}.${extension}`;
}
export const educationMonths = [
  "January",
  "February",
  "March",
  "April",
  "May",
  "June",
  "July",
  "August",
  "September",
  "October",
  "November",
  "December",
];
const educationMonthNames = educationMonths
  .flatMap((month) => [
    month,
    month.slice(0, 3) + "\\.?",
    ...(month === "September" ? ["Sept\\.?"] : []),
  ])
  .join("|");
const educationDateToken = `(?:(?:${educationMonthNames})\\s+)?(?:19|20)\\d{2}`;
const educationDateTail = new RegExp(
  `(?:\\s*\\|\\s*|\\s+|,\\s*)(?:(Graduated|Expected)\\s+)?(${educationDateToken})(?:\\s*[–—-]\\s*(${educationDateToken}|Present))?$`,
  "i",
);
const educationDateLine = new RegExp(
  `^(?:(?:Graduated|Expected)\\s+)?${educationDateToken}(?:\\s*[–—-]\\s*(?:${educationDateToken}|Present))?$`,
  "i",
);
function splitEducationDate(heading) {
  let details = heading.trim(),
    result = null,
    match;
  while ((match = details.match(educationDateTail))) {
    if (!result) {
      const end = match[3] || match[2];
      const date = end.match(/^(?:(\w+)\.?\s+)?((?:19|20)\d{2})$/i);
      if (!date) return null;
      const month = date[1]
        ? educationMonths.find(
            (value) =>
              value.slice(0, 3).toLowerCase() ===
              date[1].slice(0, 3).toLowerCase(),
          )
        : "";
      const future =
        Number(date[2]) > new Date().getFullYear() ||
        (Number(date[2]) === new Date().getFullYear() &&
          month &&
          educationMonths.indexOf(month) > new Date().getMonth());
      result = {
        status:
          /^expected$/i.test(match[1] || "") || (!match[1] && future)
            ? "progress"
            : "graduated",
        month,
        year: date[2],
      };
    }
    details = details.slice(0, match.index).trim();
  }
  return result ? { details, ...result } : null;
}

export function compactEducation(text = "") {
  const entries = [];
  const description =
    /^(?:[•*\-]\s|(?:relevant\s+)?coursework\s*:|(?:completed|attended|focused|studied|learned|developed|participated|accelerated\s+.+\s+program\s+focused)\b)/i;
  const degree =
    /^(?:bachelor|master|doctor|associate|b\.?s\.?\b|b\.?a\.?\b|m\.?s\.?\b|m\.?a\.?\b|mba\b|ph\.?d\.?\b|diploma\b|certificate\b|degree\s*:)/i;
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim();
    if (!line || /^education:?$/i.test(line) || description.test(line))
      continue;
    const previous = entries.at(-1);
    if (previous && degree.test(line) && !/\s[—–-]\s/.test(previous)) {
      entries[entries.length - 1] = `${previous} — ${line}`;
    } else if (previous && educationDateLine.test(line)) {
      entries[entries.length - 1] += ` | ${line}`;
    } else if (
      !previous ||
      /\s[—–-]\s|\s\|\s/.test(line) ||
      /\b(?:university|college|school|institute|academy|bootcamp)\b/i.test(line)
    ) {
      entries.push(line);
    }
  }
  return entries
    .map((heading) => {
      const parsed = splitEducationDate(heading);
      if (!parsed) return heading.replace(/\s*\|\s*Graduated$/i, "");
      const label = /\bExpected\b/i.test(heading) ? "Expected " : "";
      return `${parsed.details} | ${label}${[parsed.month, parsed.year].filter(Boolean).join(" ")}`;
    })
    .join("\n\n");
}

export function parseEducation(text = "") {
  const blocks = compactEducation(text)
    .split(/\n\s*\n/)
    .map((entry) => entry.trim())
    .filter(Boolean);
  if (!blocks.length) return [{ details: "", status: "", month: "", year: "" }];
  return blocks.map((block) => {
    const [heading, ...otherLines] = block.split(/\r?\n/);
    const detailsWith = (firstLine) =>
      [firstLine.trim(), ...otherLines].join("\n").trim();
    const dated = splitEducationDate(heading);
    if (dated) return { ...dated, details: detailsWith(dated.details) };
    for (const [suffix, status] of [
      [/\s*\|\s*In progress$/i, "progress"],
      [/\s*\|\s*Not completed$/i, "incomplete"],
    ]) {
      if (suffix.test(heading))
        return {
          details: detailsWith(heading.replace(suffix, "")),
          status,
          month: "",
          year: "",
        };
    }
    if (/\s*\|\s*Graduated$/i.test(heading))
      return {
        details: detailsWith(heading.replace(/\s*\|\s*Graduated$/i, "")),
        status: "graduated",
        month: "",
        year: "",
      };
    return { details: block, status: "", month: "", year: "" };
  });
}

export function educationEntryError(entry) {
  if (!entry.details.trim()) return "Add the school and degree or program.";
  if (!entry.status || entry.status === "incomplete") return "";
  if (entry.month && !entry.year)
    return "Enter the graduation year, or leave both blank.";
  if (entry.year) {
    const year = Number(entry.year);
    if (!Number.isInteger(year) || year < 1900 || year > 2099)
      return "Enter a four-digit year.";
    const date = year * 12 + educationMonths.indexOf(entry.month);
    const now = new Date();
    const current = now.getFullYear() * 12 + now.getMonth();
    if (
      entry.status === "graduated" &&
      (entry.month ? date > current : year > now.getFullYear())
    )
      return "A graduation date cannot be in the future.";
    if (
      entry.status === "progress" &&
      (entry.month ? date < current : year < now.getFullYear())
    )
      return "An expected graduation date must be this month or later.";
  }
  return "";
}

export function formatEducation(entries) {
  return entries
    .map(({ details, status, month, year }) => {
      const imported = splitEducationDate(
        compactEducation(details).split(/\r?\n/)[0] || "",
      );
      const heading =
        imported?.details ?? compactEducation(details).split(/\r?\n/)[0] ?? "";
      const date = [month, year].filter(Boolean).join(" ");
      const suffix =
        status === "graduated"
          ? date
          : status === "progress"
            ? date
              ? `Expected ${date}`
              : "In progress"
            : status === "incomplete"
              ? "Not completed"
              : "";
      return suffix ? `${heading} | ${suffix}` : heading;
    })
    .join("\n\n");
}

export function educationErrors(text = "") {
  return parseEducation(text).map(educationEntryError).filter(Boolean);
}
export const countSentences = (s) =>
  (
    s
      .trim()
      .replace(/\b(?:e\.g|i\.e|B\.S|M\.S|Ph\.D|U\.S)\./gi, "abbreviation")
      .replace(/\b(?:Mr|Ms|Mrs|Dr|Prof|Sr|Jr|Inc|Ltd|etc)\./gi, "abbreviation")
      .replace(/(?<=\d)\.(?=\d)/g, "decimal")
      .replace(/\b(?:Node|Next|Vue|React)\.js\b/gi, "technology")
      .match(/[^.!?]+(?:[.!?]+|$)/g) || []
  ).filter((x) => x.trim()).length;
export function safeLink(value) {
  try {
    const u = new URL(value);
    return ["https:", "http:"].includes(u.protocol) ? u.href : null;
  } catch {
    return null;
  }
}
export const requiresProjects = (level) =>
  ["Entry / Junior", "Mid-level"].includes(level);
export const projectList = (text = "") =>
  stripSectionHeading(text, "projects")
    .split(/\n\s*\n/)
    .map((project) => project.trim())
    .filter(Boolean);
export const projectRepository = (project = "") =>
  project
    .split(/\r?\n/, 1)[0]
    .match(
      /https?:\/\/(?:www\.)?(?:github\.com|gitlab\.com|bitbucket\.org)\/[\w.-]+\/[\w.-]+/i,
    )?.[0] || "";
export const projectDemo = (project = "") => {
  const heading = project.split(/\r?\n/, 1)[0];
  const repository = projectRepository(project);
  return (
    [...heading.matchAll(/https?:\/\/[^\s|)]+/g)]
      .map((match) => match[0])
      .find((url) => url !== repository) || ""
  );
};
export const projectLinkUrls = (project = "") =>
  [...project.split(/\r?\n/, 1)[0].matchAll(/https?:\/\/[^\s|)]+/g)].map(
    (match) => match[0],
  );
export function projectLinkError(links = "") {
  const urls = parseProjectLinks(links);
  if (!urls.length) return "";
  if (
    urls.some((url) => !safeLink(url) || !new URL(url).hostname.includes("."))
  )
    return "Check the web address, or leave this optional field blank.";
  return "";
}
export function parseProjectLinks(value = "") {
  return value
    .split(/[\s|,]+/)
    .map((part) => normalizeWebLink(part.trim()))
    .filter(Boolean);
}
export function replaceProjectLinks(project = "", links = "") {
  const [heading, ...details] = project.split(/\r?\n/);
  const stack =
    heading.match(/\s+[—–]\s+((?!(?:https?:\/\/|\[)).+)$/)?.[1] || "";
  const first = heading
    .replace(/\s+[—–]\s+(?!(?:https?:\/\/|\[)).+$/, "")
    .split("|")[0]
    .trim();
  const markedName = first.match(/^\[([^\]]+)\]\(https?:/);
  const name = markedName
    ? markedName[1].replace(
        /\s+(?:Web Page|Web Application|GitHub(?: Repo)?)$/,
        "",
      )
    : first;
  const originalLinks = [
    ...heading.matchAll(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g),
  ];
  const urls = parseProjectLinks(links).map((url) => {
    const index = originalLinks.findIndex((match) => match[2] === url);
    return index < 0 ? url : originalLinks.splice(index, 1)[0][0];
  });
  return [
    name +
      (urls.length ? " | " + urls.join(" | ") : "") +
      (stack ? ` — ${stack}` : ""),
    ...details,
  ].join("\n");
}
export function replaceProjectStack(project = "", stack = "") {
  const lines = project.split(/\r?\n/);
  const heading = (lines.shift() || "")
    .replace(/\s+[—–]\s+(?!(?:https?:\/\/|\[)).+$/, "")
    .trim();
  const details = lines.filter(
    (line) => !/^\s*(?:tech(?:nologies)?|stack|languages?)\s*:/i.test(line),
  );
  return [
    heading + (stack.trim() ? ` — ${stack.trim()}` : ""),
    ...details,
  ].join("\n");
}
export const normalizeProjectHeadings = (projects = "") =>
  projectList(projects)
    .map((project) => {
      const lines = replaceProjectStack(project, projectStack(project)).split(
        /\r?\n/,
      );
      const urls = new Set(projectLinkUrls(lines[0]));
      const extraUrls = [];
      const body = lines.slice(1).filter((line) => {
      const linkOnly = line.replace(/\[[^\]]*\]\(https?:\/\/[^)]+\)/g, '').replace(/https?:\/\/[^\s|)]+/g, '').replace(/\b(?:github|gitlab|bitbucket|repo(?:sitory)?|live|demo|links?)\b/gi, '').replace(/[\s:|•*–—-]/g, '') === '';
      if (!linkOnly && !/^\s*(?:demo|live demo|repository|repo|github(?: repo)?|links?)\s*:/i.test(line)) return true;
        const found = [...line.matchAll(/https?:\/\/[^\s|)]+/g)].map(
          (m) => m[0],
        );
        if (!found.length) return true;
        found.forEach((url) => {
          if (!urls.has(url)) extraUrls.push(url);
          urls.add(url);
        });
        return false;
      });
      if (/\[[^\]]+\]\(https?:/.test(lines[0])) {
        const stack =
          lines[0].match(/\s+[—–]\s+((?!(?:https?:\/\/|\[)).+)$/)?.[1] || "";
        const heading = lines[0].replace(
          /\s+[—–]\s+(?!(?:https?:\/\/|\[)).+$/,
          "",
        );
        return formatProjectLinks(
          [
            heading +
              extraUrls.map((url) => ` | ${url}`).join("") +
              (stack ? ` — ${stack}` : ""),
            ...body,
          ].join("\n"),
        );
      }
      return replaceProjectLinks(
        [lines[0], ...body].join("\n"),
        [...urls].join(" | "),
      );
    })
    .join("\n\n");

export function stripSectionHeading(text = "", section) {
  const patterns = {
    clinicalRotations: /^clinical rotations$/i,
    clinicalTraining: /^clinical training$/i,
    proceduresSpecialty: /^procedures\s*(?:&|and)\s*specialty experience$/i,
    experience:
      /^(?:(?:additional|earlier|previous|other|professional|work|relevant)\s+)?(?:experience|jobs|roles|companies|employment history|work history)$/i,
    skills: /^(?:technical |professional )?skills$/i,
    projects: /^(?:technical |personal |selected |software )?projects$/i,
    education: /^education$/i,
    certifications: /^certifications(?:\s*(?:&|and)\s*training)?$/i,
    summary: /^(?:professional )?summary$/i,
  };
  return text
    .split(/\r?\n/)
    .filter(
      (line) =>
        !patterns[section]?.test(line.trim().replace(/^#+\s*|[*_:]+/g, "")),
    )
    .join("\n")
    .trim();
}
export function normalizeResume(resume) {
  const result = { ...resume };
  for (const key of [
    "experience",
    "clinicalRotations",
    "clinicalTraining",
    "proceduresSpecialty",
    "skills",
    "projects",
    "education",
    "certifications",
    "summary",
  ])
    result[key] = stripSectionHeading(result[key] || "", key);
  result.experience = formatExperience(newestExperienceFirst(result.experience));
  result.projects = normalizeProjectHeadings(result.projects);
  result.education = compactEducation(result.education);
  result.certifications = normalizeCredentials(result.certifications);
  return result;
}
export function projectStack(project = "") {
  const lines = project.split(/\r?\n/);
  const labeled = lines
    .slice(1)
    .find((line) =>
      /^\s*(?:tech(?:nologies)?|stack|languages?)\s*:/i.test(line),
    );
  if (labeled)
    return labeled
      .replace(/^\s*(?:tech(?:nologies)?|stack|languages?)\s*:\s*/i, "")
      .trim();
  return (
    lines[0]
      ?.split(/\s+[—–]\s+/)
      .slice(1)
      .join(" — ")
      .trim() || ""
  );
}
export const credibleProjectStack = (value = "") =>
  Boolean(value.trim()) &&
  !/^(?:have(?:\s+have)?|yes|no|n\/a|none|unknown|not sure|tbd)$/i.test(
    value.trim(),
  );
export function formatProjectLinks(projects = "", style = "compact") {
  return projectList(projects)
    .map((project) => {
      const [heading, ...body] = project.split(/\r?\n/);
      const stack =
        heading.match(/\s+[—–]\s+((?!(?:https?:\/\/|\[)).+)$/)?.[1] || "";
      const base = heading
        .replace(/\s+[—–]\s+(?!(?:https?:\/\/|\[)).+$/, "")
        .replace(/^[•*\-]\s+/, "");
      const first = base.split(/\s*\|\s*/)[0];
      const name = first
        .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
        .trim()
        .replace(/(\bWeb\s*Page)(?:\s+Web\s*Page)+$/i, "$1")
        .replace(
          /^(.+?) (?:Web Page|Web Application|GitHub(?: Repo)?)$/,
          (whole, title) => (first.startsWith("[") ? title : whole),
        );
      const labelName =
        name.replace(/\s+Web\s*(?:Page|Application)$/i, "") || name;

      const demos = [],
        repositories = [];
      for (const match of base.matchAll(
        /\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)|(https?:\/\/[^\s|)]+)/g,
      )) {
        const label = match[1] || "",
          url = match[2] || match[3];
        const isRepo =
          /\b(?:repo(?:sitory)?|github(?: repo)?|gitlab(?: repo)?|bitbucket(?: repo)?)(?:\s+\d+)?$/i.test(
            label,
          );
        const isDemo =
          /\b(?:demo|view project|web\s*(?:page|application)|website|live|preview)(?:\s+\d+)?$/i.test(
            label,
          );
        const target = isRepo
          ? repositories
          : isDemo
            ? demos
            : /https?:\/\/(?:www\.)?(?:github\.com|gitlab\.com|bitbucket\.org)(?:\/|$)/i.test(
                  url,
                )
              ? repositories
              : demos;
        if (!target.includes(url)) target.push(url);
      }
      if ((!demos.length && !repositories.length) || !name) return project;
      const links = [
        ...demos.map(
          (url, i) =>
            `[${style === "descriptive" ? `${labelName} Web Application${i ? ` ${i + 1}` : ""}` : i ? `Demo ${i + 1}` : "Demo"}](${url})`,
        ),
        ...repositories
          .slice(0, 1)
          .map(
            (url) =>
              `[${style === "descriptive" ? `${labelName} GitHub` : "Repo"}](${url})`,
          ),
      ];
      return [
        [
          ...(style === "descriptive" && demos.length ? [] : [name]),
          ...links,
        ].join(" | ") + (stack ? ` — ${stack}` : ""),
        ...body,
      ].join("\n");
    })
    .join("\n\n");
}
export function projectDescriptionWords(project = "") {
  return countWords(
    project
      .split(/\r?\n/)
      .slice(1)
      .filter(
        (line) =>
          !/^\s*(?:tech(?:nologies)?|stack|organization)\s*:/i.test(line) &&
          !/\s\|\s[^|]*\b(?:19|20)\d{2}[^|]*$/.test(line),
      )
      .join(" ")
      .replace(/https?:\/\/\S+/g, ""),
  );
}
export function newestExperienceFirst(text = '') {
  const months = ['jan','feb','mar','apr','may','jun','jul','aug','sep','oct','nov','dec'];
  const score = entry => {
    const heading = entry.split(/\r?\n/).filter(line => !/^\s*[•*\-]\s/.test(line)).join(' ');
    const dates = [...heading.matchAll(/(?:(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)\.?\s+|(\d{1,2})\/)?((?:19|20)\d{2})/gi)]
      .map(match => Number(match[3])*12 + (match[1] ? months.indexOf(match[1].slice(0,3).toLowerCase()) : match[2] ? Number(match[2])-1 : 0));
    return [/\b(?:present|current)\b/i.test(heading) ? 1 : 0, dates.at(-1) ?? -1, dates[0] ?? -1];
  };
  return experienceList(text).map(entry=>({entry,rank:score(entry)})).sort((a,b)=>b.rank[0]-a.rank[0] || (a.rank[0] ? b.rank[2]-a.rank[2] : b.rank[1]-a.rank[1]) || b.rank[2]-a.rank[2]).map(item=>item.entry).join('\n\n');
}

export function experienceList(text = "") {
  const entries = [];
  let current = [];
  let employer = "";
  const finish = () => {
    if (current.some((line) => line.trim()))
      entries.push(current.join("\n").trim());
    current = [];
  };
  for (const line of text.split(/\r?\n/)) {
    if (!line.trim()) {
      finish();
      employer = "";
      continue;
    }
    const bullet = /^\s*[•*\-]\s/.test(line);
    const datedHeading =
      !bullet && /\b(?:19|20)\d{2}\b|\b(?:present|current)\b/i.test(line);
    if (datedHeading && current.some((part) => !/^\s*[•*\-]\s/.test(part) && /\b(?:19|20)\d{2}\b|\b(?:present|current)\b/i.test(part))) {
      finish();
      if (employer) current.push(employer);
    }
    if (!bullet && !datedHeading && current.length === 0) employer = line;
    current.push(line);
  }
  finish();
  return entries;
}
export function experienceBullets(entry = "") {
  const bullets = [];
  for (const line of entry.split(/\r?\n/)) {
    if (/^\s*[•*\-]\s+/.test(line))
      bullets.push(line.replace(/^\s*[•*\-]\s+/, "").trim());
    else if (bullets.length && line.trim())
      bullets[bullets.length - 1] += " " + line.trim();
  }
  return bullets;
}
export function experienceBulletLimit(entries, index) {
  return index === 0 ? 4 : 3;
}
export function selectExperienceBullets(entry, selected) {
  let bullet = -1;
  let keep = true;
  return entry
    .split(/\r?\n/)
    .filter((line) => {
      if (/^\s*[•*\-]\s+/.test(line)) {
        bullet++;
        keep = selected.includes(bullet);
      }
      return keep;
    })
    .join("\n");
}
function rankedIndexes(items, targetRole = "") {
  const words = [
    ...new Set(targetRole.toLowerCase().match(/[a-z0-9+#.]{3,}/g) || []),
  ];
  return items
    .map((item, index) => ({
      index,
      score: words.reduce(
        (score, word) => score + (item.toLowerCase().includes(word) ? 1 : 0),
        0,
      ),
    }))
    .sort((a, b) => b.score - a.score || a.index - b.index)
    .map(({ index }) => index);
}
export function suggestedIndexes(items, targetRole = "", limit = 3) {
  return rankedIndexes(items, targetRole)
    .slice(0, limit)
    .sort((a, b) => a - b);
}
export function suggestedProjectIndexes(projects, targetRole = "") {
  const words = [
    ...new Set(targetRole.toLowerCase().match(/[a-z0-9+#.]{3,}/g) || []),
  ];
  return projects
    .map((project, index) => ({
      index,
      score:
        words.reduce(
          (total, word) =>
            total + (project.toLowerCase().includes(word) ? 4 : 0),
          0,
        ) +
        (projectStack(project) ? 2 : 0) +
        (projectLinkUrls(project).length ? 1 : 0) +
        (project.split(/\r?\n/).slice(1).join(" ").trim() ? 1 : 0),
    }))
    .sort((a, b) => b.score - a.score || a.index - b.index)
    .slice(0, 3)
    .map(({ index }) => index)
    .sort((a, b) => a - b);
}
export function experienceYearsCovered(items) {
  const ranges = items.map((item) => {
    const heading =
      item.split("\n").find((line) => /\b(?:19|20)\d{2}\b/.test(line)) ||
      item.split("\n")[0];
    const years = [...heading.matchAll(/\b(?:19|20)\d{2}\b/g)].map(([year]) =>
      Number(year),
    );
    if (!years.length) return null;
    const end =
      years[1] ??
      (/\b(?:present|current|now)\b/i.test(heading)
        ? new Date().getFullYear()
        : null);
    return end === null || end < years[0] ? null : [years[0], end];
  });
  if (ranges.some((range) => range === null)) return null;
  const merged = ranges
    .sort((a, b) => a[0] - b[0])
    .reduce((result, range) => {
      const previous = result.at(-1);
      if (previous && range[0] <= previous[1])
        previous[1] = Math.max(previous[1], range[1]);
      else result.push([...range]);
      return result;
    }, []);
  return merged.reduce((years, [start, end]) => years + end - start, 0);
}
export function suggestedExperienceIndexes(
  items,
  targetRole = "",
  level = "Mid-level",
) {
  if (["General", "Normal"].includes(level))
    return suggestedIndexes(items, targetRole, Math.max(3, items.length));
  if (["Medical Field", "Educator"].includes(level))
    return suggestedIndexes(items, targetRole, items.length);
  if (["Senior", "Management"].includes(level))
    return suggestedIndexes(items, targetRole, 5);
  const ranked = rankedIndexes(items, targetRole);
  let best = null;
  for (let first = 0; first < ranked.length; first++)
    for (let second = first + 1; second < ranked.length; second++)
      for (let third = second + 1; third < ranked.length; third++) {
        const choice = [ranked[first], ranked[second], ranked[third]];
        const years = experienceYearsCovered(
          choice.map((index) => items[index]),
        );
        if (
          years !== null &&
          years >= 8 &&
          (!best || first + second + third < best.rank)
        )
          best = { choice, rank: first + second + third };
      }
  return (best?.choice ?? suggestedIndexes(items, targetRole, 4)).sort(
    (a, b) => a - b,
  );
}
export function validateResume(r, level) {
  r = normalizeResume(r);
  const errors = [];
  for (const key of ["name", "location", "email", "phone"])
    if (!r.contact?.[key]?.trim())
      errors.push(
        `Add your ${key === "name" ? "first and last name" : key === "location" ? "city and state or region" : key}.`,
      );
  if (r.contact?.name?.trim() && r.contact.name.trim().split(/\s+/).length < 2)
    errors.push("Include your first and last name.");
  if (r.contact?.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(r.contact.email))
    errors.push("Enter a valid email address.");
  for (const key of ["linkedin", "github", "portfolio"])
    if (r.contact?.[key] && !safeLink(r.contact[key]))
      errors.push(`Use a valid web address for ${key}.`);
  if (!r.summary?.trim())
    errors.push(
      "Add a professional summary below your contact information, using your actual experience and strengths.",
    );
  if (countWords(r.summary) < 25 || countWords(r.summary) > 45)
    errors.push("Keep the professional summary between 25 and 45 words.");
  if (countSentences(r.summary) < 1 || countSentences(r.summary) > 4)
    errors.push("Keep the summary to 1–4 sentences.");
  for (const key of ["experience", "skills", "education"])
    if (!r[key]?.trim()) errors.push(`Complete the ${key} section.`);
  if (r.education?.trim()) errors.push(...educationErrors(r.education));
  if (
    (r.experience || "")
      .split(/\r?\n/)
      .some((line) =>
        /^\s*(?:(?:additional|earlier|previous|other|professional|work|relevant)\s+)?(?:experience|jobs|roles|companies|employment history|work history)\s*:?\s*$/i.test(
          line,
        ),
      )
  )
    errors.push(
      "Keep all jobs, roles, and companies in the single Experience section. Remove extra section headings.",
    );
  const jobs = experienceList(r.experience || "");
  for (const [index, entry] of jobs.entries()) {
    const bullets = experienceBullets(entry);
    const limit = experienceBulletLimit(jobs, index);
    if (bullets.length > limit)
      errors.push(
        `Use at most ${limit} bullets for ${index === 0 ? "the newest" : index === jobs.length - 1 ? "the oldest" : "each other"} job, using only real experience.`,
      );
  }
  const projects = projectList(r.projects);
  if (codingRequired(r) && requiresProjects(level)) {
    if (projects.length < 3)
      errors.push(
        "Consider at least 3 projects for a junior or mid-level resume; more are welcome.",
      );
  }

  return [...new Set(errors)];
}
export function plainLines(r) {
  return Object.entries(r)
    .flatMap(([k, v]) =>
      k === "contact"
        ? Object.values(v)
        : typeof v === "string"
          ? v.split("\n")
          : [],
    )
    .filter(Boolean);
}

export function accessDurationLabel(access) {
  if (access.registered) {
    if (access.plan === "trial") return "Free trial · One review and build";
    if (access.plan === "subscribed") return "Approved account";
    return "";
  }
  if (access.accessKind === "permanent-coach") return "Permanent";
  const expiresAt = Number(access.trial?.expiresAt);
  if (!(expiresAt > 0) || !Number.isFinite(expiresAt)) return "";
  const date = new Date(expiresAt);
  return Number.isNaN(date.getTime()) ? "" : `Ends ${date.toLocaleString()}`;
}
