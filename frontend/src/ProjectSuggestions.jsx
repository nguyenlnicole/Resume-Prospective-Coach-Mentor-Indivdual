import React, { useEffect, useState } from "react";
import { githubUsername } from "./rules";
import { suggestProjects } from "./project-suggestions";

export default function ProjectSuggestions({
  profile,
  projects,
  target,
  disabled,
}) {
  const [suggestions, setSuggestions] = useState([]);
  const username = githubUsername(profile),
    projectKey = JSON.stringify(projects);
  useEffect(() => {
    setSuggestions([]);
    if (disabled || !username || !target.trim()) return;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);
    const timer = setTimeout(async () => {
      try {
        const response = await fetch(
          `https://api.github.com/users/${encodeURIComponent(username)}/repos?per_page=100&sort=updated`,
          {
            signal: controller.signal,
            headers: { Accept: "application/vnd.github+json" },
          },
        );
        if (!response.ok) return;
        const repos = await response.json();
        if (!controller.signal.aborted && Array.isArray(repos))
          setSuggestions(
            suggestProjects(JSON.parse(projectKey), repos, target),
          );
      } catch {}
    }, 800);
    return () => {
      controller.abort();
      clearTimeout(timer);
      clearTimeout(timeout);
    };
  }, [username, projectKey, target, disabled]);
  if (!suggestions.length) return null;
  return (
    <section
      className="card profile-help"
      aria-label="Other projects to consider"
    >
      <h2>Other projects worth considering</h2>
      <p>
        These public repo descriptions match more of the job’s technologies than
        a listed project. Check your own contribution and the project’s quality
        before replacing anything.
      </p>
      {suggestions.map((repo) => (
        <article key={repo.url}>
          <h3>
            <a href={repo.url} target="_blank" rel="noreferrer">
              {repo.name}
            </a>
          </h3>
          <p>{repo.description}</p>
          <p>
            Consider instead of{" "}
            <strong>
              {
                projects[repo.replaceIndex]
                  .split("\n")[0]
                  .split("|")[0]
                  .split(/[—–]/)[0]
              }
            </strong>
            : matches <strong>{repo.matched.join(", ")}</strong>.
          </p>
          <p>
            You can replace the project in the Projects editor using only work
            you did. Nothing has been replaced automatically.
          </p>
        </article>
      ))}
    </section>
  );
}
