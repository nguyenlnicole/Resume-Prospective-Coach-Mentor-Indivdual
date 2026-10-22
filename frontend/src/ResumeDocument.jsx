import React from "react";
import { resumeDashes } from "./resume-dashes.js";
import { credentialParts } from "./credential-format.js";
import { resumeSections, defaultResumeFont } from "./model";
import { normalizeResume, safeLink } from "./rules";

export function linkedResumeText(line) {
  return line
    .split(/(\[[^\]\n]+\]\(https?:\/\/[^\s)]+\)|https?:\/\/\S+)/g)
    .map((part, n) => {
      const marked = part.match(/^\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)$/);
      const url = marked?.[2] || (/^https?:/.test(part) ? part : "");
      if (!url) return resumeDashes(part);
      return (
        <a
          key={n}
          href={safeLink(url) || undefined}
          target="_blank"
          rel="noreferrer"
        >
          {marked?.[1] ||
            (/github|gitlab|bitbucket/.test(url) ? "GitHub Repo" : "Demo")}
        </a>
      );
    });
}

function headingAndDate(line, renderTitle, dates) {
  const match = line.match(/^(.*?)\s+\|\s+([^|]*\b(?:19|20)\d{2}[^|]*)$/);
  return match ? (
    <span
      className={`resume-entry-heading ${dates === "right" ? "right-dates" : "inline-dates"}`}
    >
      <span>{renderTitle(match[1])}</span>
      <span className="resume-date">
        {dates === "right" ? "" : " | "}
        {match[2]}
      </span>
    </span>
  ) : (
    renderTitle(line)
  );
}

export function resumeLine(line, section, dates = "right", heading = false) {
  line = resumeDashes(line);
  if (section === "projects" && !heading && /^Organization:\s*/i.test(line))
    return <em>{line.replace(/^Organization:\s*/i, "")}</em>;
  if (section === "projects" && !heading)
    return headingAndDate(line, (title) => linkedResumeText(title), dates);
  if (section === "skills" && line.includes(":")) {
    const colon = line.indexOf(":") + 1;
    return (
      <>
        <strong>{line.slice(0, colon)}</strong>
        {line.slice(colon)}
      </>
    );
  }
  if (section === "education")
    return headingAndDate(
      line,
      (title) => {
        const parts = title.split(/\s+[—–-]\s+/, 2);
        return (
          <>
            {parts[0]}
            {parts.length > 1 && (
              <>
                {" "}
                – <em>{parts[1]}</em>
              </>
            )}
          </>
        );
      },
      dates,
    );
  if (section === "experience" && !/^[•*\-]\s/.test(line))
    return headingAndDate(
      line,
      (title) => {
        const parts = title.split(/\s+[—–-]\s+/, 2);
        return parts.length > 1 ? (
          <>
            <strong>{parts[0]}</strong> – <em>{parts[1]}</em>
          </>
        ) : /\s\|\s/.test(line) ? (
          <em>{title}</em>
        ) : (
          <strong>{title}</strong>
        );
      },
      dates,
    );
  if (section === "certifications") {
    const { name, issuer, date } = credentialParts(line);
    return (
      <>
        {linkedResumeText(name)}
        {issuer && (
          <>
            , <em>{linkedResumeText(issuer)}</em>
          </>
        )}
        {date && (
          <>
            {" "}
            | <strong>{date}</strong>
          </>
        )}
      </>
    );
  }
  if (section === "projects" && heading) {
    const text = line.replace(/^[•*\-]\s+/, "");
    const stop = text.search(/\s*\|\s*|\s+[—–]\s+/);
    const end = stop < 0 ? text.length : stop;
    return (
      <>
        <strong>{linkedResumeText(text.slice(0, end))}</strong>
        {linkedResumeText(text.slice(end))}
      </>
    );
  }
  return linkedResumeText(line);
}

export default function ResumeDocument({
  resume,
  level = "Senior",
  mini = false,
  font,
  linkColor = "blue",
  dates = "right",
}) {
  const r = normalizeResume(resume);
  const selectedFont = font || defaultResumeFont(level);
  const fontFamily = ["Calibri", "Arial", "Aptos"].includes(selectedFont) ? `"${selectedFont}", Arial, sans-serif` : `"${selectedFont}", serif`;
  const nontechnical = [
    "General",
    "Normal",
    "Medical Field",
    "Educator",
  ].includes(level);
  return (
    <article
      className={`resume-paper ${mini ? "mini" : ""}`}
      aria-label="Resume preview"
      data-profession={nontechnical ? "nontechnical" : "technical"}
      style={{
        "--resume-font": fontFamily,
        "--resume-link-color": linkColor === "dark" ? "#23476A" : "#0000EE",
      }}
    >
      <header>
        <h2>{r.contact.name || "Your name"}</h2>
        <div className="resume-contact">
          {[r.contact.location, r.contact.phone].filter(Boolean).map((t) => (
            <span key={t}>{t}</span>
          ))}
          {r.contact.email && (
            <a href={`mailto:${r.contact.email}`}>{r.contact.email}</a>
          )}
          {["linkedin", "github", "portfolio"]
            .filter((k) => r.contact[k])
            .map((k) => (
              <a
                key={k}
                href={safeLink(r.contact[k]) || undefined}
                target="_blank"
                rel="noreferrer"
              >
                {k === "linkedin"
                  ? "LinkedIn"
                  : k === "github"
                    ? "GitHub"
                    : "Portfolio"}
              </a>
            ))}
        </div>
      </header>
      <p className="resume-summary">{resumeDashes(r.summary)}</p>
      {resumeSections(level, r)
        .slice(2)
        .filter(([k]) => r[k]?.trim())
        .map(([key, label]) => (
          <section key={key}>
            <h3>{label.toUpperCase()}</h3>
            {key === "projects"
              ? r[key]
                  .split(/\n\s*\n/)
                  .filter(Boolean)
                  .map((project, n) => {
                    const [title, ...body] = project.split(/\r?\n/);
                    return (
                      <div className="resume-project" key={n}>
                        <p className="resume-project-heading">
                          <span aria-hidden="true">• </span>
                          {resumeLine(title, key, dates, true)}
                        </p>
                        {body.map((line, i) => (
                          <p className="resume-project-description" key={i}>
                            {resumeLine(
                              line.replace(/^[•*\-]\s+/, ""),
                              key,
                              dates,
                            )}
                          </p>
                        ))}
                      </div>
                    );
                  })
              : r[key]
                  .split("\n")
                  .filter((line) => key !== "education" || line.trim())
                  .map((line, i) => (
                    <p
                      key={i}
                      className={
                        !line
                          ? "resume-gap"
                          : /^[•*\-]\s/.test(line)
                            ? "resume-bullet"
                            : ""
                      }
                    >
                      {resumeLine(line, key, dates)}
                    </p>
                  ))}
          </section>
        ))}
    </article>
  );
}
