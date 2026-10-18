import React, { useEffect, useState } from "react";
import { createDemoResumePdf } from "./demo-resume-pdf.js";

export default function DemoResumePdfPreview({ resume }) {
  const [url, setUrl] = useState("");
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    let active = true,
      objectUrl;
    setUrl("");
    setFailed(false);
    createDemoResumePdf(resume)
      .then((blob) => {
        if (!active) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [resume]);
  if (failed)
    return (
      <p role="alert">
        The resume preview could not load. Please refresh to try again.
      </p>
    );
  if (!url) return <p role="status">Preparing the resume preview…</p>;
  return (
    <object
      data={`${url}#toolbar=0&view=FitH`}
      type="application/pdf"
      aria-label="Alex Rivera resume PDF"
      style={{ width: "100%", height: "min(1100px, 85vh)", border: 0 }}
    >
      <a href={url} target="_blank" rel="noreferrer">
        View the resume PDF
      </a>
    </object>
  );
}
