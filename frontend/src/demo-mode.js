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
  if (path === "/feedback/roast")
    return {
      roast: [
        "You built useful tools, but this resume makes the recruiter do all the project management. Give the big wins some breathing room. They should not need a search party.",
        "Three projects with example links. Bold choice. That is a restaurant menu that says ‘trust me, I can cook.’ Show us the work.",
        "Remember: this is a resume, not witness protection for your accomplishments. Stop making the hiring manager identify your best work from a blurry description.",
      ].join("\n\n"),
    };
  if (path === "/feedback/glow-up-roast")
    return {
      jokes: [
        "There you are. Same engineer, clearer story. The first draft made your accomplishments look like they were trying to leave the party without saying goodbye.",
        "The projects finally have a name, a stack, and a point. They have stopped introducing themselves like three strangers you vaguely remember from a group assignment.",
        "Remember: you did not need a fictional promotion. You needed your resume to stop acting like it was written by the other candidate.",
      ],
    };

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
