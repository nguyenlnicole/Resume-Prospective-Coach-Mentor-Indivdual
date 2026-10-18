import React, { useState } from "react";
import { credentialParts } from "./credential-format.js";

export default function CertificationFields({ value, onChange }) {
  const [rows, setRows] = useState(() => {
    const lines = value.split(/\r?\n/).filter((line) => line.trim());
    return (lines.length ? lines : [""]).map((line) => {
      const { name, issuer, date } = credentialParts(line);
      const [issued = "", expires = ""] = date.split(/\s*[–—-]?\s*Expires\s+/i);
      return { title: name + (issuer ? `, ${issuer}` : ""), issued, expires };
    });
  });
  const save = (next) => {
    setRows(next);
    onChange(
      next
        .map((entry) => {
          const dates = [
            entry.issued.trim(),
            entry.expires.trim() ? `Expires ${entry.expires.trim()}` : "",
          ]
            .filter(Boolean)
            .join(" – ");
          return entry.title + (dates ? ` | ${dates}` : "");
        })
        .join("\n"),
    );
  };
  const update = (index, changes) => {
    save(
      rows.map((entry, position) =>
        index === position ? { ...entry, ...changes } : entry,
      ),
    );
  };
  return (
    <div className="education-editor">
      <p>
        Does any certification expire? Add its expiration date if it does. Leave
        it blank if it does not expire or you do not know.
      </p>
      {rows.map(({ title, issued, expires }, index) => {
        return (
          <div className="education-entry" key={index}>
            <div className="field">
              <label htmlFor={`cert-title-${index}`}>
                Certification and issuer
              </label>
              <input
                id={`cert-title-${index}`}
                value={title}
                onChange={(e) => update(index, { title: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor={`cert-issued-${index}`}>
                Date earned (optional)
              </label>
              <input
                id={`cert-issued-${index}`}
                placeholder="2019"
                value={issued}
                onChange={(e) => update(index, { issued: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor={`cert-expires-${index}`}>
                Expiration date (optional)
              </label>
              <input
                id={`cert-expires-${index}`}
                placeholder="2029"
                value={expires}
                onChange={(e) => update(index, { expires: e.target.value })}
              />
            </div>
            <button
              type="button"
              className="text-button"
              onClick={() => save(rows.filter((_, i) => i !== index))}
            >
              Remove certification
            </button>
          </div>
        );
      })}
      <button
        type="button"
        className="text-button"
        onClick={() => save([...rows, { title: "", issued: "", expires: "" }])}
      >
        Add certification
      </button>
    </div>
  );
}
