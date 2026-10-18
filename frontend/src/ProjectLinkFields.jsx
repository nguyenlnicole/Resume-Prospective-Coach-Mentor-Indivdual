import React, { useEffect, useRef, useState } from "react";
import {
  projectLinkFieldValues,
  combineProjectLinkFields,
} from "./project-link-fields";
export default function ProjectLinkFields({ id, value = "", onChange }) {
  const [fields, setFields] = useState(() => projectLinkFieldValues(value));
  const emitted = useRef(value);
  useEffect(() => {
    if (value !== emitted.current) {
      emitted.current = value;
      setFields(projectLinkFieldValues(value));
    }
  }, [value]);
  function update(key, text) {
    const next = { ...fields, [key]: text };
    setFields(next);
    emitted.current = combineProjectLinkFields(next);
    onChange(emitted.current);
  }
  return (
    <div className="project-link-fields">
      <div className="field">
        <label htmlFor={`${id}-repo`}>
          GitHub Repo / Project Code URL (optional)
        </label>
        <input
          id={`${id}-repo`}
          type="text"
          inputMode="url"
          value={fields.repo}
          onChange={(e) => update("repo", e.target.value)}
          placeholder="https://github.com/you/project"
        />
      </div>
      <div className="field">
        <label htmlFor={`${id}-demo`}>Project Demo URL (optional)</label>
        <input
          id={`${id}-demo`}
          type="text"
          inputMode="url"
          value={fields.demo}
          onChange={(e) => update("demo", e.target.value)}
          placeholder="https://your-project.example"
        />
      </div>
      <small>
        Both are optional. You can leave them blank. Separate multiple links
        with |.
      </small>
    </div>
  );
}
