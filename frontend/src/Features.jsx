import React, { useRef } from "react";
import { LayoutGrid, X } from "lucide-react";
import "./features.css";

const features = [
  [
    "Resume upload and preview",
    "Upload your resume and review its pages before starting. PDF previews preserve the original layout; Word files can use a text preview.",
  ],
  [
    "AI resume reviews",
    "Get separate AI reviews of your resume’s presentation and experience, with ratings, strengths, and suggestions for improvement.",
  ],
  [
    "AI feedback",
    "See what you did well and what could be improved, with practical AI feedback and an email feedback PDF you can keep.",
  ],
  [
    "Profession-specific guidance",
    "Build for Technical, General, Educator, or Medical roles, with relevant questions, examples, skills, and sections.",
  ],
  [
    "Medical specialties",
    "Choose your medical role and add relevant licenses, certifications, clinical rotations, training, or physician CV sections.",
  ],
  [
    "Guided resume building",
    "Follow step-by-step guidance, choose what to highlight, and answer profession-specific questions to build your resume.",
  ],
  [
    "AI-built resumes",
    "Have AI draft a professional summary and clearer resume content from your supplied experience and answers. Review and edit the result before choosing your finished resume.",
  ],
  [
    "Skills and credentials",
    "Bring relevant skills from your experience into your skills section, and include supplied credential dates and expirations.",
  ],
  [
    "Experience and project choices",
    "Choose what to highlight, provide missing details, and edit individual resume sections as you build.",
  ],
  [
    "Technical project links",
    "For technical resumes, add GitHub and demo links, discover matching public repositories, and review suggested technologies.",
  ],
  [
    "Job preparation",
    "Add a job posting by text, link, or file to guide your resume and generate a job-preparation document.",
  ],
  [
    "Cover letters",
    "Create, review, and revise a cover letter using your resume and supplied job information.",
  ],
  [
    "Formatting choices",
    "Choose from profession-appropriate fonts and layouts, preview the result, and compare resume versions.",
  ],
  [
    "Saved progress",
    "Save your resume and building progress, then return to continue where you left off.",
  ],
  [
    "PDF/Word exports",
    "Download your finished resume as PDF or Word and use the available email delivery options.",
  ],
  [
    "Jokes and resume roasts",
    "Enjoy optional profession-specific jokes, resume roasts, and glow-up comparisons alongside practical feedback.",
  ],
  [
    "Interactive demo and support",
    "Try the sample workflow, read How It Works, and contact support when you need help.",
  ],
];

export default function Features({ individual = false }) {
  const dialog = useRef(null);
  const items = individual
    ? features
    : [
        ...features,
        [
          "Coach and mentor workspace",
          "Work with client resumes, browse Recent reviews, reopen saved progress, and download available review files.",
        ],
      ];
  return (
    <div className="features-control">
      <button
        type="button"
        className="features-button"
        onClick={() => dialog.current?.showModal()}
        aria-haspopup="dialog"
      >
        <LayoutGrid size={17} aria-hidden="true" /> Features
      </button>
      <dialog
        ref={dialog}
        className="features-dialog"
        aria-labelledby="features-title"
        onClick={(event) => {
          if (event.target === dialog.current) dialog.current.close();
        }}
      >
        <div className="features-content">
          <div className="features-heading">
            <h2 id="features-title">Resume Prospective features</h2>
            <button
              type="button"
              className="features-close"
              aria-label="Close features"
              onClick={() => dialog.current.close()}
              autoFocus
            >
              <X size={22} />
            </button>
          </div>
          <p className="features-intro">
            {individual ? "Individual" : "Coach and Mentor"} · From your first
            review to a finished resume.
          </p>
          <div className="features-grid">
            {items.map(([title, description]) => (
              <section key={title}>
                <h3>{title}</h3>
                <p>{description}</p>
              </section>
            ))}
          </div>
        </div>
      </dialog>
    </div>
  );
}
