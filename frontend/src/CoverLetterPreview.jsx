import { demoMode } from "./demo-mode";
import React, { useEffect, useState } from "react";
import { post } from "./api";
import { saveResumeDownload } from "./download";

export default function CoverLetterPreview({
  text,
  contact,
  onEdit,
  label = "Cover letter",
  endpoint = "/cover-letter/document",
  filename = "cover-letter.pdf",
}) {
  const [document, setDocument] = useState(null),
    [page, setPage] = useState(0),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  useEffect(() => {
    let active = true,
      url;
    setDocument(null);
    setPage(0);
    setError("");
    if (!text.trim() || demoMode) return;
    setBusy(true);
    const timer = setTimeout(async () => {
      try {
        const result = await post(endpoint, { text, contact });
        if (!active) return;
        const blob = new Blob(
          [Uint8Array.from(atob(result.pdf), (c) => c.charCodeAt(0))],
          { type: "application/pdf" },
        );
        url = URL.createObjectURL(blob);
        setDocument({ pages: result.pages, url, blob });
      } catch (e) {
        if (active) setError(e.message);
      } finally {
        if (active) setBusy(false);
      }
    }, 350);
    return () => {
      active = false;
      clearTimeout(timer);
      if (url) URL.revokeObjectURL(url);
    };
  }, [text, contact, endpoint]);
  return (
    <div className="cover-letter-document">
      {demoMode && text.trim() && (
        <div className="resume-paper" style={{ whiteSpace: "pre-wrap" }}>
          {text}
        </div>
      )}
      {!text.trim() ? (
        <p>
          {label === "Cover letter"
            ? "No cover letter yet. Choose Update Cover Letter to create one from your resume and job description."
            : "Your job prep PDF will appear when the prep sheet is ready."}
        </p>
      ) : (
        <>
          {busy && (
            <p role="status">Preparing your {label.toLowerCase()} PDF…</p>
          )}
          {error && <p role="alert">{error}</p>}
          {document && (
            <>
              <img
                className="pdf-preview"
                src={document.pages[page]}
                alt={`${label} PDF, page ${page + 1}`}
              />
              {document.pages.length > 1 && (
                <div className="document-page-controls">
                  <button
                    disabled={page === 0}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous page
                  </button>
                  <span>
                    Page {page + 1} of {document.pages.length}
                  </span>
                  <button
                    disabled={page === document.pages.length - 1}
                    onClick={() => setPage(page + 1)}
                  >
                    Next page
                  </button>
                </div>
              )}
              <a href={document.url} target="_blank" rel="noreferrer">
                View {label.toLowerCase()} full size
              </a>
              <button
                className="secondary button"
                onClick={() => saveResumeDownload(document.blob, filename)}
              >
                Download {label.toLowerCase()} PDF
              </button>
            </>
          )}
        </>
      )}
      {onEdit && (
        <button className="text-button" onClick={onEdit}>
          Update Cover Letter
        </button>
      )}
    </div>
  );
}
