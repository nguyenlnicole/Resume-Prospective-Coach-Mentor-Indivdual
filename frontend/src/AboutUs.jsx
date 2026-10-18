import React from "react";
export function AboutButton({ onClick }) {
  return (
    <button
      type="button"
      className="about-control"
      onClick={onClick}
      aria-label="About Us"
    >
      <span>About Us</span>
      <img src="/pnb-robot-transparent.png" alt="" />
    </button>
  );
}
export function HowButton({ onClick, className = "" }) {
  return (
    <button
      type="button"
      className={`about-control how-control ${className}`}
      onClick={onClick}
      aria-label="How It Works"
    >
      <span>How It Works</span>
      <img className="how-paw" src="/pnb-paws-favicon.png" alt="" />
    </button>
  );
}
export function AboutDialog({ onClose }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <section
        className="modal about-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="about-title"
        onClick={(e) => e.stopPropagation()}
        onKeyDown={(e) => {
          if (e.key === "Escape") onClose();
        }}
      >
        <button
          autoFocus
          className="text-button"
          onClick={onClose}
          aria-label="Close About Us"
        >
          Close ×
        </button>
        <h2 id="about-title">About Resume Prospective</h2>
        <p>
          Your resume should highlight your strengths and still sound like you.
        </p>
        <p>
          Resume Prospective helps you make sense of feedback, highlight your
          experience, and turn a rough draft into a clearer, more professional
          resume. You can work on your own or alongside a coach or mentor,
          review every suggestion, and edit the result before sharing it.
        </p>
        <p>
          We use OpenAI to assist with reviews and writing. Your experience
          stays yours: suggestions should reflect what you have actually done,
          without made-up skills or achievements.
        </p>
        <p>
          Created by PNB Paws LLC, with care for the person behind the resume.
        </p>
      </section>
    </div>
  );
}
