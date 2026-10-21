import React, { useState, useRef, useEffect } from "react";
import { Folder, X } from "lucide-react";
import { request, post } from "./api";
export default function CoachSavedReviews({ onOpen, onResumeProgress }) {
  const [open, setOpen] = useState(false),
    [items, setItems] = useState([]),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const dialog = useRef(null);
  useEffect(() => {
    if (open) dialog.current?.showModal();
    else dialog.current?.close();
  }, [open]);
  async function show() {
    setOpen(true);
    setBusy(true);
    setError("");
    try {
      const [reviews,draft]=await Promise.all([request("/coach/saved"),request("/workflow-draft")]);setItems([...(draft.available?[{id:"workflow-draft",firstName:"Saved progress",savedAt:draft.updatedAt}]:[]),...reviews]);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function remove(id) {
    setBusy(true);
    try {
      await request(id === "workflow-draft" ? "/workflow-draft" : `/coach/saved/${id}`, { method: "DELETE" });
      setItems(items.filter((x) => x.id !== id));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function resume(id) {
    setBusy(true);
    try {
      if (id === "workflow-draft") { await onResumeProgress(); setOpen(false); return; }
      const data = await post(`/coach/saved/${id}/open`, {});
      onOpen(data);
      setOpen(false);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <button className="coach-recent-toggle" onClick={show}>
        <Folder size={20} /> Saved
      </button>
      <dialog
        ref={dialog}
        className="saved-review-dialog"
        onCancel={() => setOpen(false)}
      >
        <button
          className="saved-close"
          aria-label="Close saved reviews"
          onClick={() => setOpen(false)}
        >
          <X />
        </button>
        <h2>Saved for later</h2>
        <p>Choose a review to return to “Ready to build?”</p>
        {busy && <p role="status">Loading…</p>}
        {error && <p role="alert">{error}</p>}
        {!busy && !items.length && <p>No saved reviews yet.</p>}
        {items.map((item) => (
          <div className="saved-review-row" key={item.id}>
            <button disabled={busy} onClick={() => resume(item.id)}>
              <Folder size={22} />
              <span>
                {item.firstName}
                <small>{new Date(item.savedAt).toLocaleDateString()}</small>
              </span>
            </button>
            <button
              disabled={busy}
              aria-label={`Delete saved review for ${item.firstName}`}
              onClick={() => remove(item.id)}
            >
              <X size={18} />
            </button>
          </div>
        ))}
      </dialog>
    </>
  );
}
