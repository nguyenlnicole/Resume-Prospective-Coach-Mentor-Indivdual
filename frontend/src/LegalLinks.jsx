import React from "react";

export default function LegalLinks({ className = "", onHowItWorks }) {
  return (
    <nav
      className={`legal-links ${className}`.trim()}
      aria-label="Footer links"
    >
      <a
        href="/?how=1"
        onClick={
          onHowItWorks
            ? (event) => {
                event.preventDefault();
                onHowItWorks();
              }
            : undefined
        }
      >
        How It Works
      </a>
      <span aria-hidden="true">·</span>
      <a href="/faq.html">FAQ</a>
      <span aria-hidden="true">·</span>
      <a href="/privacy.html">Privacy</a>
      <span aria-hidden="true">·</span>
      <a href="/terms.html">Terms</a>
      <span aria-hidden="true">·</span>
      <a href="/cookies.html">Cookies</a>
    </nav>
  );
}
