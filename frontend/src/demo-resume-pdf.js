import { PDFDocument, StandardFonts, rgb, PDFName, PDFString } from "pdf-lib";
import { normalizeResume, formatProjectLinks } from "./rules.js";
import { resumeDashes } from "./resume-dashes.js";

export async function createDemoResumePdf(source) {
  const cleaned = normalizeResume(source);
  const resume = { ...cleaned, projects: formatProjectLinks(cleaned.projects) };
  const doc = await PDFDocument.create();
  const regular = await doc.embedFont(StandardFonts.Helvetica),
    bold = await doc.embedFont(StandardFonts.HelveticaBold),
    italic = await doc.embedFont(StandardFonts.HelveticaOblique);
  let page = doc.addPage([612, 792]),
    y = 756;
  const left = 43.2,
    width = 525.6,
    size = 10,
    leading = 12;
  const clean = (value) =>
    resumeDashes(value)
      .replace(/[’‘]/g, "'")
      .replace(/[^\x20-\x7E\n•–]/g, "");
  function link(url, x, baseline, w, h) {
    const annotation = doc.context.obj({
      Type: "Annot",
      Subtype: "Link",
      Rect: [x, baseline - 2, x + w, baseline + h],
      Border: [0, 0, 0],
      A: { Type: "Action", S: "URI", URI: PDFString.of(url) },
    });
    let annotations = page.node.Annots();
    if (!annotations) {
      annotations = doc.context.obj([]);
      page.node.set(PDFName.of("Annots"), annotations);
    }
    annotations.push(doc.context.register(annotation));
  }
  function room() {
    if (y < 40) {
      page = doc.addPage([612, 792]);
      y = 756;
    }
  }
  function text(value, x, baseline, font = regular, fontSize = size, url = "") {
    const t = clean(value),
      w = font.widthOfTextAtSize(t, fontSize);
    page.drawText(t, {
      x,
      y: baseline,
      size: fontSize,
      font,
      color: url ? rgb(0, 0, 0.93) : rgb(0, 0, 0),
    });
    if (url) {
      page.drawLine({
        start: { x, y: baseline - 1.5 },
        end: { x: x + w, y: baseline - 1.5 },
        thickness: 0.35,
        color: rgb(0, 0, 0.93),
      });
      link(url, x, baseline, w, fontSize);
    }
    return w;
  }
  function rich(fragments, { indent = 0, bullet = false, date = "" } = {}) {
    room();
    y -= leading;
    let x = left + indent;
    if (bullet) text("•", left + 9, y);
    const dateWidth = regular.widthOfTextAtSize(clean(date), size);
    const limit = left + width - (date ? dateWidth + 12 : 0);
    for (const fragment of fragments)
      for (const word of clean(fragment.text).split(/\s+/).filter(Boolean)) {
        const f = fragment.font || regular,
          w = f.widthOfTextAtSize(word, size);
        if (x + w > limit && x > left + indent) {
          room();
          y -= leading;
          x = left + indent;
        }
        text(word, x, y, f, size, fragment.url);
        x += w + f.widthOfTextAtSize(" ", size);
      }
    if (date) text(date, left + width - dateWidth, y);
  }
  function linked(value) {
    const result = [];
    let offset = 0;
    for (const match of value.matchAll(/\[([^\]]+)\]\((https?:\/\/[^)]+)\)/g)) {
      if (match.index > offset)
        result.push({ text: value.slice(offset, match.index) });
      result.push({ text: match[1], url: match[2] });
      offset = match.index + match[0].length;
    }
    if (offset < value.length) result.push({ text: value.slice(offset) });
    return result;
  }
  const name = clean(resume.contact.name),
    nameWidth = bold.widthOfTextAtSize(name, 16);
  y -= 16;
  text(name, left + (width - nameWidth) / 2, y, bold, 16);
  y -= 4;
  const contact = [
    { text: resume.contact.location },
    { text: resume.contact.phone },
    { text: resume.contact.email, url: `mailto:${resume.contact.email}` },
    ...["linkedin", "github", "portfolio"]
      .filter((k) => resume.contact[k])
      .map((k) => ({
        text:
          k === "linkedin"
            ? "LinkedIn"
            : k === "github"
              ? "GitHub"
              : "Portfolio",
        url: resume.contact[k],
      })),
  ].filter((f) => f.text);
  const contactSize = 9;
  const total =
    contact.reduce(
      (n, f) => n + regular.widthOfTextAtSize(clean(f.text), contactSize),
      0,
    ) +
    (contact.length - 1) * regular.widthOfTextAtSize(" | ", contactSize);
  y -= 12;
  let x = left + Math.max(0, (width - total) / 2);
  contact.forEach((f, i) => {
    if (i) x += text(" | ", x, y, regular, contactSize);
    x += text(f.text, x, y, regular, contactSize, f.url);
  });
  y -= 6;
  rich([{ text: resume.summary }]);
  const sections = [
    ["experience", "EXPERIENCE"],
    ["skills", "TECHNICAL SKILLS"],
    ["projects", "TECHNICAL PROJECTS"],
    ["education", "EDUCATION"],
    ["certifications", "CERTIFICATIONS"],
  ];
  for (const [key, label] of sections) {
    if (!resume[key]?.trim()) continue;
    y -= 6;
    rich([{ text: label, font: bold }]);
    page.drawLine({
      start: { x: left, y: y - 1.5 },
      end: { x: left + bold.widthOfTextAtSize(label, size), y: y - 1.5 },
      thickness: 0.5,
    });
    y -= 3;
    let projectHeading = true;
    for (const raw of resume[key].split("\n")) {
      if (!raw.trim()) {
        if (key !== "education") y -= 3;
        projectHeading = true;
        continue;
      }
      const line = raw.replace(/^[•*\-]\s+/, ""),
        bullet = /^[•*\-]\s+/.test(raw);
      const dated = line.match(/^(.*?)\s+\|\s+([^|]*\b(?:19|20)\d{2}[^|]*)$/),
        title = dated ? dated[1] : line,
        date = dated?.[2] || "";
      if (key === "projects") {
        if (projectHeading) {
          const stop = line.search(/\s*\|\s*|\s+[—–]\s+/),
            end = stop < 0 ? line.length : stop;
          rich(
            [
              { text: line.slice(0, end), font: bold },
              ...linked(line.slice(end)),
            ],
            { indent: 18, bullet: true },
          );
          projectHeading = false;
        } else rich(linked(line), { indent: 18 });
      } else if (key === "experience" && !bullet) {
        const parts = title.split(/\s+[—–-]\s+/, 2);
        rich(
          parts.length > 1
            ? [
                { text: parts[0], font: bold },
                { text: " - " },
                { text: parts[1], font: italic },
              ]
            : [{ text: title, font: date ? italic : bold }],
          { date },
        );
      } else if (key === "education") {
        const parts = title.split(/\s+[—–-]\s+/, 2);
        rich(
          [
            { text: parts[0] },
            ...(parts.length > 1
              ? [{ text: " - " }, { text: parts[1], font: italic }]
              : []),
          ],
          { date },
        );
      } else if (key === "skills" && line.includes(":")) {
        const colon = line.indexOf(":") + 1;
        rich([
          { text: line.slice(0, colon), font: bold },
          { text: line.slice(colon) },
        ]);
      } else if (key === "certifications")
        rich([{ text: title, font: bold }], { date });
      else rich(linked(line), { indent: bullet ? 18 : 0, bullet });
    }
  }
  return new Blob([await doc.save()], { type: "application/pdf" });
}
