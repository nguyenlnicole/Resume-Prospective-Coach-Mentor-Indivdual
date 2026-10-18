import React, { useEffect, useRef, useState } from "react";
import { Folder, FolderOpen, FileText, X } from "lucide-react";
import { request } from "./api";
import { saveResumeDownload } from "./download";

export default function CoachRecentReviews({ onResume }) {
  const [open, setOpen] = useState(false);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [downloading, setDownloading] = useState("");
  const [selected, setSelected] = useState({});
  const dialogRef = useRef(null);

  useEffect(() => {
    if (open && !dialogRef.current?.open) dialogRef.current?.showModal();
    if (!open && dialogRef.current?.open) dialogRef.current.close();
  }, [open]);

  useEffect(() => {
    if (!open) return;
    let active = true;
    setLoading(true);
    setError("");
    request("/coach/history")
      .then((items) => {
        if (active) setReviews(items);
      })
      .catch((e) => {
        if (active) setError(e.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [open]);

  async function download(review) {
    const kinds = files
      .map(([kind]) => kind)
      .filter((kind) => selected[review.id]?.includes(kind));
    if (!kinds.length) return;
    setDownloading(review.id);
    setError("");
    try {
      const response = await fetch(
        `/api/coach/history/${encodeURIComponent(review.id)}/bundle?${kinds.map((kind) => `kinds=${encodeURIComponent(kind)}`).join("&")}`,
        {
          credentials: "same-origin",
          cache: "no-store",
        },
      );
      if (!response.ok)
        throw new Error(
          "The selected files could not be downloaded. Please sign in again if your session ended.",
        );
      await saveResumeDownload(
        await response.blob(),
        `${review.firstName}-review-files.zip`,
      );
    } catch (e) {
      setError(e.message);
    } finally {
      setDownloading("");
    }
  }

  const files = [
    ["original", "Uploaded resume"],
    ["feedback", "Feedback PDF"],
    ["pdf", "Finished resume PDF"],
    ["docx", "Finished resume Word"],
  ];

  return (
    <section className="coach-recent-reviews" aria-label="Recent reviews">
      <button
        type="button"
        className="coach-recent-toggle"
        onClick={() => setOpen(true)}
      >
        <Folder size={18} /> Recent reviews
      </button>
      <dialog
        ref={dialogRef}
        className="coach-recent-dialog"
        aria-label="Recent reviews"
        onClose={() => setOpen(false)}
        onClick={(event) => {
          if (event.target === dialogRef.current) dialogRef.current.close();
        }}
      >
        <div className="coach-recent-heading">
          <h2>Recent reviews</h2>
          <button
            type="button"
            aria-label="Close recent reviews"
            onClick={() => dialogRef.current?.close()}
          >
            <X size={20} />
          </button>
        </div>
        <div className="coach-recent-panel">
          {loading && <p role="status">Loading reviews…</p>}
          {!loading && !error && reviews.length === 0 && (
            <p>No reviews yet. Your last five clients will appear here.</p>
          )}
          {reviews.map((review) => (
            <details className="coach-review-folder" key={review.id}>
              <summary>
                <FolderOpen size={18} /> {review.firstName} ·{" "}
                {new Date(review.reviewedAt).toLocaleDateString(undefined, {
                  month: "short",
                  day: "numeric",
                  year: "numeric",
                })}
              </summary>
              <div className="coach-review-files">
                {review.hasProgress && (
                  <button
                    type="button"
                    className="secondary button"
                    onClick={() => {
                      setOpen(false);
                      onResume(review.id);
                    }}
                  >
                    Continue where I left off
                  </button>
                )}
                {!review.hasProgress && (
                  <p>This older review has downloadable files only.</p>
                )}
                {files
                  .filter(
                    ([kind]) =>
                      review.hasFinished || !["pdf", "docx"].includes(kind),
                  )
                  .map(([kind, label]) => (
                    <label className="coach-review-file" key={kind}>
                      <input
                        type="checkbox"
                        checked={Boolean(selected[review.id]?.includes(kind))}
                        onChange={(event) =>
                          setSelected((current) => {
                            const prior = current[review.id] || [];
                            return {
                              ...current,
                              [review.id]: event.target.checked
                                ? [...prior, kind]
                                : prior.filter((item) => item !== kind),
                            };
                          })
                        }
                      />
                      <FileText size={24} aria-hidden="true" />
                      <span>
                        {review.firstName} — {label}
                      </span>
                    </label>
                  ))}
                <button
                  type="button"
                  className="coach-review-download"
                  disabled={
                    Boolean(downloading) || !selected[review.id]?.length
                  }
                  onClick={() => download(review)}
                >
                  <img src="/coach-download-folder.png" alt="" />
                  {downloading === review.id
                    ? "Preparing files…"
                    : "Download checked files"}
                </button>
              </div>
            </details>
          ))}
          {error && (
            <p className="coach-recent-error" role="alert">
              {error}
            </p>
          )}
        </div>
      </dialog>
    </section>
  );
}
