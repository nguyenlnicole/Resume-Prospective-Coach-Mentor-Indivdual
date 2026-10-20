import { demoJobDescription, demoCoverLetter } from "./demo-job.js";
export const demoMode =
  typeof window !== "undefined" &&
  new URLSearchParams(window.location.search).get("walkthrough") === "sample";

export async function demoRequest(path, options = {}) {
  const data = typeof options.body === "string" ? JSON.parse(options.body) : {};
  if (path === "/access")
    return { role: "demo", allowed: true, state: "active" };
  if (path === "/status") return { emailReady: false };
  if (path === "/workflow-draft") return { available: false };
  if (path === "/feedback/details")
    return {
      details:
        data.finding?.detail ||
        data.finding?.message ||
        "Review this section for specific examples, clear wording, and consistent formatting.",
    };
  if (path === "/feedback/positive")
    return {
      feedback:
        (data.findings || [])
          .map((f) => `${f.title}: ${f.evidence}\n${f.suggestion || ""}`)
          .join("\n\n") ||
        "No completed strengths are available for this review yet.",
    };
  if (path === '/feedback/roast') return { roast: "Your accomplishments are strong enough to stop hiding on page two. You made the hiring manager play hide-and-seek, then put all the good stuff in the one place they might never look.\n\nPut the best parts up front. This is a job application, not a treasure hunt. Nobody should need a map to find out you’re good at your job." };
  if (path === '/feedback/glow-up-roast') return { jokes: [
    "Remember when your skills played hide-and-seek in a paragraph? We found them. The hiring manager can finally stop scrolling and go eat lunch.",
    "Same person, same work, and suddenly your resume looks like it pays its bills on time. Your old layout owes it an apology.",
    "Now click Apply instead of fixing one comma and saving final_FINAL_really_final. The comma has had enough attention. Go get the interview."
  ] };
  if (path === "/cover-letter")
    return {
      text:
        demoCoverLetter +
        (data.suggestions?.trim()
          ? "\n\nDemo suggestion for review: " + data.suggestions.trim()
          : ""),
    };
  if (path === "/job-description/link" || path === "/job-description/file")
    return { text: demoJobDescription };
  if (path === "/job-prep")
    return {
      study: [
        "Java service design",
        "Spring Boot APIs",
        "PostgreSQL query tuning",
        "Idempotent processing",
        "Automated testing",
        "Cross-team delivery",
      ],
      prepare: [
        "Explain the database upgrade described in the fictional sample.",
        "Prepare a mentoring example with your specific contribution.",
      ],
      facts: [],
      researchLimits:
        "Demo only. No company, founder, or review research was performed.",
      interviewQuestions: [
        "How did you investigate slow reporting queries?",
        "How did you plan database compatibility checks?",
        "How did you coordinate a release across teams?",
        "How do you mentor engineers?",
      ].map((question) => ({
        question,
        whyLikely: "Relevant to the sample software role.",
        howToPrepare:
          "Use the corresponding example in Alex Rivera’s supplied resume.",
        basis:
          "Illustrative practice question, not verified employer interview evidence.",
        sources: [],
      })),
    };
  if (path === "/resume/pdf") {
    const { createDemoResumePdf } = await import("./demo-resume-pdf.js");
    return createDemoResumePdf(data.resume);
  }
  throw new Error(
    "This action is available after signing in. The demo uses sample data and does not send emails or run live research.",
  );
}
