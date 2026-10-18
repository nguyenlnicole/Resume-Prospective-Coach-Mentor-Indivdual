import React, { useEffect, useState } from "react";
import { request } from "./api";
import "./uploaded-preview.css";

export default function UploadedResumePreview({
  file,
  onBack,
  onReview,
  progressControls,
  sampleResume,
  features,
}) {
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState("");
  const [activePage, setActivePage] = useState(0);
  const [zoomedPage, setZoomedPage] = useState(null);
  useEffect(() => {
    let active = true;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    setPreview(null);
    setError("");
    setActivePage(0);
    setZoomedPage(null);
    if (sampleResume) {
      setPreview({
        previewType: "text",
        textPreview: [
          sampleResume.contact.name,
          sampleResume.summary,
          sampleResume.experience,
          sampleResume.skills,
          sampleResume.projects,
          sampleResume.education,
        ].join("\n\n"),
      });
      return () => {
        active = false;
        document.body.style.overflow = previous;
      };
    }
    const body = new FormData();
    body.append("file", file);
    request("/resume/upload-preview", { method: "POST", body })
      .then((result) => {
        if (active) setPreview(result);
      })
      .catch((e) => {
        if (active) setError(e.message);
      });
    return () => {
      active = false;
      document.body.style.overflow = previous;
    };
  }, [file, sampleResume]);
  const count = preview?.pages?.length || 0;
  const textPreview = preview?.previewType === "text";
  const columns = count === 1 ? 1 : 2;
  return (
    <section className="uploaded-preview" aria-label="Uploaded resume preview">
      <header className="uploaded-preview-heading">
        <div>
          <h1>Your uploaded resume</h1>
          <p className="uploaded-filename" title={file.name}>
            {file.name}
          </p>
        </div>
        {features}
        <strong className="uploaded-page-count" role="status">
          {preview
            ? textPreview
              ? "Word document · pages unverified"
              : `${count} ${count === 1 ? "page" : "pages"}`
            : "Loading preview…"}
        </strong>
      </header>
      {progressControls && (
        <aside className="uploaded-progress-tools" aria-label="Resume progress">
          {progressControls}
        </aside>
      )}
      {error ? (
        <div className="uploaded-preview-status" role="alert">
          {error}
        </div>
      ) : !preview ? (
        <div className="uploaded-preview-status" role="status">
          Preparing all pages for your preview…
        </div>
      ) : textPreview ? (
        <div className="uploaded-word-preview">
          <p>
            This is a text preview of your Word file. Its original page count,
            fonts, and spacing cannot be checked here. Upload a PDF if you want
            the full format review.
          </p>
          <div>{preview.textPreview}</div>
        </div>
      ) : (
        <div
          className="uploaded-page-grid"
          style={{
            "--page-columns": columns,
            "--page-rows": Math.ceil(count / columns),
          }}
        >
          {preview.pages.map((src, index) => (
            <figure
              key={index}
              className={`${count > 1 && count % 2 === 1 && index === count - 1 ? "uploaded-last-page" : ""} ${activePage === index ? "uploaded-mobile-active" : ""}`}
            >
              <button
                type="button"
                className="uploaded-page-zoom"
                onClick={() => setZoomedPage(index)}
                aria-label={`Enlarge page ${index + 1} of ${count}`}
              >
                <img
                  src={src}
                  alt={`Original resume, page ${index + 1} of ${count}`}
                />
              </button>
              <figcaption>Page {index + 1}</figcaption>
            </figure>
          ))}
        </div>
      )}
      {preview && !textPreview && count > 1 && (
        <div className="uploaded-mobile-pages" aria-label="Resume pages">
          <button
            type="button"
            disabled={activePage === 0}
            onClick={() => setActivePage(activePage - 1)}
          >
            Previous
          </button>
          <span>
            Page {activePage + 1} of {count} · tap to enlarge
          </span>
          <button
            type="button"
            disabled={activePage === count - 1}
            onClick={() => setActivePage(activePage + 1)}
          >
            Next
          </button>
        </div>
      )}
      {zoomedPage !== null && preview?.pages?.[zoomedPage] && (
        <div
          className="uploaded-zoom-overlay"
          role="dialog"
          aria-modal="true"
          aria-label={`Enlarged resume page ${zoomedPage + 1}`}
        >
          <button
            type="button"
            className="uploaded-zoom-close"
            onClick={() => setZoomedPage(null)}
          >
            Close enlarged page
          </button>
          <div>
            <img
              src={preview.pages[zoomedPage]}
              alt={`Enlarged resume page ${zoomedPage + 1} of ${count}`}
            />
          </div>
        </div>
      )}
      <footer className="uploaded-preview-actions">
        <p>
          {textPreview
            ? "Check the extracted text before starting your ratings."
            : "Check every page before starting your ratings."}
        </p>
        <div className="uploaded-review-actions">
          <button type="button" className="secondary button" onClick={onBack}>
            Choose Another File
          </button>
          <button
            type="button"
            className="primary button"
            disabled={!preview}
            onClick={onReview}
          >
            Rate My Resume
          </button>
        </div>
      </footer>
    </section>
  );
}
