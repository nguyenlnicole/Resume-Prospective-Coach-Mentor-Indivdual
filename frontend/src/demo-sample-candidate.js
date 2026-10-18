import resume from "./demo-sample-candidate.json" with { type: "json" };
import { normalizeResume, projectList, replaceProjectLinks } from "./rules.js";
export const demoResume = resume;
export const demoProjectLinks = ["schedule", "reports", "library"].map(name => `https://github.com/example/${name} | https://example.com/${name}`);
export const demoSummary = resume.summary;
export const demoFinishedResume = normalizeResume({...resume, summary:demoSummary, projects:projectList(resume.projects).map((project,index)=>replaceProjectLinks(project,demoProjectLinks[index])).join("\n\n")});
const finding = (id,title,status,evidence,suggestion) => ({id,title,status,assessment:status,evidence,suggestion,detail:evidence+" "+suggestion,points:status==="pass"?"1 / 1":status==="partial"?"0.5 / 1":status==="unverified"?"Not verified":"0 / 1"});
const formatFindings = [
finding("contact","Contact information","pass","The fictional candidate has a name, location, phone, and example email address together in the sample contact section.","Keep contact details current and use a professional email address when replacing the fictional example."),
finding("header","Name and contact header","partial","The sample supplies structured contact data; the final alignment depends on the selected preview and export layout.","Review the exported header for consistent alignment and readable spacing before sending an application."),
finding("summary","Professional summary","pass","The fictional summary describes Java services, delivery, mentoring, and documentation without introducing a career objective.","Write a concise summary of your own relevant experience and remove any statement you cannot support."),
finding("sections","Section organization","pass","The sample separates experience, skills, projects, education, and certifications so a reviewer can find the main information.","Keep only relevant sections and check that each heading accurately describes the information beneath it."),
finding("typography","Fonts and emphasis","unverified","The demonstration has no uploaded source PDF, so the original font sizes and heading emphasis cannot be independently checked.","Inspect the final document for consistent fonts, readable text, and the required heading emphasis."),
finding("margins","Page margins","unverified","This fictional sample is structured data rather than an uploaded document with measurable page coordinates and margins.","Check the exported PDF margins and make sure all content stays within the printable area of the page."),
finding("spacing","Bullet and section spacing","unverified","The sample text includes separate entries, but its source does not provide line positions for a measured spacing assessment.","Use consistent bullet indentation and section spacing, then inspect every page after downloading the file."),
finding("layout","Page layout","partial","The preview demonstrates a résumé layout using fictional content; the best page length depends on the selected profession and level.","Check the final page count and remove empty space through relevant content and balanced formatting."),
finding("skills","Grouped skills","pass","Languages, frameworks, databases, tools, and engineering practices appear in labeled groups in the fictional sample résumé.","Group related skills and keep only technologies and practices you can explain with relevant examples."),
finding("projects","Project links","partial","The scheduling, reporting, and library examples use reserved example links to demonstrate project formatting without real candidate data.","Replace demonstration links with your own working project pages and verify the destination before applying.")
];
const contentFindings = [
finding("ownership","Technical ownership","pass","The fictional scheduling role describes validation, access controls, and documentation as specific responsibilities in an example service.","Explain what you personally owned and distinguish your contribution from the wider team’s responsibilities."),
finding("ambiguity","Problem definition","partial","The sample mentions planning and compatibility checks, but it does not describe every constraint that shaped the example database upgrade.","When supported by your experience, add the main constraint and explain how you selected an appropriate approach."),
finding("architecture","Design decisions","pass","The example service includes input validation and access controls, giving a reader concrete design mechanisms rather than only general claims.","Connect important design choices to the user need or operational problem they were intended to address."),
finding("tradeoffs","Tradeoffs","partial","The fictional upgrade includes a rollback plan, but the sample does not compare alternative approaches or claim any measured cost savings.","Describe a real tradeoff when relevant, preserving uncertainty and avoiding invented financial or performance results."),
finding("standards","Engineering standards","pass","Regression coverage, code reviews, and documentation recur in the sample responsibilities and show practical approaches to consistent delivery.","Explain how the practice helped your team and preserve the scope supported by your actual experience."),
finding("systemic","Lasting improvements","partial","The fictional roles describe regression tests and troubleshooting documentation, while longer-term outcomes are intentionally left unspecified.","Add a lasting result if you can verify it; otherwise describe the improvement without inventing a numerical outcome."),
finding("leadership","Technical leadership","partial","The sample includes release coordination and design discussions, but does not claim a specific team size or formal management responsibility.","Explain the decisions you coordinated and who benefited without inflating your title or reporting responsibilities."),
finding("mentoring","Knowledge sharing","pass","The fictional résumé describes mentoring through design discussions and code reviews, along with setup instructions for new teammates.","Name the method you used to support colleagues and include an outcome only when you can substantiate it."),
finding("influence","Cross-team collaboration","pass","The example scheduling role mentions coordination with product and support colleagues while preparing an implementation for release.","Describe the shared decision or delivery problem so collaboration is more specific than a generic teamwork statement."),
finding("impact","Evidence of impact","partial","The fictional sample explains tasks and intended benefits without reporting private candidate metrics or independently verified employer results.","Add genuine outcomes and accurate measurements when available, keeping them attached to the correct role.")
];
export function demoRating(findings) {
  const value = findings.reduce(
    (sum, f) =>
      sum + (f.status === "pass" ? 1 : f.status === "partial" ? 0.5 : 0),
    0,
  );
  const unverified = findings.filter((f) => f.status === "unverified").length;
  return {
    value,
    display: String(value),
    minimum: value,
    maximum: value + unverified,
    unverified,
  };
}
const formatRating = demoRating(formatFindings),
  contentRating = demoRating(contentFindings);
const overall =
  Math.round((formatRating.value * 0.4 + contentRating.value * 0.6) * 10) / 10;
export const demoReview = {
  sample: true,
  level: "Senior",
  resume,
  questions: [],
  formatScore: formatRating.value,
  overallScore: overall,
  formatRating,
  contentRating,
  overallRating: {
    value: overall,
    display: String(overall),
    minimum: overall,
    maximum:
      Math.round(
        (formatRating.maximum * 0.4 + contentRating.maximum * 0.6) * 10,
      ) / 10,
    unverified: formatRating.unverified + contentRating.unverified,
  },
  scoringNote: "Fictional demonstration only. Scores illustrate the interface and are not a live AI assessment. No original applicant document is included. Page typography, margins, and spacing remain unverified.",
  formatFindings,
  contentFindings,
};
