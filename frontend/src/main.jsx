import { demoCreateDetails } from './demo-create-details.js';
import CreateResume from './CreateResume.jsx';
import { blankResumeDetails, createResumeError, resumeFromDetails } from './create-resume.js';
import NewProjectFields from './NewProjectFields.jsx';
import { newProjectsError, addNewProjects } from './new-projects.js';
import NewExperienceFields from './NewExperienceFields.jsx';
import { newExperienceError, removeOldestExperience } from './new-experience.js';
import { visibleReviewFindings } from "./review-findings.js";
import { demoJobUrl, demoJobTitle, demoJobDescription } from "./demo-job";
import { addRepositorySkills, newlyListedSkills } from "./repo-skills";
import ProjectSuggestions from "./ProjectSuggestions";
import {
  demoResume,
  demoReview,
  demoProjectLinks,
  demoSummary,
  demoFinishedResume,
} from "./demo-sample-candidate";
import DemoResumePdfPreview from "./DemoResumePdfPreview.jsx";
import { saveLatestWorkflow } from "./workflow-save.js";
import Features from "./Features.jsx";
import { showFeatures } from "./features-access.js";
import CertificationFields from "./CertificationFields.jsx";
import "./sample-walkthrough.css";
import { demoMode } from "./demo-mode";
import {
  mergeProjectLinkAutofill,
  preserveProjectHeadings,
  projectLinksFromEdits,
} from "./project-link-fields";
import ProjectLinkFields from "./ProjectLinkFields";
import { resumeImprovementQuestions } from "./resume-improvement-questions";
import { jobPrepDocumentText, readableJobPrepText } from "./job-prep-document";
import CoverLetterPreview from "./CoverLetterPreview";

import Resume from "./ResumeDocument";
import { SubscriptionRequest } from "./Accounts";
import { AboutButton, AboutDialog, HowButton } from "./AboutUs";
import { saveResumeDownload } from "./download";
import referenceResume from "./reference-resume.json" with { type: "json" };
import { resumeExampleFor } from "./resume-examples.js";
import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Folder,
  Upload,
  PawPrint,
  FileText,
  Monitor,
  Check,
  ArrowRight,
  ArrowLeft,
  Mail,
  UserRound,
  ShieldCheck,
  X,
  CheckCheck,
  ClipboardList,
  ChevronDown,
  Download,
  CheckCircle2,
  Info,
  AlertCircle,
  LoaderCircle,
  Settings,
  LogOut,
  Plus,
  Trash2,
  Eye,
  PenLine,
  LockKeyhole,
  RefreshCw,
  ExternalLink,
} from "lucide-react";
import {
  levels,
  levelGuidelines,
  fonts,
  sections,
  resumeSections,
  defaultResumeFont,
  sampleReview,
  preferenceGroups,
  rewriteOptions,
  howSteps,
} from "./model";
import {
  accessDurationLabel,
  normalizeResume,
  countWords,
  briefFeedback,
  briefNextStep,
  githubUsername,
  resumeFilename,
  validateResume,
  codingRequired,
  isTechnical,
  safeLink,
  normalizeWebLink,
  projectList,
  projectDemo,
  projectStack,
  credibleProjectStack,
  projectLinkUrls,
  projectLinkError,
  parseProjectLinks,
  replaceProjectLinks,
  replaceProjectStack,
  normalizeProjectHeadings,
  projectDescriptionWords,
  formatProjectLinks,
  experienceList,
  suggestedIndexes,
  suggestedProjectIndexes,
  suggestedExperienceIndexes,
  experienceYearsCovered,
  experienceBullets,
  experienceBulletLimit,
  selectExperienceBullets,
  requiresProjects,
  educationMonths,
  parseEducation,
  educationEntryError,
  formatEducation,
} from "./rules";
import { request, post } from "./api";
import { discoverProjectLinks } from "./project-link-discovery";
import { githubRepository, inspectGithubRepository } from "./github-repo";
import "./style.css";
import { CoachSignIn } from "./CoachAccess";
import CoachRecentReviews from "./CoachRecentReviews";
import CoachSavedReviews from "./CoachSavedReviews";
import { storedEmail, saveRememberedEmail } from "./remember-email";
import UploadedResumePreview from "./UploadedResumePreview";
import LegalLinks from "./LegalLinks";

import "./compact-layout.css";

const hostedDemo = import.meta.env.VITE_DEMO === "true";
const betaEdition =
  import.meta.env.VITE_APP_EDITION === "beta" ||
  ["betarp.pnbpaws.com", "resumep.pnbpaws.com"].includes(
    window.location.hostname,
  );
const coachEdition = true;
const icons = [Upload, Monitor, FileText, ClipboardList, PenLine, Mail];
const clone = (x) => structuredClone(x);
function Watercolor({ inside = false }) {
  return (
    <div
      className={inside ? "watercolor inside" : "watercolor"}
      aria-hidden="true"
    />
  );
}
function Divider() {
  return <div className="section-divider" aria-hidden="true" />;
}
function Button({ children, secondary = false, className = "", ...p }) {
  return (
    <button
      className={`${secondary ? "secondary" : "primary"} button ${className}`}
      {...p}
    >
      {children}
    </button>
  );
}
function Notice({ children, error = false }) {
  return (
    <div
      role={error ? "alert" : "status"}
      className={"notice " + (error ? "error" : "")}
    >
      <Info size={18} />
      <div>{children}</div>
    </div>
  );
}
function JobPrepSheet({ sheet, loading, error, onRetry }) {
  return (
    <details className="card job-prep-sheet">
      <summary>
        Job prep sheet{" "}
        {loading
          ? "is on its way…"
          : sheet
            ? "is ready"
            : error
              ? "needs another try"
              : "— create"}
      </summary>
      {!sheet && !loading && !error && (
        <p>
          <button type="button" className="secondary button" onClick={onRetry}>
            Create job prep sheet
          </button>
        </p>
      )}
      {loading && (
        <p role="status">
          Researching the company, role reviews, and interview themes in two
          passes…
        </p>
      )}
      {error && (
        <p role="alert">
          {error}{" "}
          <button type="button" className="text-button" onClick={onRetry}>
            Try again
          </button>
        </p>
      )}
      {sheet && (
        <>
          {sheet.company && (
            <section className="job-prep-company">
              <h3>Company at a glance</h3>
              {sheet.company.name && (
                <p>
                  <strong>{readableJobPrepText(sheet.company.name)}</strong>
                </p>
              )}
              <dl>
                {[
                  ["founders", "Founders"],
                  ["founded", "Founded: year and month"],
                  ["bio", "About the company"],
                  ["employees", "Employees"],
                ].map(([key, label]) => (
                  <div key={key}>
                    <dt>{label}</dt>
                    <dd>
                      {readableJobPrepText(
                        sheet.company[key]?.text ||
                          "Not verified from available public sources.",
                      )}
                      {sheet.company[key]?.sourceUrl && (
                        <a
                          href={
                            safeLink(sheet.company[key].sourceUrl) || undefined
                          }
                          target="_blank"
                          rel="noreferrer"
                        >
                          Source:{" "}
                          {sheet.company[key].sourceTitle || "Read source"}
                        </a>
                      )}
                    </dd>
                  </div>
                ))}
              </dl>
            </section>
          )}
          <div className="job-prep-columns">
            <div>
              <h3>6 things to study</h3>
              <ol>
                {sheet.study.map((item, index) => (
                  <li key={index}>{item}</li>
                ))}
              </ol>
            </div>
            <div>
              <h3>2 things to prep</h3>
              <ol>
                {sheet.prepare.map((item, index) => (
                  <li key={index}>{item}</li>
                ))}
              </ol>
            </div>
          </div>
          {sheet.interviewQuestions?.length > 0 && (
            <section>
              <h3>4 likely interview questions</h3>
              <p>
                Practice predictions—not guaranteed questions from this
                employer.
              </p>
              <ol className="job-prep-questions">
                {sheet.interviewQuestions.map((item, index) => (
                  <li key={index}>
                    <strong>{readableJobPrepText(item.question)}</strong>
                    <p>Why: {readableJobPrepText(item.whyLikely)}</p>
                    <p>Prepare: {readableJobPrepText(item.howToPrepare)}</p>
                    <small>{item.basis}</small>
                    {item.sources?.map((source, i) => (
                      <a
                        key={i}
                        href={safeLink(source.sourceUrl) || undefined}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Source: {source.sourceTitle || "Read source"}
                      </a>
                    ))}
                  </li>
                ))}
              </ol>
            </section>
          )}
          <h3>Outside facts</h3>
          <p>These come from linked sources, not the job posting.</p>
          <ol className="job-prep-facts">
            {sheet.facts.map((item, index) => (
              <li key={index}>
                <p>{readableJobPrepText(item.fact)}</p>
                <a
                  href={safeLink(item.sourceUrl) || undefined}
                  target="_blank"
                  rel="noreferrer"
                >
                  Source: {item.sourceTitle || "Read the source"}
                </a>
              </li>
            ))}
          </ol>
          {sheet.facts.length < 5 && (
            <p>
              We found {sheet.facts.length} sourced facts. We left out anything
              we could not verify.
            </p>
          )}
          {sheet.researchLimits && (
            <p className="microcopy">
              Research limits: {readableJobPrepText(sheet.researchLimits)}
            </p>
          )}
        </>
      )}
    </details>
  );
}
function ScaledResume(props) {
  const frame = useRef(null);
  const [scale, setScale] = useState(1);
  useEffect(() => {
    let active = true;
    const fitExample = () => {
      const paper = frame.current?.querySelector(".resume-paper");
      if (!active || !paper) return;
      const top = parseFloat(getComputedStyle(paper).paddingTop);
      const bottom = parseFloat(getComputedStyle(paper).paddingBottom);

      const target = Math.min(1056 - bottom, 1056 * 0.85 + top);
      const last = paper.lastElementChild;
      const contentBottom = () => last.offsetTop + last.offsetHeight;
      paper.style.setProperty("--example-leading", "1.2");
      if (contentBottom() >= target) return;
      let low = 1.2,
        high = 1.5;
      for (let i = 0; i < 12; i++) {
        const leading = (low + high) / 2;
        paper.style.setProperty("--example-leading", String(leading));
        if (contentBottom() > target) high = leading;
        else low = leading;
      }
      paper.style.setProperty("--example-leading", String(low));
    };
    fitExample();
    document.fonts.ready.then(fitExample);
    return () => {
      active = false;
    };
  }, [props.resume, props.level]);
  useEffect(() => {
    const observer = new ResizeObserver(([entry]) =>
      setScale(Math.min(1, entry.contentRect.width / 816)),
    );
    if (frame.current) observer.observe(frame.current);
    return () => observer.disconnect();
  }, []);
  return (
    <div
      ref={frame}
      className="scaled-resume-frame"
      style={{ height: 1056 * scale }}
    >
      <div
        className="scaled-resume-page"
        style={{ transform: `scale(${scale})` }}
      >
        <Resume {...props} mini={false} />
      </div>
    </div>
  );
}
function PdfPreview({
  resume,
  level,
  font,
  dates,
  linkColor = "blue",
  projectFormat = "compact",
  mini = false,
}) {
  const [preview, setPreview] = useState(null);
  const [problem, setProblem] = useState("");
  useEffect(() => {
    let current = true;
    let url;
    setPreview(null);
    setProblem("");
    if (demoMode) return;
    const timer = setTimeout(async () => {
      try {
        const image = await post("/resume/preview", {
          resume: {
            ...resume,
            projects: formatProjectLinks(resume.projects, projectFormat),
          },
          level,
          font,
          dates,
          linkColor,
        });
        if (!current) return;
        url = URL.createObjectURL(image);
        setPreview(url);
      } catch (e) {
        if (current) setProblem(e.message);
      }
    }, 400);
    return () => {
      current = false;
      clearTimeout(timer);
      if (url) URL.revokeObjectURL(url);
    };
  }, [resume, level, font, dates, linkColor, projectFormat]);
  return (
    <div className="resume-preview-container">
      {preview ? (
        <>
          <div className="document-preview-frame">
            <img
              className="pdf-preview"
              src={preview}
              alt={`Your resume at its exported proportions in ${font}`}
            />
          </div>
        </>
      ) : (
        <ScaledResume
          resume={{
            ...resume,
            projects: formatProjectLinks(resume.projects, projectFormat),
          }}
          level={level}
          font={font}
          dates={dates}
          linkColor={linkColor}
          mini={mini}
        />
      )}
      {problem && <p className="microcopy">PDF preview: {problem}</p>}
      <details className="preview-options">
        <summary>PDF font details</summary>
        <p className="microcopy">
          If your selected font is unavailable, the PDF uses an open-source
          alternative: Calibri → Carlito; Cambria → Caladea; Georgia → DejaVu
          Serif; Arial or Aptos → Liberation Sans; Times New Roman → Liberation
          Serif. The PDF preview shows the exported PDF appearance. Word may
          reflow if fonts or page settings differ on the device opening it.
        </p>
      </details>
      {preview && (
        <a
          className="preview-full-size"
          href={preview}
          target="_blank"
          rel="noreferrer"
        >
          View resume full size
        </a>
      )}
    </div>
  );
}
function DocumentSwitcher({
  resume,
  aiResume,
  level,
  font,
  dates,
  linkColor,
  projectFormat,
  coverLetter,
  onEditCover,
  onViewJobPrep,
  initial = "original",
  showCover = true,
}) {
  const [view, setView] = useState(initial);
  useEffect(() => setView(initial), [initial]);
  return (
    <div className="document-switcher">
      <div
        className="document-tabs"
        role="group"
        aria-label="Choose a document to preview"
      >
        <button
          type="button"
          aria-pressed={view === "original"}
          onClick={() => setView("original")}
        >
          Your resume
        </button>
        <button
          type="button"
          aria-pressed={view === "template"}
          disabled={!aiResume}
          onClick={() => setView("template")}
        >
          PNB Paws resume
        </button>
        {showCover && (
          <span className="document-related-actions">
            <button
              type="button"
              aria-pressed={view === "cover"}
              onClick={() => setView("cover")}
            >
              View cover letter
            </button>
            <button type="button" onClick={onViewJobPrep}>
              View Job Prep PDF
            </button>
          </span>
        )}
      </div>
      {view === "cover" ? (
        <CoverLetterPreview
          text={coverLetter}
          contact={resume.contact}
          onEdit={onEditCover}
        />
      ) : (
        <PdfPreview
          resume={view === "template" && aiResume ? aiResume : resume}
          level={level}
          font={view === "template" ? defaultResumeFont(level) : font}
          dates={view === "template" ? "right" : dates}
          linkColor={linkColor}
          projectFormat={
            demoMode && view === "template" ? "compact" : projectFormat
          }
        />
      )}
    </div>
  );
}
function Modal({ title, onClose, children, wide = false }) {
  const ref = useRef();
  useEffect(() => {
    const el = ref.current;
    el.showModal();
    return () => el.close();
  }, []);
  return (
    <dialog
      ref={ref}
      className={"modal " + (wide ? "wide" : "")}
      onCancel={onClose}
      onClick={(e) => {
        if (e.target === ref.current) onClose();
      }}
    >
      {wide && <Watercolor inside />}
      <button
        className="icon-button close"
        aria-label="Close dialog"
        onClick={onClose}
      >
        <X />
      </button>
      <h2>{title}</h2>
      {children}
    </dialog>
  );
}
function HowItWorksDialog({ onClose }) {
  return (
    <Modal
      title={
        <>
          <PawPrint /> How It Works
        </>
      }
      wide
      onClose={onClose}
    >
      <p className="modal-subtitle">
        Preview your upload, see your scores, then build and compare two resume
        versions.
      </p>
      <div className="how-grid">
        {howSteps.map(([title, description], i) => {
          const Icon = icons[i];
          return (
            <article className="how-step" key={title}>
              <span className="step-number">{i + 1}</span>
              <div className="icon-orb">
                <Icon />
              </div>
              <h3>{title}</h3>
              <p>{description}</p>
              <HowExample step={i} />
              {i < howSteps.length - 1 && (
                <ArrowRight
                  className="step-arrow"
                  size={20}
                  aria-hidden="true"
                />
              )}
            </article>
          );
        })}
      </div>
      <Notice>
        Your selections help us prioritize truthful, relevant details within the
        finished resume.
      </Notice>
    </Modal>
  );
}
function ExperienceRatingPanel({ onSubmitted, submitted, beta }) {
  const [stars, setStars] = useState(null);
  const [aiStars, setAiStars] = useState(null);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  return (
    <section className="beta-rating card" aria-label="Rate your experience">
      <h2>How was your experience?</h2>
      <p>
        Rate {beta ? "the app and AI feedback" : "the AI feedback"} from 0 to 5
        stars and tell us what we should improve.
      </p>
      {submitted ? (
        <Notice>Thank you for your feedback!</Notice>
      ) : (
        <form
          onSubmit={async (event) => {
            event.preventDefault();
            if (aiStars === null || (beta && stars === null)) return;
            setBusy(true);
            setError("");
            try {
              await post(beta ? "/beta/app-rating" : "/coach/ai-rating", {
                stars: beta ? stars : undefined,
                aiStars,
                comment: comment.trim(),
              });
              onSubmitted();
            } catch (e) {
              setError(e.message);
            } finally {
              setBusy(false);
            }
          }}
        >
          {beta && (
            <>
              <strong>Overall app</strong>
              <div
                className="beta-rating-options"
                role="group"
                aria-label="Rate the app from 0 to 5 stars"
              >
                {[0, 1, 2, 3, 4, 5].map((number) => (
                  <button
                    key={number}
                    type="button"
                    className={stars === number ? "selected" : ""}
                    aria-pressed={stars === number}
                    onClick={() => setStars(number)}
                  >
                    {number} {number === 1 ? "star" : "stars"}
                  </button>
                ))}
              </div>
            </>
          )}
          <strong>AI resume feedback</strong>
          <div
            className="beta-rating-options"
            role="group"
            aria-label="Rate the AI feedback from 0 to 5 stars"
          >
            {[0, 1, 2, 3, 4, 5].map((number) => (
              <button
                key={number}
                type="button"
                className={aiStars === number ? "selected" : ""}
                aria-pressed={aiStars === number}
                onClick={() => setAiStars(number)}
              >
                {number} {number === 1 ? "star" : "stars"}
              </button>
            ))}
          </div>
          <label htmlFor="beta-rating-comment">
            What worked, or what should change?
          </label>
          <textarea
            id="beta-rating-comment"
            value={comment}
            maxLength={1000}
            required
            onChange={(event) => setComment(event.target.value)}
          />
          {error && <Notice error>{error}</Notice>}
          <Button
            disabled={
              aiStars === null ||
              (beta && stars === null) ||
              !comment.trim() ||
              busy
            }
          >
            {busy ? "Sending…" : "Send My Feedback"}
          </Button>
        </form>
      )}
    </section>
  );
}

function App() {
  const [createDetails, setCreateDetails] = useState(blankResumeDetails);
  const restoringWorkflow = useRef(false);
  const [progressBusy, setProgressBusy] = useState(false);
  const [savedProgress, setSavedProgress] = useState(null);
  const [progressMessage, setProgressMessage] = useState("");
  useEffect(() => {
    if (!progressMessage) return;
    const timer = setTimeout(() => setProgressMessage(""), 4500);
    return () => clearTimeout(timer);
  }, [progressMessage]);
  const [progressError, setProgressError] = useState("");
  const [jobPrepOpen, setJobPrepOpen] = useState(false);
  const [coverSuggestions, setCoverSuggestions] = useState("");
  const [coverRemaking, setCoverRemaking] = useState(false);
  const [betaRated, setBetaRated] = useState(false);
  const [page, setPage] = useState("home");
  const [modal, setModal] = useState(() =>
      new URLSearchParams(location.search).get("how") === "1" ? "how" : null,
    ),
    [email, setEmail] = useState(demoMode ? demoResume.contact.email : ""),
    [level, setLevel] = useState(demoMode ? "Senior" : "");
  const [profession, setProfession] = useState(demoMode ? "Technical" : "");
  const [codingChoice, setCodingChoice] = useState(demoMode ? true : null);
  const [file, setFile] = useState(
      demoMode
        ? new File(["Sample resume"], "Alex_Rivera_Resume.pdf", {
            type: "application/pdf",
          })
        : null,
    ),
    [consent, setConsent] = useState(demoMode),
    [drag, setDrag] = useState(false),
    [review, setReview] = useState(null),
    [resume, setResume] = useState(
      clone(demoMode ? demoResume : referenceResume),
    );
  const [preferences, setPreferences] = useState([]),
    [targetRole, setTargetRole] = useState(demoMode ? demoJobTitle : ""),
    [generalFocus, setGeneralFocus] = useState(""),
    [otherGeneralFocus, setOtherGeneralFocus] = useState(""),
    [generalService, setGeneralService] = useState(""),
    [generalProblemSolving, setGeneralProblemSolving] = useState(""),
    [generalPracticalSkills, setGeneralPracticalSkills] = useState(""),
    [generalVolunteerWork, setGeneralVolunteerWork] = useState(""),
    [medicalRoleType, setMedicalRoleType] = useState(""),
    [medicalCredentials, setMedicalCredentials] = useState(""),
    [medicalSystems, setMedicalSystems] = useState(""),
    [medicalExperience, setMedicalExperience] = useState(""),
    [jobDescription, setJobDescription] = useState(
      demoMode ? demoJobDescription : "",
    ),
    [jobLink, setJobLink] = useState(demoMode ? demoJobUrl : ""),
    [jobLoading, setJobLoading] = useState(false),
    [jobPrep, setJobPrep] = useState(null),
    [jobPrepLoading, setJobPrepLoading] = useState(false),
    [jobPrepError, setJobPrepError] = useState(""),
    [selectedExperience, setSelectedExperience] = useState(null),
    [bulletChoices, setBulletChoices] = useState({}),
    [roleDetails, setRoleDetails] = useState({}),
    [newExperience, setNewExperience] = useState({}),
    [newProjects, setNewProjects] = useState([]),
    [selectedProjects, setSelectedProjects] = useState(null),
    [projectStacks, setProjectStacks] = useState({}),
    [repoChecks, setRepoChecks] = useState({}),
    [projectLinks, setProjectLinks] = useState({}),
    [linkWarning, setLinkWarning] = useState([]),
    [font, setFont] = useState("Calibri"),
    [linkColor, setLinkColor] = useState("blue"),
    [projectFormat, setProjectFormat] = useState(
      demoMode ? "descriptive" : "compact",
    ),
    [dates, setDates] = useState("right"),
    [layout, setLayout] = useState(null),
    [aiResume, setAiResume] = useState(null),
    [aiSource, setAiSource] = useState(null),
    [aiBusy, setAiBusy] = useState(false),
    [aiError, setAiError] = useState(""),
    [rewrite, setRewrite] = useState(rewriteOptions[0]),
    [otherRewrite, setOtherRewrite] = useState(""),
    [otherPriority, setOtherPriority] = useState(""),
    [answers, setAnswers] = useState({}),
    [profileLinkedin, setProfileLinkedin] = useState(""),
    [profileGithub, setProfileGithub] = useState(""),
    [projectDemoUrl, setProjectDemoUrl] = useState(""),
    [linkDiscoveryNote, setLinkDiscoveryNote] = useState(""),
    [profilePortfolio, setProfilePortfolio] = useState(""),
    [proposal, setProposal] = useState(null),
    [proposalApproved, setProposalApproved] = useState(false),
    [editSection, setEditSection] = useState(null);
  const [mobilePreviewOpen, setMobilePreviewOpen] = useState(false);
  const [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [feedbackDetails, setFeedbackDetails] = useState({}),
    [positiveFeedback, setPositiveFeedback] = useState({}),
    [roastFeedback, setRoastFeedback] = useState({}),
    [glowRoast, setGlowRoast] = useState(null),
    [rerates, setRerates] = useState({}),
    [feedbackEmail, setFeedbackEmail] = useState({}),
    [message, setMessage] = useState(""),
    [reviewProgress, setReviewProgress] = useState(0),
    [reviewSeconds, setReviewSeconds] = useState(0),
    [coverLetter, setCoverLetter] = useState(""),
    [coverOpen, setCoverOpen] = useState(false),
    [wantCoverLetter, setWantCoverLetter] = useState(demoMode),
    [coverBusy, setCoverBusy] = useState(false),
    [coverError, setCoverError] = useState(""),
    [status, setStatus] = useState(null);
  const [deliveryConsent, setDeliveryConsent] = useState(false),
    [emailTo, setEmailTo] = useState("");
  const [access, setAccess] = useState({ state: "loading", allowed: false });
  const accessRef = useRef(access);
  accessRef.current = access;
  const [desktopPreview, setDesktopPreview] = useState(false);
  const profileHelpEnabled = isTechnical(
    profession === "Technical" ? level : profession || review?.level,
  );
  async function refreshAccess(verifiedAccess) {
    if (
      verifiedAccess &&
      typeof verifiedAccess.allowed === "boolean" &&
      typeof verifiedAccess.role === "string"
    ) {
      setAccess(verifiedAccess);
      return verifiedAccess;
    }
    try {
      setAccess(await request("/access"));
    } catch {
      setAccess({ state: "offline", allowed: false });
    }
  }
  useEffect(() => {
    refreshAccess();
    if (location.protocol === "pnb-app:")
      fetch("/desktop-info")
        .then((r) => r.json())
        .then((d) => setDesktopPreview(d.preview))
        .catch(() => {});
    let lastActivity = Date.now();
    let activitySinceCheck = false;
    let signingOut = false;
    const markActivity = () => {
      lastActivity = Date.now();
      activitySinceCheck = true;
    };
    if (coachEdition) {
      for (const event of [
        "pointermove",
        "pointerdown",
        "keydown",
        "scroll",
        "touchstart",
      ])
        window.addEventListener(event, markActivity, { passive: true });
    }
    const timer = setInterval(() => {
      if (
        accessRef.current.role === "coach" &&
        !accessRef.current.keepLoggedIn &&
        Date.now() - lastActivity >= 15 * 60 * 1000
      ) {
        if (!signingOut) {
          signingOut = true;
          post("/coach/logout", {})
            .catch(() => {})
            .finally(() => {
              setAccess({ role: "guest", state: "sign-in", allowed: false });
              signingOut = false;
            });
        }
      } else if (activitySinceCheck || accessRef.current.role === "coach") {
        activitySinceCheck = false;
        refreshAccess();
      }
    }, 30000);
    const checkOnReturn = () => {
      if (document.visibilityState === "visible") refreshAccess();
    };
    document.addEventListener("visibilitychange", checkOnReturn);
    window.addEventListener("focus", checkOnReturn);
    window.addEventListener("pnb-access-changed", refreshAccess);
    return () => {
      clearInterval(timer);
      if (coachEdition) {
        for (const event of [
          "pointermove",
          "pointerdown",
          "keydown",
          "scroll",
          "touchstart",
        ])
          window.removeEventListener(event, markActivity);
      }
      document.removeEventListener("visibilitychange", checkOnReturn);
      window.removeEventListener("focus", checkOnReturn);
      window.removeEventListener("pnb-access-changed", refreshAccess);
    };
  }, []);
  useEffect(() => {
    const shell = document.querySelector(".app-shell");
    if (!shell || page !== "home" || window.innerWidth > 760) return;
    let frame,
      cancelled = false;
    const fit = () => {
      if (cancelled) return;
      if (/INPUT|TEXTAREA|SELECT/.test(document.activeElement?.tagName)) return;
      shell.style.zoom = "1";
      frame = requestAnimationFrame(() => {
        const available = window.innerHeight - 12;
        const height = shell.getBoundingClientRect().height;
        if (height > available)
          shell.style.zoom = String(Math.min(1, available / height));
      });
    };
    fit();
    window.addEventListener("resize", fit);
    document.fonts.ready.then(fit);
    return () => {
      cancelled = true;
      cancelAnimationFrame(frame);
      window.removeEventListener("resize", fit);
      shell.style.zoom = "";
    };
  }, [page, profession, level, access.role, file, error]);
  useEffect(() => {
    if (!access.allowed && !demoMode) {
      setCreateDetails(blankResumeDetails());
      setFile(null);
      setReview(null);
      setFeedbackDetails({});
      setPositiveFeedback({});
      setRoastFeedback({});
      setGlowRoast(null);
      setRerates({});
      setFeedbackEmail({});
      setProposal(null);
      setProposalApproved(false);
      setResume(clone(demoMode ? demoResume : referenceResume));
      setAiResume(null);
      setAiError("");
      setAnswers({}); setNewExperience({}); setNewProjects([]);
      setProfileLinkedin("");
      setProfileGithub("");
      setProfilePortfolio("");
      setDeliveryConsent(false);
      setEmail("");
      setEmailTo("");
      setProfession("");
      setLevel("");
      setPreferences([]);
      setOtherPriority("");
      setOtherRewrite("");
      setTargetRole(demoMode ? demoJobTitle : "");
      setGeneralFocus("");
      setOtherGeneralFocus("");
      setGeneralService("");
      setGeneralProblemSolving("");
      setGeneralPracticalSkills("");
      setGeneralVolunteerWork("");
      setMedicalRoleType("");
      setMedicalCredentials("");
      setMedicalSystems("");
      setMedicalExperience("");
      setJobDescription(demoMode ? demoJobDescription : "");
      setJobLink(demoMode ? demoJobUrl : "");
      setJobPrep(null);
      setJobPrepLoading(false);
      setJobPrepError("");
      setSelectedExperience(null);
      setBulletChoices({});
      setRoleDetails({});
      setSelectedProjects(null);
      setProjectStacks({});
      setRepoChecks({});
      setProjectLinks(
        demoMode
          ? Object.fromEntries(
              demoProjectLinks.map((links, index) => [index, links]),
            )
          : {},
      );
      setLinkWarning([]);
    }
  }, [access.allowed]);
  const inputRef = useRef();
  const aiRequestId = useRef(0);
  useEffect(() => {
    request("/status")
      .then(setStatus)
      .catch(() => {});
  }, []);
  useEffect(() => {
    window.scrollTo({ top: 0 });
    setError("");
    setMessage("");
  }, [page]);
  const go = (p) => {
    if (coachEdition && ["admin", "forgot", "reset"].includes(p)) return;
    setPage(p);
    history.replaceState(null, "", p === "home" ? location.pathname : "#" + p);
  };
  useEffect(() => {
    window.pnbAppBack = () => {
      if (modal) {
        setModal(null);
        return true;
      }
      if (editSection) {
        setEditSection(null);
        return true;
      }
      const previous = {
        "create-resume": "home",
        "upload-preview": "home",
        format: "home",
        overall: "format",
        invitation: "overall",
        preferences: "invitation",
        questions: "preferences",
        approval: "questions",
        edit: "preferences",
        layout: "edit",
        complete: "layout",
      }[page];
      if (!previous) return false;
      go(previous);
      return true;
    };
    return () => {
      delete window.pnbAppBack;
    };
  }, [page, modal, editSection, access.allowed]);
  const demo = demoMode || Boolean(review?.sample);
  const selectedStyle =
    layout === "template"
      ? { font: defaultResumeFont(profession), dates: "right", linkColor }
      : { font, dates, linkColor };
  const selectedResume = layout === "template" && aiResume ? aiResume : resume;
  const selectedProjectFormat =
    demoMode && layout === "template" ? "compact" : projectFormat;
  const currentGlowRoast =
    glowRoast?.source === selectedResume ? glowRoast : null;
  useEffect(() => {
    if (restoringWorkflow.current) {
      restoringWorkflow.current = false;
      return;
    }
    aiRequestId.current++;
    setLayout(null);
    setAiResume(null);
    setAiSource(null);
    setAiError("");
  }, [resume]);
  const reviewLevel =
    review?.level || (profession === "Technical" ? level : profession);
  const invitationExample = resumeExampleFor(profession, reviewLevel);
  const technicalOptions = isTechnical(
    profession === "Technical" ? reviewLevel : profession || reviewLevel,
  );
  const gapQuestions = resumeImprovementQuestions(review, reviewLevel);
  const [visibleQuestionCount, setVisibleQuestionCount] = useState(4);
  const [addedRepoSkills, setAddedRepoSkills] = useState([]);
  useEffect(() => {
    setVisibleQuestionCount(4);
    setAddedRepoSkills([]);
  }, [review]);
  useEffect(() => {
    if (
      !technicalOptions ||
      !["preferences", "questions"].includes(page) ||
      demo ||
      !review?.resume?.projects ||
      !normalizeWebLink(profileGithub)
    )
      return;
    let active = true;
    const timer = setTimeout(async () => {
      try {
        const found = await discoverProjectLinks(
          projectList(review.resume.projects),
          normalizeWebLink(profileGithub),
          "",
        );
        if (!active) return;
        setProjectLinks((current) => {
          const next = { ...current };
          for (const [index, links] of Object.entries(found)) {
            next[index] = mergeProjectLinkAutofill(
              current[index] ??
                projectLinkUrls(
                  projectList(review.resume.projects)[index],
                ).join(" | "),
              links,
            );
          }
          return next;
        });
        setLinkDiscoveryNote(
          Object.keys(found).length
            ? "Matching GitHub project links filled in. Review the repo and demo fields below."
            : "GitHub was checked, but no public repository matched these project names. Add each project's direct repo link to identify it.",
        );
      } catch {
        if (active)
          setLinkDiscoveryNote(
            "GitHub could not be read right now. Use Check GitHub again, or add a direct repository link.",
          );
      }
    }, 600);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [page, profileGithub, review?.resume?.projects, technicalOptions]);
  const roleContext =
    reviewLevel === "General"
      ? {
          focus:
            generalFocus === "Other" ? otherGeneralFocus.trim() : generalFocus,
          service: generalService,
          problemSolving: generalProblemSolving,
          practicalSkills: generalPracticalSkills,
          volunteerWork: generalVolunteerWork,
        }
      : reviewLevel === "Medical Field"
        ? {
            roleType: medicalRoleType,
            credentials: medicalCredentials,
            nursingStage: answers.nursingStage || "",
            hasClinicalRotations: answers.hasClinicalRotations || "",
            clinicalRotations:
              answers.hasClinicalRotations === "Yes"
                ? answers.clinicalRotations || ""
                : "",
            systems: medicalSystems,
            relevantExperience: medicalExperience,
            externshipTraining: answers.medicalExternshipTraining || "",
            clinicalTraining: /phlebotom/i.test(targetRole)
              ? answers.phlebotomyClinicalTraining || ""
              : "",
            proceduresSpecialty: /physician assistant|\bPA\b/i.test(targetRole)
              ? answers.paProceduresSpecialty || ""
              : "",
            physicianCv: /physician(?! assistant)|\b(?:MD|DO)\b/i.test(
              targetRole,
            )
              ? {
                  residencyFellowship: answers.physicianResidency || "",
                  licensure: answers.physicianLicensure || "",
                  boardCertification: answers.physicianBoard || "",
                  research: answers.physicianResearch || "",
                  publications: answers.physicianPublications || "",
                  teaching: answers.physicianTeaching || "",
                  presentations: answers.physicianPresentations || "",
                }
              : null,
          }
        : {};
  const aiInputKey = JSON.stringify([
    resume,
    jobDescription.trim(),
    targetRole.trim(),
    roleContext,
  ]);
  const coding = isTechnical(reviewLevel) && codingChoice === true;
  const guideline = levelGuidelines[reviewLevel];
  const experienceOptions = experienceList(review?.resume?.experience);
  const projectOptions = projectList(review?.resume?.projects);
  const repoTechnologyRequests = useRef(new Map());
  const projectRepoKey = JSON.stringify(
    projectOptions.map((project, index) => ({
      project,
      index,
      repo:
        parseProjectLinks(
          projectLinks[index] ?? projectLinkUrls(project).join(" | "),
        )
          .map(normalizeWebLink)
          .find((url) => githubRepository(url)) || "",
    })),
  );
  useEffect(() => {
    if (page !== "questions" || demo || !coding) return;
    let active = true;
    setRepoChecks({});
    const timer = setTimeout(() => {
      for (const { project, index, repo } of JSON.parse(projectRepoKey)) {
        if (!repo || (projectStacks[index] ?? projectStack(project)).trim())
          continue;
        setRepoChecks((current) => ({
          ...current,
          [index]: { loading: true },
        }));
        if (!repoTechnologyRequests.current.has(repo)) {
          const request = inspectGithubRepository(repo).catch((error) => {
            repoTechnologyRequests.current.delete(repo);
            throw error;
          });
          repoTechnologyRequests.current.set(repo, request);
        }
        repoTechnologyRequests.current
          .get(repo)
          .then((technologies) => {
            if (!active) return;
            setRepoChecks((current) => ({
              ...current,
              [index]: technologies.length
                ? { technologies, url: repo }
                : {
                    error:
                      "No technologies were found in this public repo. You can add them yourself or leave this optional field blank.",
                  },
            }));
            if (technologies.length)
              setProjectStacks((current) =>
                (current[index] ?? projectStack(project)).trim()
                  ? current
                  : { ...current, [index]: technologies.join(", ") },
              );
          })
          .catch((error) => {
            if (active)
              setRepoChecks((current) => ({
                ...current,
                [index]: { error: error.message },
              }));
          });
      }
    }, 700);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [page, demo, coding, projectRepoKey]);
  const seniorExperience = ["Senior", "Management"].includes(reviewLevel);
  const experienceMin = seniorExperience ? 4 : 3;
  const experienceMax = seniorExperience ? 6 : 4;
  const experienceChoiceRequired =
    experienceOptions.length > (seniorExperience ? 5 : 4);
  const keptExperience =
    selectedExperience ??
    suggestedExperienceIndexes(experienceOptions, targetRole, reviewLevel);
  const keptProjects =
    selectedProjects ?? suggestedProjectIndexes(projectOptions, targetRole);
  const selectedJobs = keptExperience.map((index) => experienceOptions[index]);
  const bulletLimitFor = (index) =>
    experienceBulletLimit(selectedJobs, keptExperience.indexOf(index));
  useEffect(() => {
    if (page !== "edit" || editSection || aiSource === aiInputKey) return;
    const timer = setTimeout(() => generateAiVersion(resume), 2500);
    return () => clearTimeout(timer);
  }, [page, editSection, reviewLevel, aiSource, aiInputKey]);
  function toggleSelection(
    index,
    items,
    selected,
    setSelected,
    limit,
    isExperience = false,
  ) {
    const current =
      selected ??
      (isExperience
        ? suggestedExperienceIndexes(items, targetRole, reviewLevel)
        : suggestedProjectIndexes(items, targetRole));
    setSelected(
      current.includes(index)
        ? current.filter((item) => item !== index)
        : [...current, index].sort((a, b) => a - b),
    );
  }
  function selectFile(f) {
    if (demoMode) return;
    setError("");
    if (!f) return;
    if (!/\.(pdf|docx?|png|jpe?g)$/i.test(f.name)) {
      setFile(null);
      setError("Choose a PDF, Word document, PNG, or JPG resume.");
      return;
    }
    if (f.size > 10 * 1024 * 1024) {
      setFile(null);
      setError("Choose a resume file smaller than 10 MB.");
      return;
    }
    setFile(f);
    setReview(null);
    setResume({
      contact: {
        name: "",
        location: "",
        phone: "",
        email: "",
        linkedin: "",
        github: "",
        portfolio: "",
      },
      summary: "",
      experience: "",
      skills: "",
      projects: "",
      education: "",
      certifications: "",
    });
    setProposal(null);
    setAnswers({}); setNewExperience({}); setNewProjects([]);
    setProgressMessage("");
    setProgressError("");
  }
  async function createFromScratch() {
    for (let step = 0; step < 3; step++) {
      const issue = createResumeError(createDetails, step);
      if (issue) { setError(issue); setCreateDetails(current => ({...current, step})); return; }
    }
    setBusy(true); setError("");
    try {
      const selected = createDetails.profession === "Technical" ? createDetails.level : createDetails.profession;
      const initialResume = resumeFromDetails(createDetails);
      const result = demo ? {resume: initialResume, level: selected, questions: [], sample: true}
        : await post("/resume/create", {resume: initialResume, level: selected, email: createDetails.email.trim(), consent: createDetails.consent});
      setFile(null); setReview(result); setResume(clone(result.resume)); setEmail(createDetails.email.trim());
      setProfession(createDetails.profession); setLevel(createDetails.level);
      setProposal(null); setProposalApproved(false); setAiResume(null); setAiSource(null); setLayout(null);
      setAnswers({}); setNewExperience({}); setNewProjects([]); setCodingChoice(null);
      setSelectedExperience(null); setBulletChoices({}); setRoleDetails({}); setSelectedProjects(null);
      setProjectLinks({}); setProjectStacks({}); setRepoChecks({}); setPreferences([]);
      setTargetRole(""); setJobDescription(""); setJobLink(""); setJobPrep(null);
      setProfileLinkedin(""); setProfileGithub(""); setProfilePortfolio(""); setProjectDemoUrl("");
      setGeneralFocus(""); setOtherGeneralFocus(""); setGeneralService(""); setGeneralProblemSolving(""); setGeneralPracticalSkills(""); setGeneralVolunteerWork("");
      setMedicalRoleType(""); setMedicalCredentials(""); setMedicalSystems(""); setMedicalExperience("");
      setWantCoverLetter(false); setCoverLetter(""); setFont(defaultResumeFont(createDetails.profession)); setDates("right");
      if (!demo) { await refreshAccess(); await post("/builder/start", {}); }
      go("preferences");
    } catch (error) { setError(error.message); }
    finally { setBusy(false); }
  }
  async function analyze(sample = demoMode) {
    setError("");
    setFeedbackDetails({});
    setPositiveFeedback({});
    setRoastFeedback({});
    setGlowRoast(null);
    setRerates({});
    setFeedbackEmail({});
    setProposal(null);
    setProposalApproved(false);
    if (!sample && (!file || !email || !consent)) {
      setError(
        "Add your email and resume file, then confirm the processing notice.",
      );
      return;
    }
    go("grading");
    setBusy(true);
    setReviewProgress(0);
    setReviewSeconds(0);
    const startedAt = Date.now();
    const timer = setInterval(() => {
      const seconds = Math.floor((Date.now() - startedAt) / 1000);
      setReviewSeconds(seconds);
      setReviewProgress(
        Math.min(
          95,
          Math.floor(95 * (1 - Math.exp(-seconds / (sample ? 0.8 : 35)))),
        ),
      );
    }, 1000);
    try {
      let result;
      if (sample) {
        await new Promise((r) => setTimeout(r, 2400));
        result = clone(demoMode ? demoReview : sampleReview);
        setLevel("Senior");
      } else {
        const body = new FormData();
        body.append("file", file);
        body.append("level", profession === "Technical" ? level : profession);
        body.append("email", email.trim());
        body.append("consent", "true");
        result = await request("/review", { method: "POST", body });
        await refreshAccess();
      }
      clearInterval(timer);
      setReviewProgress(100);
      await new Promise((resolve) => setTimeout(resolve, 400));
      setCodingChoice(demoMode ? true : null);
      setReview(result);
      setWantCoverLetter(demoMode);
      setCoverLetter("");
      setResume(clone(result.resume));
      setAnswers({}); setNewExperience({}); setNewProjects([]);
      setProfileLinkedin(result.resume?.contact?.linkedin || "");
      setProfileGithub(result.resume?.contact?.github || "");
      setProjectDemoUrl("");
      setLinkDiscoveryNote("");
      setProfilePortfolio(result.resume?.contact?.portfolio || "");
      setTargetRole(demoMode ? demoJobTitle : "");
      setGeneralFocus("");
      setOtherGeneralFocus("");
      setOtherPriority("");
      setOtherRewrite("");
      setGeneralService("");
      setGeneralProblemSolving("");
      setGeneralPracticalSkills("");
      setGeneralVolunteerWork("");
      setMedicalRoleType("");
      setMedicalCredentials("");
      setMedicalSystems("");
      setMedicalExperience("");
      setJobDescription(demoMode ? demoJobDescription : "");
      setJobLink(demoMode ? demoJobUrl : "");
      setSelectedExperience(null);
      setSelectedProjects(null);
      setProjectStacks({});
      setRepoChecks({});
      setProjectLinks(
        demoMode
          ? Object.fromEntries(
              demoProjectLinks.map((links, index) => [index, links]),
            )
          : {},
      );
      setLinkWarning([]);
      setBulletChoices({});
      setRoleDetails({});
      go("format");
    } catch (e) {
      go("home");
      setTimeout(() => setError(e.message), 0);
    } finally {
      clearInterval(timer);
      setBusy(false);
    }
  }
  async function toggleFeedbackDetails(finding, group) {
    const key = `${group}:${finding.id}`;
    const current = feedbackDetails[key];
    if (current?.loading) return;
    if (current?.text) {
      setFeedbackDetails((values) => ({
        ...values,
        [key]: { ...values[key], open: !values[key].open },
      }));
      return;
    }
    setFeedbackDetails((values) => ({
      ...values,
      [key]: { open: true, loading: true },
    }));
    try {
      const result = await post("/feedback/details", {
        resume: review.resume,
        finding,
        level: reviewLevel,
      });
      setFeedbackDetails((values) => ({
        ...values,
        [key]: { open: true, text: result.details },
      }));
    } catch (e) {
      setFeedbackDetails((values) => ({
        ...values,
        [key]: { open: true, error: e.message },
      }));
    }
  }
  async function togglePositiveFeedback(group) {
    const current = positiveFeedback[group];
    if (current?.loading) return;
    if (current?.text) {
      setPositiveFeedback((values) => ({
        ...values,
        [group]: { ...current, open: !current.open },
      }));
      return;
    }
    const findings = (
      group === "format" ? review.formatFindings : review.contentFindings
    ).filter((finding) => finding.status === "pass");
    if (!findings.length) {
      setPositiveFeedback((values) => ({
        ...values,
        [group]: {
          open: true,
          text: "No checks passed in this section yet; the improvement notes show what to work on next.",
        },
      }));
      return;
    }
    setPositiveFeedback((values) => ({
      ...values,
      [group]: { open: true, loading: true },
    }));
    try {
      const result = await post("/feedback/positive", {
        resume: review.resume,
        findings,
        level: reviewLevel,
      });
      setPositiveFeedback((values) => ({
        ...values,
        [group]: { open: true, text: result.feedback },
      }));
    } catch (e) {
      setPositiveFeedback((values) => ({
        ...values,
        [group]: { open: true, error: e.message },
      }));
    }
  }
  async function emailReviewFeedback() {
    if (feedbackEmail.loading || demo) return;
    setFeedbackEmail({ loading: true });
    try {
      await post("/review/email-feedback", {});
      setFeedbackEmail({ sent: true });
    } catch (e) {
      setFeedbackEmail({ error: e.message });
    }
  }
  async function toggleRoastFeedback(group) {
    const current = roastFeedback[group];
    if (current?.loading) return;
    if (current?.text) {
      setRoastFeedback((values) => ({
        ...values,
        [group]: { ...current, open: !current.open },
      }));
      return;
    }
    const findings =
      group === "format" ? review.formatFindings : review.contentFindings;
    setRoastFeedback((values) => ({
      ...values,
      [group]: { open: true, loading: true },
    }));
    try {
      const result = await post("/feedback/roast", {
        resume: review.resume,
        findings,
        level: reviewLevel,
      });
      setRoastFeedback((values) => ({
        ...values,
        [group]: { open: true, text: result.roast },
      }));
    } catch (e) {
      setRoastFeedback((values) => ({
        ...values,
        [group]: { open: true, error: e.message },
      }));
    }
  }
  async function toggleGlowRoast() {
    if (currentGlowRoast?.loading) return;
    if (currentGlowRoast?.jokes) {
      setGlowRoast({ ...currentGlowRoast, open: !currentGlowRoast.open });
      return;
    }
    setGlowRoast({ source: selectedResume, open: true, loading: true });
    try {
      const result = await post("/feedback/glow-up-roast", {
        original: review.resume,
        finished: selectedResume,
        level: reviewLevel,
      });
      setGlowRoast({ source: selectedResume, open: true, jokes: result.jokes });
    } catch (e) {
      setGlowRoast({ source: selectedResume, open: true, error: e.message });
    }
  }
  async function rerateVersion(version) {
    const source = version === "template" ? aiResume : resume;
    if (!source || rerates[version]?.loading) return;
    const style =
      version === "template"
        ? { font: defaultResumeFont(reviewLevel), dates: "right" }
        : { font, dates };
    setRerates((values) => ({
      ...values,
      [version]: { source, loading: true },
    }));
    try {
      const result = await post("/review/finished", {
        resume: {
          ...source,
          projects: formatProjectLinks(source.projects, projectFormat),
        },
        level: reviewLevel,
        ...style,
        linkColor,
      });
      setRerates((values) => ({ ...values, [version]: { source, result } }));
    } catch (e) {
      setRerates((values) => ({
        ...values,
        [version]: { source, error: e.message },
      }));
    }
  }
  async function build() {
    const projectError = technicalOptions ? newProjectsError(newProjects) : "";
    if (projectError) { setError(projectError); return; }
    const experienceError = newExperienceError(newExperience);
    if (experienceError) { setError(experienceError); return; }
    setBusy(true);
    setError("");
    setProposal(null);
    setProposalApproved(false);
    try {
      const chosenResume = {
        ...clone(review.resume),
        softwareEngineering: coding,
      };
      if (profileHelpEnabled)
        chosenResume.contact = {
          ...chosenResume.contact,
          linkedin:
            normalizeWebLink(profileLinkedin) ||
            chosenResume.contact?.linkedin ||
            "",
          github:
            githubUsername(profileGithub) && !githubRepository(profileGithub)
              ? normalizeWebLink(profileGithub)
              : chosenResume.contact?.github || "",
          portfolio:
            normalizeWebLink(profilePortfolio) ||
            chosenResume.contact?.portfolio ||
            "",
        };
      const chosenIndexes = experienceChoiceRequired
        ? keptExperience
        : experienceOptions.map((_, index) => index);
      const combineExperience = [];
      chosenResume.experience = chosenIndexes
        .map((index, position) => {
          const entry = experienceOptions[index];
          const extra = (roleDetails[index] || "")
            .split(/\r?\n/)
            .map((line) => line.trim())
            .filter(Boolean)
            .map((line) => `• ${line}`)
            .join("\n");
          if (experienceBullets(entry).length <= bulletLimitFor(index))
            return extra ? `${entry}\n${extra}` : entry;
          const choice = bulletChoices[index];
          if (choice?.mode === "combine") {
            combineExperience.push(position);
            return entry;
          }
          return choice?.mode === "select"
            ? selectExperienceBullets(entry, choice.selected || [])
            : extra
              ? `${entry}\n${extra}`
              : entry;
        })
        .join("\n\n");
      if (newExperience.enabled && newExperience.replaceOldest !== false) {
        const before = experienceList(chosenResume.experience);
        chosenResume.experience = removeOldestExperience(chosenResume.experience);
        const after = experienceList(chosenResume.experience);
        const remapped = combineExperience.map(index => after.indexOf(before[index])).filter(index => index >= 0);
        combineExperience.splice(0, combineExperience.length, ...remapped);
      }
      const projectIndexes =
        coding && projectOptions.length > 3
          ? keptProjects
          : projectOptions.map((_, index) => index);
      chosenResume.projects = projectIndexes
        .map((index) => {
          const links =
            projectLinks[index] ??
            projectLinkUrls(projectOptions[index]).join(" | ");
          const project = replaceProjectLinks(projectOptions[index], links);
          return coding
            ? replaceProjectStack(
                project,
                projectStacks[index] ?? projectStack(project),
              )
            : project;
        })
        .join("\n\n");
      if (technicalOptions) chosenResume.projects = addNewProjects(chosenResume.projects, newProjects);
      const repoResults = demo
        ? []
        : await Promise.allSettled(
            projectIndexes.map(async (index) => {
              const links =
                projectLinks[index] ??
                projectLinkUrls(projectOptions[index]).join(" | ");
              const repo = parseProjectLinks(links)
                .map(normalizeWebLink)
                .find((url) => githubRepository(url));
              if (!repo) return [];
              return (
                repoTechnologyRequests.current.get(repo) ||
                inspectGithubRepository(repo)
              );
            }),
          );
      const discoveredSkills = repoResults.flatMap((result) =>
        result.status === "fulfilled" ? result.value : [],
      );
      const skillUpdate = addRepositorySkills(
        chosenResume.skills,
        discoveredSkills,
      );
      chosenResume.skills = skillUpdate.skills;
      setAddedRepoSkills(skillUpdate.added);
      if (demo) {
        let next = {
          ...chosenResume,
          summary: demoMode ? demoSummary : chosenResume.summary,
        };
        if (answers.outcome?.trim() && answers.outcome !== "Not sure")
          next.experience += "\n• " + answers.outcome.trim();
        if (newExperience.enabled) {
          const month = value => new Date(`${value}-01T12:00:00`).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
          const heading = `${newExperience.employer} — ${newExperience.role} | ${month(newExperience.start)} – ${newExperience.current ? 'Present' : month(newExperience.end)}`;
          next.experience = `${heading}\n• ${newExperience.description.trim()}\n\n${next.experience}`;
        }
        setResume(normalizeResume(next));
      } else {
        const data = await post("/build", {
          resume: chosenResume,
          level: reviewLevel,
          font,
          dates,
          layout: "original",
          preferences: preferences.map((item) =>
            item === "Add another option" ? otherPriority.trim() : item,
          ),
          targetRole: targetRole.trim(),
          roleContext,
          jobDescription: jobDescription.trim(),
          rewrite:
            rewrite === "Add another option" ? otherRewrite.trim() : rewrite,
          answers: Object.fromEntries(
            Object.entries(answers).map(([id, value]) => [
              gapQuestions.find((q) => q.id === id)?.question || id,
              value,
            ]),
          ),
          combineExperience,
          newExperience,
          newProjects: technicalOptions ? newProjects : [],
        });
        await refreshAccess();
        const normalized = {
          ...data,
          resume: normalizeResume({
            ...data.resume,
            skills: addRepositorySkills(data.resume.skills, skillUpdate.added)
              .skills,
            projects: preserveProjectHeadings(
              chosenResume.projects,
              data.resume.projects,
            ),
          }),
        };
        setAddedRepoSkills(
          newlyListedSkills(review.resume.skills, normalized.resume.skills),
        );
        if (data.protectedChanges?.length || data.approvalQuestions?.length) {
          setProposal(normalized);
          go("approval");
          return;
        }
        setResume(normalized.resume);
        if (coachEdition)
          sendOwnerCopy("draft", normalized.resume, { font, dates, linkColor });
      }
      go("edit");
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function checkProjectRepo(index, project) {
    if (demoMode) {
      setProjectStacks((current) => ({
        ...current,
        [index]: projectStack(project),
      }));
      setLinkDiscoveryNote(
        "Demo technologies filled from the supplied resume.",
      );
      return;
    }
    const links = parseProjectLinks(
      projectLinks[index] ?? projectLinkUrls(project).join(" | "),
    );
    const repo = links.find((link) => githubRepository(link));
    if (!repo) {
      setRepoChecks((current) => ({
        ...current,
        [index]: { error: "Add a public GitHub repository link first." },
      }));
      return;
    }
    setRepoChecks((current) => ({ ...current, [index]: { loading: true } }));
    try {
      const technologies = await inspectGithubRepository(repo);
      if (technologies.length)
        setProjectStacks((current) =>
          (current[index] ?? projectStack(project)).trim()
            ? current
            : { ...current, [index]: technologies.join(", ") },
        );
      setRepoChecks((current) => ({
        ...current,
        [index]: technologies.length
          ? { technologies, url: repo }
          : {
              error:
                "No languages or recognized framework manifests were found. Add the technologies you actually used.",
            },
      }));
    } catch (e) {
      setRepoChecks((current) => ({
        ...current,
        [index]: { error: e.message },
      }));
    }
  }
  async function sendOwnerCopy(stage, document, style) {
    if (!coachEdition || demo) return;
    try {
      await post(
        access.accessKind === "beta-pass"
          ? "/beta/resume-copy"
          : "/review/owner-copy",
        {
          stage,
          resume: {
            ...document,
            projects: formatProjectLinks(document.projects, projectFormat),
          },
          level: reviewLevel,
          ...style,
        },
      );
    } catch (e) {
      setError(`We could not send the ${stage} copy to PNB Paws: ${e.message}`);
    }
  }
  function chooseFinished(kind) {
    const document = kind === "template" && aiResume ? aiResume : resume;
    const style =
      kind === "template"
        ? { font: defaultResumeFont(reviewLevel), dates: "right", linkColor }
        : { font, dates, linkColor };
    setLayout(kind);
    go("complete");
    if (
      wantCoverLetter &&
      !coverLetter &&
      !coverBusy &&
      jobDescription.trim() &&
      (!demo || demoMode)
    ) {
      setCoverBusy(true);
      setCoverError("");
      post("/cover-letter", {
        resume: document,
        listing: jobDescription.trim(),
      })
        .then((result) => setCoverLetter(result.text))
        .catch((e) => setCoverError(e.message))
        .finally(() => setCoverBusy(false));
    }
    sendOwnerCopy("final", document, style);
    if (coachEdition && !betaEdition && !demo)
      post("/coach/history/final", {
        resume: {
          ...document,
          projects: formatProjectLinks(document.projects, projectFormat),
        },
        level: reviewLevel,
        ...style,
      }).catch((e) =>
        setError(
          `The finished resume could not be saved to Recent reviews: ${e.message}`,
        ),
      );
  }
  async function startBuilder() {
    setError("");
    if (!demo) {
      setBusy(true);
      try {
        await post("/builder/start", {});
      } catch (e) {
        setError(e.message);
        setBusy(false);
        return;
      }
      setBusy(false);
    }
    setFont(
      fonts.includes(review?.originalFont) ? review.originalFont : "Calibri",
    );
    setDates("right");
    go("preferences");
  }
  async function generateAiVersion(source = resume) {
    if (demo) {
      const example = normalizeResume(clone(source));
      example.summary =
        "Software engineering lead experienced in Java and Spring Boot services, reporting systems, and cross-team delivery. Works with TypeScript, React, and PostgreSQL, guiding engineers through technical decisions, testing, releases, monitoring, and documentation while supporting reliable software and practical mentoring.";
      if (demoMode) example.summary = demoSummary;
      setAiResume(example);
      setAiSource(
        JSON.stringify([
          source,
          jobDescription.trim(),
          targetRole.trim(),
          roleContext,
        ]),
      );
      setAiError("");
      return;
    }
    const requestId = ++aiRequestId.current;
    setAiBusy(true);
    setAiError("");
    try {
      const result = await post("/resume/alternative", {
        resume: source,
        level: reviewLevel,
        jobDescription: jobDescription.trim(),
        targetRole: targetRole.trim(),
        roleContext,
      });
      if (requestId !== aiRequestId.current) return;
      setAiResume(
        normalizeResume({
          ...result.resume,
          projects: preserveProjectHeadings(
            source.projects,
            result.resume.projects,
          ),
        }),
      );
      setAiSource(
        JSON.stringify([
          source,
          jobDescription.trim(),
          targetRole.trim(),
          roleContext,
        ]),
      );
    } catch (e) {
      if (requestId === aiRequestId.current) setAiError(e.message);
    } finally {
      if (requestId === aiRequestId.current) setAiBusy(false);
    }
  }
  async function importJobFile(file) {
    if (!file) return;
    if (file.size > 5_000_000) {
      setError("Choose a job posting file under 5 MB.");
      return;
    }
    setJobLoading(true);
    setError("");
    try {
      const body = new FormData();
      body.append("file", file);
      const result = await request("/job-description/file", {
        method: "POST",
        body,
      });
      setJobDescription(result.text);
      setJobPrep(null);
      setJobPrepError("");
      setMessage("Job description added. Review the text before building.");
    } catch (e) {
      setError(e.message);
    } finally {
      setJobLoading(false);
    }
  }
  async function importJobLink() {
    if (!jobLink.trim()) return;
    setJobLoading(true);
    setError("");
    try {
      const url = normalizeWebLink(jobLink);
      setJobLink(url);
      const result = await post("/job-description/link", { url });
      setJobDescription(result.text);
      setJobPrep(null);
      setJobPrepError("");
      setMessage("Job description added. Review the text before building.");
    } catch (e) {
      setError(e.message);
    } finally {
      setJobLoading(false);
    }
  }
  async function makeJobPrep(force = false) {
    const listing = jobDescription.trim();
    if (
      !listing ||
      (demo && !demoMode) ||
      jobPrepLoading ||
      (force !== true && jobPrep?.source === listing)
    )
      return;
    setJobPrepLoading(true);
    setJobPrepError("");
    try {
      const sheet = await post("/job-prep", {
        listing,
        targetRole: targetRole.trim(),
      });
      setJobPrep({ source: listing, sheet });
    } catch (e) {
      setJobPrepError(e.message);
    } finally {
      setJobPrepLoading(false);
    }
  }
  async function beginBuild(continueWithoutLinks = false) {
    if (jobLoading) {
      setError("Wait for the job posting to finish loading before building.");
      return;
    }
    setBusy(true);
    try {
      const found = !technicalOptions
        ? {}
        : demoMode
          ? Object.fromEntries(
              demoProjectLinks.map((links, i) => [i, links.split(" | ")]),
            )
          : demo
            ? {}
            : await discoverProjectLinks(
                projectOptions,
                normalizeWebLink(profileGithub),
                projectDemoUrl.split(/\r?\n/).map(normalizeWebLink).join("\n"),
              );
      if (normalizeWebLink(profileGithub) && !demo) {
        const checks = await Promise.allSettled(
          Object.entries(found).map(async ([index, links]) => {
            const repo = links.find((url) => githubRepository(url));
            return {
              index,
              technologies: repo ? await inspectGithubRepository(repo) : [],
            };
          }),
        );
        setProjectStacks((current) => {
          const next = { ...current };
          for (const check of checks)
            if (
              check.status === "fulfilled" &&
              check.value.technologies.length
            ) {
              const { index, technologies } = check.value;
              if (
                !(current[index] ?? projectStack(projectOptions[index])).trim()
              )
                next[index] = technologies.join(", ");
            }
          return next;
        });
      }
      setProjectLinks((current) => {
        const next = { ...current };
        for (const [index, links] of Object.entries(found)) {
          const existing =
            current[index] ??
            projectLinkUrls(projectOptions[index]).join(" | ");
          next[index] = mergeProjectLinkAutofill(existing, links);
        }
        return next;
      });
      setLinkDiscoveryNote(
        Object.keys(found).length
          ? demoMode
            ? "Demo: simulated repo and demo links filled for all three projects. No live lookup was performed."
            : "Matching project links filled in. You can edit them in your resume."
          : "No additional project links were matched. You can add them in the editor or leave them blank.",
      );
    } catch {
      setLinkDiscoveryNote(
        "Public project links could not be checked. You can add them in the editor or leave them blank.",
      );
    } finally {
      setBusy(false);
    }
    setLinkWarning([]);
    if (jobDescription.trim()) makeJobPrep();
    if (demoMode) setAnswers(current => ({ improve_certifications: "Sample certification: AWS Certified Cloud Practitioner — Amazon Web Services. In progress; exam planned for December 2026.", ...current }));
    go("questions");
  }
  async function autofillProjectLinks() {
    if (demoMode) {
      setProjectLinks(
        Object.fromEntries(demoProjectLinks.map((links, i) => [i, links])),
      );
      setLinkDiscoveryNote(
        "Demo: all three project repo and demo links filled with simulated examples. No GitHub or LinkedIn lookup was performed.",
      );
      return;
    }
    setBusy(true);
    setLinkDiscoveryNote("");
    try {
      const found = await discoverProjectLinks(
        projectOptions,
        normalizeWebLink(
          profileGithub || review?.resume?.contact?.github || "",
        ),
        projectDemoUrl.split(/\r?\n/).map(normalizeWebLink).join("\n"),
      );
      setProjectLinks((current) => {
        const next = { ...current };
        for (const [index, links] of Object.entries(found)) {
          const existing =
            current[index] ??
            projectLinkUrls(projectOptions[index]).join(" | ");
          const existingUrls = parseProjectLinks(existing);
          const isRepo = (url) =>
            /(?:github\.com|gitlab\.com|bitbucket\.org)\//i.test(url);
          const additions = links.filter(
            (url) => !existingUrls.some((old) => isRepo(old) === isRepo(url)),
          );
          next[index] = [...new Set([...existingUrls, ...additions])].join(
            " | ",
          );
        }
        return next;
      });
      setLinkDiscoveryNote(
        Object.keys(found).length
          ? "Matching public links filled in. Review them below; both fields remain optional."
          : "No matching public project links found. You can add them yourself or leave them blank.",
      );
    } catch {
      setLinkDiscoveryNote(
        "GitHub links could not be checked. You can continue with both fields blank.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function downloadPdf() {
    if (!layout) return;
    setBusy(true);
    setError("");
    try {
      const blob = await request("/resume/pdf", {
        method: "POST",
        body: JSON.stringify({
          resume: {
            ...selectedResume,
            projects: formatProjectLinks(
              selectedResume.projects,
              selectedProjectFormat,
            ),
          },
          level: reviewLevel,
          ...selectedStyle,
        }),
      });
      await saveResumeDownload(
        blob,
        resumeFilename(selectedResume.contact?.name, "pdf"),
      );
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function downloadWord() {
    if (!layout) return;
    setBusy(true);
    setError("");
    try {
      const blob = await request("/resume/docx", {
        method: "POST",
        body: JSON.stringify({
          resume: {
            ...selectedResume,
            projects: formatProjectLinks(
              selectedResume.projects,
              selectedProjectFormat,
            ),
          },
          level: reviewLevel,
          ...selectedStyle,
        }),
      });
      await saveResumeDownload(
        blob,
        resumeFilename(selectedResume.contact?.name, "docx"),
      );
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  function openDelivery(kind) {
    if (!layout) return;
    setEmailTo(
      coachEdition && !betaEdition
        ? email
        : access.email || email || resume.contact.email,
    );
    setDeliveryConsent(false);
    setError("");
    setMessage("");
    setModal(kind);
  }
  async function deliver(kind) {
    if (!layout) return;
    setBusy(true);
    setError("");
    try {
      await post("/delivery", {
        resume: {
          ...selectedResume,
          projects: formatProjectLinks(
            selectedResume.projects,
            selectedProjectFormat,
          ),
        },
        level: reviewLevel,
        ...selectedStyle,
        email: emailTo,
        kind: kind === "human" && betaEdition ? "beta-human" : kind,
        consent: deliveryConsent,
      });
      setMessage(
        kind === "human"
          ? "Your review request has been sent. We’ll reply by email."
          : "Your resume has been sent. Please check your inbox.",
      );
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  useEffect(() => {
    let active = true;
    setSavedProgress(null);
    setProgressMessage("");
    setProgressError("");
    if (access.allowed)
      request("/workflow-draft")
        .then((info) => {
          if (active) setSavedProgress(info);
        })
        .catch(() => {});
    return () => {
      active = false;
    };
  }, [access.allowed, access.email]);
  const workflowSnapshot = JSON.stringify({ createDetails,
    version: 1,
    page,
    email,
    level,
    profession,
    codingChoice,
    consent,
    review,
    resume,
    preferences,
    targetRole,
    generalFocus,
    otherGeneralFocus,
    generalService,
    generalProblemSolving,
    generalPracticalSkills,
    generalVolunteerWork,
    medicalRoleType,
    medicalCredentials,
    medicalSystems,
    medicalExperience,
    jobDescription,
    jobLink,
    jobPrep,
    selectedExperience,
    bulletChoices,
    roleDetails, newExperience, newProjects,
    selectedProjects,
    projectStacks,
    projectLinks,
    font,
    linkColor,
    projectFormat,
    dates,
    layout,
    aiResume,
    aiSource,
    rewrite,
    otherRewrite,
    otherPriority,
    answers,
    profileLinkedin,
    profileGithub,
    projectDemoUrl,
    linkDiscoveryNote,
    profilePortfolio,
    proposal,
    proposalApproved,
    coverLetter,
    wantCoverLetter,
    betaRated,
  });
  const savingWorkflow = useRef(false);
  const latestWorkflow = useRef(null);
  latestWorkflow.current =
    !demo &&
    access.allowed &&
    [
      "create-resume", "home",
      "upload-preview",
      "format",
      "overall",
      "invitation",
      "preferences",
      "questions",
      "approval",
      "edit",
      "layout",
      "complete",
      "experience-feedback",
    ].includes(page)
      ? { snapshot: workflowSnapshot, file }
      : null;
  const lastSavedWorkflow = useRef({ snapshot: "", file: null });
  const lastFailedWorkflow = useRef({ snapshot: "", file: null });
  async function saveWorkflowProgress(automatic = false) {
    if (savingWorkflow.current) return;
    savingWorkflow.current = true;
    setProgressBusy(true);
    setProgressMessage("");
    setProgressError("");
    let attempted = latestWorkflow.current;
    try {
      await saveLatestWorkflow(
        () => latestWorkflow.current,
        async (current) => {
          attempted = current;
          const body = new FormData();
          body.append("snapshot", current.snapshot);
          if (current.file) body.append("file", current.file);
          return request("/workflow-draft", { method: "POST", body });
        },
        (current, info) => {
          lastSavedWorkflow.current = current;
          lastFailedWorkflow.current = { snapshot: "", file: null };
          setSavedProgress(info);
        },
      );
      if (!automatic) setProgressMessage("Saved");
    } catch (error) {
      lastFailedWorkflow.current = attempted || {
        snapshot: workflowSnapshot,
        file,
      };
      setProgressError(error.message);
    } finally {
      savingWorkflow.current = false;
      setProgressBusy(false);
    }
  }
  useEffect(() => {
    if (
      lastSavedWorkflow.current.snapshot === workflowSnapshot &&
      lastSavedWorkflow.current.file === file
    )
      return;
    if (
      lastFailedWorkflow.current.snapshot === workflowSnapshot &&
      lastFailedWorkflow.current.file === file
    )
      return;
    if (
      demo ||
      !access.allowed ||
      busy ||
      aiBusy ||
      progressBusy ||
      ![
        "upload-preview",
        "format",
        "overall",
        "invitation",
        "preferences",
        "questions",
        "approval",
        "edit",
        "layout",
        "complete",
        "experience-feedback",
      ].includes(page) ||
      (!file && !review)
    )
      return;
    const timer = setTimeout(() => {
      void saveWorkflowProgress(true);
    }, 1500);
    return () => clearTimeout(timer);
  }, [workflowSnapshot, file, access.allowed, busy, aiBusy, progressBusy]);
  async function resumeWorkflowProgress(historyId = "") {
    setProgressBusy(true);
    setProgressMessage("");
    setProgressError("");
    try {
      const result = await post(
        historyId
          ? `/workflow-draft/review/${encodeURIComponent(historyId)}/resume`
          : "/workflow-draft/resume",
        {},
      );
      const saved = {
        codingChoice: null,
        consent: false,
        preferences: [],
        targetRole: "",
        generalFocus: "",
        otherGeneralFocus: "",
        generalService: "",
        generalProblemSolving: "",
        generalPracticalSkills: "",
        generalVolunteerWork: "",
        medicalRoleType: "",
        medicalCredentials: "",
        medicalSystems: "",
        medicalExperience: "",
        jobDescription: "",
        jobLink: "",
        jobPrep: null,
        selectedExperience: null,
        bulletChoices: {},
        roleDetails: {},
        selectedProjects: null,
        projectStacks: {},
        projectLinks: {},
        font: defaultResumeFont(result.snapshot?.profession),
        linkColor: "blue",
        projectFormat: "compact",
        dates: "right",
        layout: null,
        aiResume: null,
        aiSource: null,
        rewrite: rewriteOptions[0],
        otherRewrite: "",
        otherPriority: "",
        answers: {},
        profileLinkedin:
          result.snapshot?.review?.resume?.contact?.linkedin || "",
        profileGithub: result.snapshot?.review?.resume?.contact?.github || "",
        projectDemoUrl: "",
        linkDiscoveryNote: "",
        profilePortfolio:
          result.snapshot?.review?.resume?.contact?.portfolio || "",
        proposal: null,
        proposalApproved: false,
        coverLetter: "",
        wantCoverLetter: false,
        betaRated: false,
        ...result.snapshot,
      };
      if (saved?.version !== 1)
        throw Error("This saved draft could not be opened.");
      restoringWorkflow.current = true;
      if (Object.hasOwn(saved, "email")) setEmail(saved.email);
      if (Object.hasOwn(saved, "level")) setLevel(saved.level);
      if (Object.hasOwn(saved, "profession")) setProfession(saved.profession);
      if (Object.hasOwn(saved, "codingChoice"))
        setCodingChoice(saved.codingChoice);
      if (Object.hasOwn(saved, "consent")) setConsent(saved.consent);
      if (Object.hasOwn(saved, "review")) setReview(saved.review);
      if (Object.hasOwn(saved, "resume")) setResume(saved.resume);
      if (Object.hasOwn(saved, "preferences"))
        setPreferences(saved.preferences);
      if (Object.hasOwn(saved, "targetRole")) setTargetRole(saved.targetRole);
      if (Object.hasOwn(saved, "generalFocus"))
        setGeneralFocus(saved.generalFocus);
      if (Object.hasOwn(saved, "otherGeneralFocus"))
        setOtherGeneralFocus(saved.otherGeneralFocus);
      if (Object.hasOwn(saved, "generalService"))
        setGeneralService(saved.generalService);
      if (Object.hasOwn(saved, "generalProblemSolving"))
        setGeneralProblemSolving(saved.generalProblemSolving);
      if (Object.hasOwn(saved, "generalPracticalSkills"))
        setGeneralPracticalSkills(saved.generalPracticalSkills);
      if (Object.hasOwn(saved, "generalVolunteerWork"))
        setGeneralVolunteerWork(saved.generalVolunteerWork);
      if (Object.hasOwn(saved, "medicalRoleType"))
        setMedicalRoleType(saved.medicalRoleType);
      if (Object.hasOwn(saved, "medicalCredentials"))
        setMedicalCredentials(saved.medicalCredentials);
      if (Object.hasOwn(saved, "medicalSystems"))
        setMedicalSystems(saved.medicalSystems);
      if (Object.hasOwn(saved, "medicalExperience"))
        setMedicalExperience(saved.medicalExperience);
      if (Object.hasOwn(saved, "jobDescription"))
        setJobDescription(saved.jobDescription);
      if (Object.hasOwn(saved, "jobLink")) setJobLink(saved.jobLink);
      if (Object.hasOwn(saved, "jobPrep")) setJobPrep(saved.jobPrep);
      if (Object.hasOwn(saved, "selectedExperience"))
        setSelectedExperience(saved.selectedExperience);
      if (Object.hasOwn(saved, "bulletChoices"))
        setBulletChoices(saved.bulletChoices);
      setNewExperience(saved.newExperience || {});
      setNewProjects(saved.newProjects || []);
      if (Object.hasOwn(saved, "roleDetails"))
        setRoleDetails(saved.roleDetails);
      if (Object.hasOwn(saved, "selectedProjects"))
        setSelectedProjects(saved.selectedProjects);
      if (Object.hasOwn(saved, "projectStacks"))
        setProjectStacks(saved.projectStacks);
      if (Object.hasOwn(saved, "projectLinks"))
        setProjectLinks(saved.projectLinks);
      if (Object.hasOwn(saved, "font")) setFont(saved.font);
      if (Object.hasOwn(saved, "linkColor")) setLinkColor(saved.linkColor);
      if (Object.hasOwn(saved, "projectFormat"))
        setProjectFormat(saved.projectFormat);
      if (Object.hasOwn(saved, "dates")) setDates(saved.dates);
      if (Object.hasOwn(saved, "layout")) setLayout(saved.layout);
      if (Object.hasOwn(saved, "aiResume")) setAiResume(saved.aiResume);
      if (Object.hasOwn(saved, "aiSource")) setAiSource(saved.aiSource);
      if (Object.hasOwn(saved, "rewrite")) setRewrite(saved.rewrite);
      if (Object.hasOwn(saved, "otherRewrite"))
        setOtherRewrite(saved.otherRewrite);
      if (Object.hasOwn(saved, "otherPriority"))
        setOtherPriority(saved.otherPriority);
      if (Object.hasOwn(saved, "answers")) setAnswers(saved.answers);
      if (Object.hasOwn(saved, "profileLinkedin"))
        setProfileLinkedin(saved.profileLinkedin);
      if (Object.hasOwn(saved, "profileGithub"))
        setProfileGithub(saved.profileGithub);
      if (Object.hasOwn(saved, "projectDemoUrl"))
        setProjectDemoUrl(saved.projectDemoUrl);
      if (Object.hasOwn(saved, "linkDiscoveryNote"))
        setLinkDiscoveryNote(saved.linkDiscoveryNote);
      if (Object.hasOwn(saved, "profilePortfolio"))
        setProfilePortfolio(saved.profilePortfolio);
      if (Object.hasOwn(saved, "proposal")) setProposal(saved.proposal);
      if (Object.hasOwn(saved, "proposalApproved"))
        setProposalApproved(saved.proposalApproved);
      if (Object.hasOwn(saved, "coverLetter"))
        setCoverLetter(saved.coverLetter);
      if (Object.hasOwn(saved, "wantCoverLetter"))
        setWantCoverLetter(saved.wantCoverLetter);
      if (Object.hasOwn(saved, "betaRated")) setBetaRated(saved.betaRated);
      setFile(
        result.file
          ? new File(
              [
                Uint8Array.from(atob(result.file.base64), (char) =>
                  char.charCodeAt(0),
                ),
              ],
              result.file.name,
              { type: result.file.type },
            )
          : null,
      );
      setError("");
      setEditSection(null);
      setModal(null);
      setCreateDetails(saved.createDetails || blankResumeDetails());
      go(saved.page === "upload-preview" && !result.file ? "home" : saved.page);
      setProgressMessage("Your saved progress is ready.");
    } catch (error) {
      setProgressError(error.message);
    } finally {
      setProgressBusy(false);
    }
  }
  const workflowProgressControls =
    !demo &&
    access.allowed &&
    !["grading", "admin", "forgot", "reset"].includes(page) ? (
      <div className="workflow-progress" aria-label="Save resume progress">
        <Button
          secondary
          title={progressError || undefined}
          aria-label={
            progressError
              ? `Save failed: ${progressError}. Try again.`
              : undefined
          }
          disabled={
            progressBusy ||
            busy ||
            aiBusy ||
            jobPrepLoading ||
            coverBusy ||
            Boolean(editSection)
          }
          type="button"
          onClick={() => saveWorkflowProgress()}
        >
          {progressBusy
            ? "Saving…"
            : progressError
              ? "Save failed · Retry"
              : progressMessage
                ? "Saved"
                : "Save progress"}
        </Button>
      </div>
    ) : null;
  const stage = [
    "home",
    "grading",
    "format",
    "overall",
    "invitation",
    "preferences",
    "questions",
    "edit",
    "complete",
  ].indexOf(
    page === "layout" ? "complete" : page === "approval" ? "edit" : page,
  );
  const featuresControl = showFeatures({
    publicEdition: coachEdition,
    hostname: window.location.hostname,
    role: access.role,
    email: access.email,
  }) ? (
    <Features individual={betaEdition} />
  ) : null;
  if (coachEdition && !access.allowed)
    return (
      <>
        <Watercolor />
        {desktopPreview && (
          <div className="desktop-preview-label">
            Desktop preview · Local service connection · Not ready for coach
            distribution
          </div>
        )}
        <CoachSignIn
          access={access}
          refresh={refreshAccess}
          onHowItWorks={() => setModal("how")}
          onAbout={() => setModal("about")}
          features={featuresControl}
        />
        {modal === "how" && <HowItWorksDialog onClose={() => setModal(null)} />}
        {modal === "about" && <AboutDialog onClose={() => setModal(null)} />}
      </>
    );
  if (page === "upload-preview" && file && access.allowed)
    return (
      <UploadedResumePreview
        features={featuresControl}
        progressControls={workflowProgressControls}
        file={file}
        sampleResume={demoMode ? demoResume : null}
        onBack={() => go("home")}
        onReview={() => analyze()}
      />
    );
  return (
    <>
      <Watercolor />
      {desktopPreview && (
        <div className="desktop-preview-label">
          Desktop preview · Local service connection · Not ready for coach
          distribution
        </div>
      )}
      <div className="app-shell">

        <SubscriptionRequest
          individual={betaEdition}
          access={access}
          refresh={refreshAccess}
          onRestore={async () => {
            try {
              const saved = await post("/accounts/restore", {});
              setReview(saved.review);
              setResume(clone(saved.build?.resume || saved.review.resume));
              setEmail(saved.email);
              setLevel(saved.level);
              setProfession(
                ["General", "Medical Field", "Educator"].includes(saved.level)
                  ? saved.level
                  : "Technical",
              );
              setProposal(saved.build || null);
              setAnswers({}); setNewExperience({}); setNewProjects([]);
              setProjectLinks({});
              setProjectStacks({});
              setLayout(null);
              setAiResume(null);
              go(
                saved.build
                  ? saved.build.protectedChanges?.length ||
                    saved.build.approvalQuestions?.length
                    ? "approval"
                    : "edit"
                  : "format",
              );
            } catch (e) {
              setError(e.message);
            }
          }}
        />
        {demoMode && (
          <div className="sample-label demo-banner">
            <span>
              Interactive demo · Sample data · No emails or live AI requests
            </span>
            <a className="demo-exit-button" href="/">
              Exit demo
            </a>
          </div>
        )}
        <nav className="topnav">
          <AboutButton onClick={() => setModal("about")} />
          <button
            className="text-button home-link"
            onClick={() => go("home")}
            aria-label="Resume Prospective home"
          >
            Home
          </button>
          <HowButton onClick={() => setModal("how")} />
        </nav>
        {featuresControl}
        {stage > 0 && page !== "grading" && (
          <div className="journey" aria-label="Your progress">
            {[
              "Upload",
              "Your reviews",
              "Your preferences",
              "Build together",
              "Your resume",
            ].map((s, i) => {
              const current =
                stage <= 3 ? 1 : stage <= 6 ? 2 : stage === 7 ? 3 : 4;
              return (
                <React.Fragment key={s}>
                  <span
                    className={
                      i === current ? "current" : i < current ? "done" : ""
                    }
                  >
                    {i < current ? <Check size={13} /> : <b>{i + 1}</b>}
                    <span>{s}</span>
                  </span>
                  {i < 4 && <i />}
                </React.Fragment>
              );
            })}
          </div>
        )}
        {demo && stage >= 2 && (
          <div className="sample-label">
            <Eye size={14} />
            {demoMode
              ? "Alex Rivera sample · illustrative scores · GitHub, LinkedIn, repo and demo links are simulated"
              : "Jordan Avery sample · illustrative scores · project links are fictional examples"}
          </div>
        )}
        <main className={`page-${page}`}>
          {addedRepoSkills.length > 0 &&
            ["approval", "edit", "compare", "complete"].includes(page) && (
              <div className="notice" role="status">
                Added to your Skills section from your resume and project
                evidence: <strong>{addedRepoSkills.join(", ")}</strong>. You can
                edit these in the Skills section.
              </div>
            )}
          {workflowProgressControls}
          {hostedDemo &&
            !betaEdition &&
            ["preferences", "questions", "approval", "edit"].includes(page) && (
              <Notice>
                Work together with your student, client, or mentee to build
                their resume.
              </Notice>
            )}
          {access.role === "coach" && (
            <div className="coach-session">
              <span>
                {access.accessKind === "beta-pass" || betaEdition
                  ? "Your account"
                  : "Coach and Mentor access"}
                {accessDurationLabel(access) &&
                  ` · ${accessDurationLabel(access)}`}
              </span>
              <button
                className="text-button"
                onClick={async () => {
                  await post("/coach/logout", {});
                  await refreshAccess();
                  go("home");
                }}
              >
                Sign Out
              </button>
            </div>
          )}
          {["preferences", "questions", "edit", "complete"].includes(page) && (
            <aside className="level-guide" aria-label="Selected career level">
              <div className="icon-orb">
                <ShieldCheck size={23} />
              </div>
              <div>
                <strong>Building your {guideline.name} resume</strong>
                <p>{guideline.focus}</p>
              </div>
            </aside>
          )}
          {page === "create-resume" && <CreateResume value={createDetails} onChange={setCreateDetails} onContinue={createFromScratch} onBack={() => go("home")} busy={busy} error={error} />}
          {page === "home" && (
            <>
              <header className="hero">
                <h1>Resume Prospective</h1>
                <p>
                  <em>Ai Resume Rater &amp; Assistant Builder</em>
                </p>
                <Divider />
              </header>
              {demoMode && <section className="demo-entry-paths" aria-label="Choose a demo path">
                <div className="demo-path-heading"><h2>Two ways to start your resume</h2><p>Try either path with sample information. No upload, email, or paid AI request is needed.</p></div>
                <article><h3>Already have a resume?</h3><p><strong>Preview My Resume</strong> takes you to a document preview, then ratings and feedback before you improve it.</p><p>To try this path, select Preview My Resume below.</p></article>
                <article><h3>Starting from scratch?</h3><p><strong>Create a resume</strong> starts with your profession, then experience, education, skills, and languages.</p><p>To try this path, select Create a resume below.</p></article>
                <p className="demo-path-footer">Both paths lead to editing and choosing your finished resume.</p>
              </section>}
              {access.role === "coach" &&
                access.accessKind !== "beta-pass" &&
                !betaEdition && (
                  <div className="coach-history-buttons">
                    <CoachSavedReviews
                      onOpen={(data) => {
                        setReview(data.review);
                        setResume(clone(data.review.resume));
                        setEmail(data.reviewUserEmail);
                        setLevel(data.reviewedLevel);
                        setAnswers({}); setNewExperience({}); setNewProjects([]);
                        setProjectLinks({});
                        setProjectStacks({});
                        setRoleDetails({});
                        setBulletChoices({});
                        setSelectedExperience(null);
                        setSelectedProjects(null);
                        setPreferences([]);
                        setProjectDemoUrl("");
                        setLinkDiscoveryNote("");
                        setTargetRole(demoMode ? demoJobTitle : "");
                        setProposal(null);
                        setLayout("");
                        setAiResume(null);
                        setCoverLetter("");
                        setProfileGithub(
                          data.review.resume?.contact?.github || "",
                        );
                        setProfileLinkedin(
                          data.review.resume?.contact?.linkedin || "",
                        );
                        setProfilePortfolio(
                          data.review.resume?.contact?.portfolio || "",
                        );
                        setCodingChoice(null);
                        setJobDescription(demoMode ? demoJobDescription : "");
                        setJobLink(demoMode ? demoJobUrl : "");
                        setJobPrep(null);
                        go("invitation");
                      }}
                    />
                    <CoachRecentReviews onResume={resumeWorkflowProgress} />
                  </div>
                )}
              <form
                className="upload-card card"
                onSubmit={(e) => {
                  e.preventDefault();
                  if (!file || !consent) {
                    setError(
                      "Add your resume file and confirm the processing notice.",
                    );
                    return;
                  }
                  go("upload-preview");
                }}
              >
                <div className="field">
                  <label htmlFor="email">
                    {coachEdition && !betaEdition
                      ? "Resume’s email"
                      : "Email address"}
                  </label>
                  <input
                    id="email"
                    type="email"
                    placeholder="you@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoComplete={
                      coachEdition && !betaEdition ? "off" : "email"
                    }
                  />
                </div>
                <div className="field">
                  <label htmlFor="profession">Profession?</label>
                  <select
                    id="profession"
                    value={profession}
                    required
                    onChange={(event) => {
                      setProfession(event.target.value);
                      setFont(defaultResumeFont(event.target.value));
                      setLevel("");
                    }}
                  >
                    <option value="" disabled>
                      Choose a profession
                    </option>
                    {["Technical", "General", "Medical Field", "Educator"].map(
                      (item) => (
                        <option key={item}>{item}</option>
                      ),
                    )}
                  </select>
                </div>
                {profession === "Technical" && (
                  <div className="field">
                    <label htmlFor="level">Level?</label>
                    <select
                      id="level"
                      value={level}
                      required
                      onChange={(e) => setLevel(e.target.value)}
                    >
                      <option value="" disabled>
                        Choose a level
                      </option>
                      {levels.map((l) => (
                        <option key={l}>{l}</option>
                      ))}
                    </select>
                  </div>
                )}
                <label className="field-label">Upload your resume</label>
                <button
                  type="button"
                  className={"dropzone " + (drag ? "drag" : "")}
                  onClick={() =>
                    demoMode
                      ? setMessage(
                          "The demo uses a sample resume. Exit the demo to upload your own.",
                        )
                      : inputRef.current.click()
                  }
                  onDragOver={(e) => {
                    e.preventDefault();
                    setDrag(true);
                  }}
                  onDragLeave={() => setDrag(false)}
                  onDrop={(e) => {
                    e.preventDefault();
                    setDrag(false);
                    selectFile(e.dataTransfer.files[0]);
                  }}
                >
                  <FileText size={40} />
                  <strong>
                    {file ? file.name : "Click to upload your resume"}
                  </strong>
                  <span>
                    {file
                      ? demoMode
                        ? "Alex’s sample resume is loaded"
                        : "Click to choose another file"
                      : "or drag and drop your file here"}
                  </span>
                  <small>
                    PDF, Word (.docx or .doc), PNG, or JPG · up to 10 MB
                  </small>
                </button>
                <input
                  ref={inputRef}
                  className="visually-hidden"
                  type="file"
                  accept=".pdf,.docx,.doc,.png,.jpg,.jpeg,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/msword,image/png,image/jpeg"
                  onChange={(e) => selectFile(e.target.files[0])}
                />
                <label className="check-row consent">
                  <input
                    type="checkbox"
                    checked={consent}
                    onChange={(e) => setConsent(e.target.checked)}
                  />
                  <span>
                    Your resume data will be deleted unless you ask us to save it.
                  </span>
                </label>
                {error && <Notice error>{error}</Notice>}
                <Button type="submit">
                  Preview My Resume <ArrowRight size={18} />
                </Button>
                <button
                  className="text-button sample-link"
                  type="button"
                  onClick={() => analyze(true)}
                >
                  Just looking? Explore a sample review <ArrowRight size={14} />
                </button>
              </form>
              <section className="ratings-intro">
              <div className="create-resume-entry">
                <button className="coach-recent-toggle create-resume-button" type="button" onClick={() => { setError(""); setCreateDetails(current => demoMode && !current.first ? demoCreateDetails() : {...current, email: current.email || email}); go("create-resume"); }}><Folder size={18} aria-hidden="true" />Create a resume</button>
              </div>

                <h2>Two scores. One clear next step.</h2>
                <div className="rating-tiles">
                  <article className="rating-tile">
                    <div className="icon-orb">
                      <FileText />
                    </div>
                    <div>
                      <span className="eyebrow">RATING 01</span>
                      <h3>Format & Presentation</h3>
                      <p>Layout, readability, and key sections.</p>
                    </div>
                  </article>
                  <article className="rating-tile">
                    <div className="icon-orb">
                      <Monitor />
                    </div>
                    <div>
                      <span className="eyebrow">RATING 02</span>
                      <h3>Overall Resume</h3>
                      <p>Your experience and fit for your career level.</p>
                    </div>
                  </article>
                </div>
              </section>

            </>
          )}
          {page === "grading" && (
            <section className="grading card">
              <div className="loading-orb">
                <FileText size={42} />
                <span />
              </div>
              <div className="eyebrow">A THOUGHTFUL REVIEW TAKES A MOMENT</div>
              <h1>Getting to know your resume</h1>
              <p>
                We’re looking at the presentation and the experience behind it.
              </p>
              <div className="review-progress">
                <div className="review-progress-label">
                  <span>
                    {reviewProgress === 100
                      ? "Your review is ready!"
                      : "Estimated progress"}
                  </span>
                  <strong>{reviewProgress}%</strong>
                </div>
                <div
                  className="review-progress-track"
                  role="progressbar"
                  aria-label="Estimated resume review progress"
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={reviewProgress}
                  aria-valuetext={`${reviewProgress}% estimated`}
                >
                  <div
                    className="review-progress-fill"
                    style={{ width: `${reviewProgress}%` }}
                  >
                    <FileText size={22} aria-hidden="true" />
                  </div>
                </div>
                <p className="review-progress-caption" role="status">
                  {reviewProgress === 100
                    ? "Opening your scores…"
                    : reviewSeconds >= 90
                      ? "Still waiting for your review. Your file is being processed—no need to submit again."
                      : "Checking your resume’s presentation and experience."}
                </p>
              </div>
              <small>
                {demo ? "" : file?.name} · {level}
              </small>
              {error && <Notice error>{error}</Notice>}
            </section>
          )}
          {["format", "overall"].includes(page) && review && (
            <>
              <PageTitle
                eyebrow={
                  page === "format" ? "YOUR FIRST REVIEW" : "YOUR SECOND REVIEW"
                }
                title={
                  page === "format"
                    ? "A closer look at your presentation"
                    : "Your experience, in perspective"
                }
                subtitle={
                  page === "format"
                    ? "Your layout and readability."
                    : `Your ${reviewLevel} resume review.`
                }
              />
              <div className="review-controls">
                {!demo && (
                  <div className="feedback-email-toolbar">
                    <Button
                      secondary
                      onClick={emailReviewFeedback}
                      disabled={Boolean(feedbackEmail.loading)}
                    >
                      {feedbackEmail.loading ? "Sending…" : "Email me this"}
                    </Button>
                    {feedbackEmail.sent && (
                      <span role="status">
                        Feedback PDF sent to your email.
                      </span>
                    )}
                    {feedbackEmail.error && (
                      <span role="alert">{feedbackEmail.error}</span>
                    )}
                  </div>
                )}
                {review.scoringNote && (
                  <details className="score-policy">
                    <summary>How scores work</summary>
                    <p>{review.scoringNote}</p>
                  </details>
                )}
                <div className="feedback-actions-top">
                  <button
                    type="button"
                    className="roast-image-button positive-feedback-button"
                    disabled={Boolean(positiveFeedback[page]?.loading)}
                    aria-label={
                      positiveFeedback[page]?.open
                        ? "Hide positive details"
                        : "More positive details"
                    }
                    aria-expanded={Boolean(positiveFeedback[page]?.open)}
                    onClick={() => togglePositiveFeedback(page)}
                  >
                    <img
                      src="/positive-feedback-banner.png"
                      alt="Positive Feedback"
                    />
                  </button>
                  {page === "overall" && (
                    <button
                      type="button"
                      className="roast-image-button"
                      disabled={Boolean(roastFeedback[page]?.loading)}
                      aria-label={
                        roastFeedback[page]?.open
                          ? "Hide roast"
                          : "Roast my resume"
                      }
                      aria-expanded={Boolean(roastFeedback[page]?.open)}
                      onClick={() => toggleRoastFeedback(page)}
                    >
                      <img src="/roast-me-transparent.webp" alt="Roast Me" />
                    </button>
                  )}
                </div>
              </div>
              <div className="positive-feedback-top">
                {positiveFeedback[page]?.loading && (
                  <p role="status">Writing positive feedback…</p>
                )}
                {positiveFeedback[page]?.open && (
                  <div className="feedback-details" role="status">
                    <p>
                      {positiveFeedback[page]?.error ||
                        positiveFeedback[page]?.text ||
                        "Finding what worked well…"}
                    </p>
                  </div>
                )}
                {roastFeedback[page]?.loading && (
                  <p role="status">Writing your roast…</p>
                )}
                {roastFeedback[page]?.open && (
                  <div className="feedback-details" role="status">
                    <p style={{ whiteSpace: "pre-line" }}>
                      {roastFeedback[page]?.error ||
                        roastFeedback[page]?.text ||
                        "Finding the funny side…"}
                    </p>
                  </div>
                )}
              </div>
              <div className="results-layout">
                <aside className="score-card card">
                  <span className="eyebrow">
                    {page === "format"
                      ? "FORMAT & PRESENTATION"
                      : "OVERALL RESUME"}
                  </span>
                  <p className="rating-scale">Rating scale: 0–10</p>
                  <div className="score-ring">
                    <strong>
                      {(page === "format"
                        ? review.formatRating
                        : review.overallRating
                      )?.value ??
                        (page === "format"
                          ? review.formatScore
                          : review.overallScore) ??
                        "—"}
                    </strong>
                  </div>
                  {!!(
                    page === "format"
                      ? review.formatRating
                      : review.overallRating
                  )?.unverified && (
                    <p className="range-explanation">
                      Provisional score. Checks awaiting verification:{" "}
                      {
                        (page === "format"
                          ? review.formatRating
                          : review.overallRating
                        ).unverified
                      }{" "}
                      . These checks have not earned points yet.
                    </p>
                  )}
                  <h3>
                    {page === "format"
                      ? "Your presentation matters."
                      : "Your next step is in reach."}
                  </h3>
                  <p>
                    {page === "format"
                      ? "This score focuses on appearance and organization."
                      : "This score includes your format score and evidence for your selected career level."}
                  </p>
                  {page === "overall" && (
                    <button
                      className="text-button"
                      onClick={() => go("format")}
                    >
                      Revisit format feedback <ArrowLeft size={14} />
                    </button>
                  )}
                  <div className="score-note">
                    <ShieldCheck size={17} />
                    <span>
                      Evaluated against our rubric.
                      <br />
                      Similar responsibilities count.
                    </span>
                  </div>
                </aside>
                <section className="findings">
                  {visibleReviewFindings(page === "format" ? review.formatFindings : review.contentFindings, demoMode)
                    .map((f, i) => (
                      <article key={i} className={"finding " + f.status}>
                        <div className="finding-icon">
                          {f.status === "pass" ? (
                            <CheckCircle2 />
                          ) : f.status === "unverified" ? (
                            <Info />
                          ) : (
                            <PenLine />
                          )}
                        </div>
                        <div>
                          <span className="eyebrow">
                            {f.status === "pass"
                              ? "WHAT’S WORKING"
                              : f.status === "unverified"
                                ? "NEEDS VERIFICATION"
                                : "ROOM TO IMPROVE"}
                          </span>
                          <h3>
                            {f.title}{" "}
                            {f.points && (
                              <span className="finding-points">{f.points}</span>
                            )}
                          </h3>
                          <p>{briefFeedback(f.evidence)}</p>
                          {f.suggestion && <p><strong>Next step:</strong> {briefNextStep(f.suggestion)}</p>}
                          {f.requirement && (
                            <details className="finding-requirement">
                              <summary>What this check looks for</summary>
                              <p>{f.requirement}</p>
                            </details>
                          )}
                          <button
                            type="button"
                            className="feedback-more"
                            aria-expanded={Boolean(
                              feedbackDetails[`${page}:${f.id}`]?.open,
                            )}
                            aria-controls={`feedback-${page}-${i}`}
                            disabled={Boolean(
                              feedbackDetails[`${page}:${f.id}`]?.loading,
                            )}
                            onClick={() => toggleFeedbackDetails(f, page)}
                          >
                            {feedbackDetails[`${page}:${f.id}`]?.loading
                              ? "Writing details…"
                              : feedbackDetails[`${page}:${f.id}`]?.text &&
                                  feedbackDetails[`${page}:${f.id}`]?.open
                                ? "Hide details"
                                : "More details"}
                          </button>
                          {feedbackDetails[`${page}:${f.id}`]?.open && (
                            <div
                              id={`feedback-${page}-${i}`}
                              className="feedback-details"
                              role="status"
                            >
                              {feedbackDetails[`${page}:${f.id}`]?.error ? (
                                <p>
                                  {feedbackDetails[`${page}:${f.id}`].error}{" "}
                                  Press “More details” to try again.
                                </p>
                              ) : feedbackDetails[`${page}:${f.id}`]?.text ? (
                                <p>{feedbackDetails[`${page}:${f.id}`].text}</p>
                              ) : (
                                <p>Adding detail to this finding…</p>
                              )}
                            </div>
                          )}
                        </div>
                      </article>
                    ))}
                  {visibleReviewFindings(page === "format" ? review.formatFindings : review.contentFindings).length === 0 && (<p role="status">Feedback for this rating is unavailable in this saved review. Start a new review to receive a complete assessment.</p>)}
                </section>
              </div>
              <div className="page-actions">
                <Button
                  secondary
                  onClick={() => go(page === "format" ? "home" : "format")}
                >
                  <ArrowLeft size={16} />
                  Back
                </Button>
                <Button
                  onClick={() =>
                    go(page === "format" ? "overall" : "invitation")
                  }
                >
                  {page === "format"
                    ? "Continue to Overall Feedback"
                    : "See How We Can Help"}
                  <ArrowRight size={17} />
                </Button>
              </div>
            </>
          )}
          {page === "invitation" && (
            <>
              <PageTitle
                eyebrow="BUILD YOUR RESUME"
                title="Ready to build?"
                subtitle="Choose what to highlight, then review your draft."
              />
              <div className="invitation-layout">
                <div className="example-frame">
                  <div className="preview-tag">
                    <FileText size={15} />
                    {demoMode && technicalOptions
                      ? "Alex Rivera · Resume PDF"
                      : invitationExample.label}{" "}
                    <span>
                      {demoMode && technicalOptions
                        ? "PDF preview"
                        : "Illustrative example"}
                    </span>
                  </div>
                  {demoMode && technicalOptions ? (
                    <DemoResumePdfPreview resume={demoFinishedResume} />
                  ) : (
                    <ScaledResume
                      resume={invitationExample.resume}
                      level={invitationExample.level}
                    />
                  )}
                </div>
                <div className="invitation-copy">
                  <div className="invitation-actions">
                    <Button disabled={busy} onClick={startBuilder}>
                      Yes, Help Me Build My Resume
                      <ArrowRight size={17} />
                    </Button>
                    <Button
                      secondary
                      onClick={() =>
                        go(
                          coachEdition && !demo
                            ? "experience-feedback"
                            : "overall",
                        )
                      }
                    >
                      No, thank you
                    </Button>
                    {coachEdition &&
                      !betaEdition &&
                      !demo &&
                      access.role === "coach" && (
                        <Button
                          secondary
                          disabled={busy}
                          onClick={async () => {
                            setBusy(true);
                            setError("");
                            try {
                              await post("/coach/saved", {});
                              go("home");
                            } catch (e) {
                              setError(e.message);
                            } finally {
                              setBusy(false);
                            }
                          }}
                        >
                          Save me for later
                        </Button>
                      )}
                  </div>
                </div>
              </div>
            </>
          )}
          {page === "layout" && (
            <>
              <PageTitle
                eyebrow="YOUR COMPLETED RESUME"
                title="Compare your two versions"
                subtitle="Choose one to email or download."
              />
              <div
                className="layout-comparison"
                role="group"
                aria-label="Compare resume versions"
              >
                <section
                  className={`card layout-option ${layout === "original" ? "selected" : ""}`}
                >
                  <h2>Resume you built</h2>
                  {demoMode && (
                    <p className="rerate-result">
                      <strong>Example final rating: 9.2/10</strong>
                      <span>
                        Illustrative demo score, not a live re-review. Your
                        edits are not automatically rescored.
                      </span>
                    </p>
                  )}
                  <PdfPreview
                    resume={resume}
                    level={reviewLevel}
                    font={font}
                    dates={dates}
                    linkColor={linkColor}
                    projectFormat={projectFormat}
                  />
                  {!demo && (
                    <>
                      <Button
                        secondary
                        onClick={() => rerateVersion("original")}
                        disabled={Boolean(rerates.original?.loading)}
                      >
                        {rerates.original?.loading
                          ? "Rating…"
                          : "Re-rate this resume"}
                      </Button>
                      {rerates.original?.source === resume &&
                        (rerates.original.result || rerates.original.error) && (
                          <div className="rerate-result" role="status">
                            {rerates.original.error || (
                              <>
                                <strong>
                                  New overall:{" "}
                                  {rerates.original.result.overallRating
                                    ?.display ?? "—"}
                                  /10
                                </strong>
                                <span>
                                  Format:{" "}
                                  {rerates.original.result.formatRating
                                    ?.display ?? "—"}
                                  /10
                                </span>
                              </>
                            )}
                          </div>
                        )}
                    </>
                  )}
                  <Button onClick={() => chooseFinished("original")}>
                    Choose the resume you built <ArrowRight size={17} />
                  </Button>
                </section>
                <section
                  className={`card layout-option ${layout === "template" ? "selected" : ""}`}
                >
                  <h2>PNB Paws version</h2>
                  {aiResume?.rewriteNotice && <Notice>{aiResume.rewriteNotice}</Notice>}
                  {aiResume?.rewriteNotice && !aiBusy && <Button secondary onClick={() => generateAiVersion(resume)}>Try PNB Paws wording again</Button>}
                  {aiResume ? (
                    <PdfPreview
                      resume={aiResume}
                      level={reviewLevel}
                      font={defaultResumeFont(reviewLevel)}
                      dates="right"
                      linkColor={linkColor}
                      projectFormat={demoMode ? "compact" : projectFormat}
                    />
                  ) : aiBusy ? (
                    <div className="notice" role="status">
                      <LoaderCircle className="spin" size={18} /> Preparing the
                      PNB Paws version…
                    </div>
                  ) : (
                    <Notice error={Boolean(aiError)}>
                      {aiError || "The PNB Paws version is not ready yet."}
                    </Notice>
                  )}
                  {!aiResume && !aiBusy && (
                    <Button secondary onClick={() => generateAiVersion(resume)}>
                      Try PNB Paws wording again
                    </Button>
                  )}
                  {!demo && aiResume && (
                    <>
                      <Button
                        secondary
                        onClick={() => rerateVersion("template")}
                        disabled={Boolean(rerates.template?.loading)}
                      >
                        {rerates.template?.loading
                          ? "Rating…"
                          : "Re-rate this resume"}
                      </Button>
                      {rerates.template?.source === aiResume &&
                        (rerates.template.result || rerates.template.error) && (
                          <div className="rerate-result" role="status">
                            {rerates.template.error || (
                              <>
                                <strong>
                                  New overall:{" "}
                                  {rerates.template.result.overallRating
                                    ?.display ?? "—"}
                                  /10
                                </strong>
                                <span>
                                  Format:{" "}
                                  {rerates.template.result.formatRating
                                    ?.display ?? "—"}
                                  /10
                                </span>
                              </>
                            )}
                          </div>
                        )}
                    </>
                  )}
                  <Button
                    disabled={!aiResume || aiBusy}
                    onClick={() => chooseFinished("template")}
                  >
                    Choose PNB Paws version <ArrowRight size={17} />
                  </Button>
                </section>
              </div>
              <div className="page-actions">
                <Button secondary onClick={() => go("edit")}>
                  <ArrowLeft size={17} />
                  Back to Section Editing
                </Button>
              </div>
            </>
          )}
          {page === "preferences" && (
            <>
              <PageTitle
                eyebrow="LET’S MAKE THIS YOURS"
                title="What matters most to you?"
                subtitle=""
              />
              {isTechnical(reviewLevel) && (
                <section className="card rewrite-card">
                  <h2>Does your target role involve coding projects?</h2>
                  <div className="radio-grid">
                    {[true, false].map((value) => (
                      <label
                        className={
                          "choice " + (codingChoice === value ? "selected" : "")
                        }
                        key={String(value)}
                      >
                        <input
                          type="radio"
                          name="coding-applies"
                          checked={codingChoice === value}
                          onChange={() => {
                            setCodingChoice(value);
                            setResume((current) => ({
                              ...current,
                              softwareEngineering: value,
                            }));
                            setProposal(null);
                          }}
                        />
                        <span>
                          {value
                            ? "Yes, this applies"
                            : "No, this does not apply"}
                        </span>
                      </label>
                    ))}
                  </div>
                  {codingChoice === true && (
                    <p role="status">
                      {requiresProjects(reviewLevel)
                        ? "Use exactly 3 real projects and list the technologies used."
                        : "Projects are optional at your level; list the technologies for any you include."}{" "}
                      A repo or demo link helps show the work, but you can
                      continue without one.
                    </p>
                  )}
                  {codingChoice === false && (
                    <p role="status">
                      GitHub, coding projects, repositories, and technology
                      lists are optional. Include relevant work, labs, or
                      projects you can support with real details.
                    </p>
                  )}
                </section>
              )}
              <section className="card job-description-card">
                <h2>Tailor this resume to a job (optional)</h2>
                <p>
                  Add a job listing to tailor your resume, get an interview prep
                  sheet, and optionally create a matching cover letter.
                </p>
                <label
                  className={
                    "choice cover-letter-choice" +
                    (wantCoverLetter ? " selected" : "")
                  }
                >
                  <input
                    type="checkbox"
                    checked={wantCoverLetter}
                    onChange={(e) => setWantCoverLetter(e.target.checked)}
                  />
                  <span>
                    <strong>Create a cover letter</strong> (optional)
                    <small>Review and edit it with your finished resume.</small>
                  </span>
                </label>
                {reviewLevel === "General" && (
                  <div className="field">
                    <label htmlFor="general-focus">
                      Type of work you want to emphasize
                    </label>
                    <select
                      id="general-focus"
                      value={generalFocus}
                      onChange={(event) => setGeneralFocus(event.target.value)}
                    >
                      <option value="">Choose a focus (optional)</option>
                      {[
                        "Customer service",
                        "Retail or hospitality",
                        "Office work",
                        "Other",
                      ].map((focus) => (
                        <option key={focus} value={focus}>
                          {focus}
                        </option>
                      ))}
                    </select>
                    {generalFocus === "Other" && (
                      <input
                        aria-label="Other kind of work"
                        maxLength={100}
                        placeholder="What kind of work?"
                        value={otherGeneralFocus}
                        onChange={(event) =>
                          setOtherGeneralFocus(event.target.value)
                        }
                      />
                    )}
                  </div>
                )}
                {reviewLevel === "Medical Field" && (
                  <div className="field">
                    <label htmlFor="medical-role">
                      Which medical role are you building your resume for?
                    </label>
                    <select
                      id="medical-role"
                      value={
                        [
                          "",
                          "Nurse",
                          "Patient Care Coordinator / Representative",
                          "Medical Assistant",
                          "Phlebotomist",
                          "Physician Assistant (PA)",
                          "Physician (MD/DO)",
                          "PCT / PCA / CNA",
                        ].includes(targetRole)
                          ? targetRole
                          : "Other"
                      }
                      onChange={(event) => setTargetRole(event.target.value)}
                    >
                      <option value="">
                        Choose a role (highly recommended)
                      </option>
                      {[
                        "Nurse",
                        "Patient Care Coordinator / Representative",
                        "Medical Assistant",
                        "Phlebotomist",
                        "Physician Assistant (PA)",
                        "Physician (MD/DO)",
                        "PCT / PCA / CNA",
                        "Other",
                      ].map((role) => (
                        <option key={role} value={role}>
                          {role}
                        </option>
                      ))}
                    </select>
                    <small>
                      For Other, enter your role below. Choosing a role does not
                      add qualifications or credentials.
                    </small>
                  </div>
                )}
                {reviewLevel === "Medical Field" && (
                  <div className="field">
                    <label htmlFor="medical-role-type">
                      Is the target role clinical or nonclinical?
                    </label>
                    <select
                      id="medical-role-type"
                      value={medicalRoleType}
                      onChange={(event) =>
                        setMedicalRoleType(event.target.value)
                      }
                    >
                      <option value="">Choose if you know (optional)</option>
                      <option value="Clinical">Clinical</option>
                      <option value="Nonclinical">Nonclinical</option>
                      <option value="Not sure">Not sure</option>
                    </select>
                  </div>
                )}
                <div className="field">
                  <label htmlFor="target-role">Target role (optional)</label>
                  <input
                    id="target-role"
                    value={targetRole}
                    onChange={(event) => setTargetRole(event.target.value)}
                    placeholder={
                      reviewLevel === "Medical Field"
                        ? "For example, RN, physician assistant, or patient coordinator"
                        : reviewLevel === "Educator"
                          ? "For example, elementary teacher, professor, or tutor"
                          : reviewLevel === "General"
                            ? "For example, customer service representative"
                            : "For example, software engineer"
                    }
                  />
                </div>
                {reviewLevel === "General" && (
                  <div className="role-context-fields">
                    <p>
                      Share real examples you want considered. Leave any field
                      blank if it does not apply.
                    </p>
                    <div className="field">
                      <label htmlFor="general-service">
                        Service or communication
                      </label>
                      <textarea
                        id="general-service"
                        rows="2"
                        maxLength="1200"
                        value={generalService}
                        onChange={(event) =>
                          setGeneralService(event.target.value)
                        }
                        placeholder="How did you help customers, guests, coworkers, or others?"
                      />
                    </div>
                    <div className="field">
                      <label htmlFor="general-problems">
                        Problems you solved
                      </label>
                      <textarea
                        id="general-problems"
                        rows="2"
                        maxLength="1200"
                        value={generalProblemSolving}
                        onChange={(event) =>
                          setGeneralProblemSolving(event.target.value)
                        }
                        placeholder="What went wrong, what did you do, and what changed?"
                      />
                    </div>
                    <div className="field">
                      <label htmlFor="general-skills">
                        Practical skills and tools
                      </label>
                      <textarea
                        id="general-skills"
                        rows="2"
                        maxLength="1200"
                        value={generalPracticalSkills}
                        onChange={(event) =>
                          setGeneralPracticalSkills(event.target.value)
                        }
                        placeholder="For example, scheduling, POS, inventory, or training—only if you used them"
                      />
                    </div>
                    <div className="field">
                      <label htmlFor="general-volunteer">
                        Volunteer experience (optional)
                      </label>
                      <textarea
                        id="general-volunteer"
                        rows="2"
                        maxLength="1200"
                        value={generalVolunteerWork}
                        onChange={(event) =>
                          setGeneralVolunteerWork(event.target.value)
                        }
                        placeholder="Organization, role, dates, and what you actually did"
                      />
                    </div>
                    <small>
                      Volunteer work can support your resume when paid
                      experience is limited. We will label it as volunteer work,
                      not employment.
                    </small>
                  </div>
                )}
                {reviewLevel === "Medical Field" &&
                  ((/\b(nurs(?:e|ing)|RN|LPN|LVN)\b/i.test(targetRole) &&
                    !/\b(assistant|technician)\b/i.test(targetRole)) ||
                    /physician assistant|\bPA\b/i.test(targetRole)) && (
                    <div className="role-context-fields">
                      <div className="field">
                        <label htmlFor="nursing-stage">
                          Are you a student, a new graduate, or an experienced
                          professional in this role?
                        </label>
                        <select
                          id="nursing-stage"
                          value={answers.nursingStage || ""}
                          onChange={(event) =>
                            setAnswers({
                              ...answers,
                              nursingStage: event.target.value,
                            })
                          }
                        >
                          <option value="">
                            Choose if you know (optional)
                          </option>
                          {[
                            "Student",
                            "New graduate",
                            "Experienced professional",
                          ].map((stage) => (
                            <option key={stage}>{stage}</option>
                          ))}
                        </select>
                      </div>
                      {["Student", "New graduate"].includes(
                        answers.nursingStage,
                      ) && (
                        <>
                          <div className="field">
                            <label htmlFor="nursing-rotations">
                              Do you have clinical rotations to include?
                            </label>
                            <select
                              id="nursing-rotations"
                              value={answers.hasClinicalRotations || ""}
                              onChange={(event) =>
                                setAnswers({
                                  ...answers,
                                  hasClinicalRotations: event.target.value,
                                  ...(event.target.value === "Yes"
                                    ? {}
                                    : { clinicalRotations: "" }),
                                })
                              }
                            >
                              <option value="">Choose (optional)</option>
                              <option>Yes</option>
                              <option>No</option>
                              <option>Not sure</option>
                            </select>
                          </div>
                          {answers.hasClinicalRotations === "Yes" && (
                            <div className="field">
                              <label htmlFor="clinical-rotations">
                                Your clinical rotations
                              </label>
                              <textarea
                                id="clinical-rotations"
                                rows="4"
                                value={answers.clinicalRotations || ""}
                                onChange={(event) =>
                                  setAnswers({
                                    ...answers,
                                    clinicalRotations: event.target.value,
                                  })
                                }
                                placeholder="List the facility, unit or specialty, dates, hours if known, and supervised activities. Do not include patient information."
                              />
                            </div>
                          )}
                        </>
                      )}
                    </div>
                  )}
                {reviewLevel === "Medical Field" && (
                  <div className="role-context-fields">
                    <p>
                      Add only details that apply to your actual role. Every
                      field below is optional.
                    </p>
                    {/physician(?! assistant)|\b(?:MD|DO)\b/i.test(
                      targetRole,
                    ) && (
                      <>
                        <p>
                          A physician CV can be up to three pages when your
                          experience warrants it. Include only applicable
                          sections.
                        </p>
                        {[
                          [
                            "physicianResidency",
                            "Residency / Fellowship",
                            "Program, institution, specialty, dates, and completion status",
                          ],
                          [
                            "physicianLicensure",
                            "Licensure",
                            "License, jurisdiction, and current status; expiration if known",
                          ],
                          [
                            "physicianBoard",
                            "Board Certification",
                            "Board, specialty, certification dates and status; distinguish eligibility from certification",
                          ],
                          [
                            "physicianResearch",
                            "Research",
                            "Project, institution, role, dates, and your contributions",
                          ],
                          [
                            "physicianPublications",
                            "Publications",
                            "Authors, title, publication, date, and DOI or link if known; identify publication status",
                          ],
                          [
                            "physicianTeaching",
                            "Teaching",
                            "Institution, learners, teaching role, dates, and contributions",
                          ],
                          [
                            "physicianPresentations",
                            "Presentations",
                            "Title, event, date, and your presenter or coauthor role",
                          ],
                        ].map(([key, label, hint]) => (
                          <div className="field" key={key}>
                            <label htmlFor={key}>{label} (optional)</label>
                            <textarea
                              id={key}
                              rows="2"
                              value={answers[key] || ""}
                              onChange={(event) =>
                                setAnswers({
                                  ...answers,
                                  [key]: event.target.value,
                                })
                              }
                              placeholder={hint}
                            />
                          </div>
                        ))}
                      </>
                    )}
                    {/physician assistant|\bPA\b/i.test(targetRole) && (
                      <div className="field">
                        <label htmlFor="pa-procedures-specialty">
                          Do you have procedures or specialty experience to
                          highlight separately? (optional)
                        </label>
                        <textarea
                          id="pa-procedures-specialty"
                          rows="3"
                          maxLength="2400"
                          value={answers.paProceduresSpecialty || ""}
                          onChange={(event) =>
                            setAnswers({
                              ...answers,
                              paProceduresSpecialty: event.target.value,
                            })
                          }
                          placeholder="Name the specialty or procedures, setting, and your actual role. Distinguish observing, assisting, supervised performance, and independent work. Include dates or counts only if known."
                        />
                      </div>
                    )}
                    {/phlebotom/i.test(targetRole) && (
                      <div className="field">
                        <label htmlFor="phlebotomy-clinical-training">
                          Clinical Training: what supervised blood-draw
                          experience do you have? (optional)
                        </label>
                        <textarea
                          id="phlebotomy-clinical-training"
                          rows="3"
                          maxLength="2400"
                          value={answers.phlebotomyClinicalTraining || ""}
                          onChange={(event) =>
                            setAnswers({
                              ...answers,
                              phlebotomyClinicalTraining: event.target.value,
                            })
                          }
                          placeholder="Share the program or facility, dates, completed or in-progress status, and blood draws performed under supervision. Include hours or draw counts only if known. Do not include patient details."
                        />
                      </div>
                    )}
                    <div className="field">
                      <label htmlFor="medical-externship-training">
                        Have you completed, or are you currently completing, an
                        externship or medical training placement? (optional)
                      </label>
                      <textarea
                        id="medical-externship-training"
                        rows="3"
                        maxLength="2400"
                        value={answers.medicalExternshipTraining || ""}
                        onChange={(event) =>
                          setAnswers({
                            ...answers,
                            medicalExternshipTraining: event.target.value,
                          })
                        }
                        placeholder="Share the program or placement, facility, dates, completed or in-progress status, hours if known, and what you did under supervision. Leave blank if none."
                      />
                    </div>
                    <div className="field">
                      <label htmlFor="medical-credentials">
                        Licenses and credentials you hold
                      </label>
                      <textarea
                        id="medical-credentials"
                        rows="2"
                        maxLength="1200"
                        value={medicalCredentials}
                        onChange={(event) =>
                          setMedicalCredentials(event.target.value)
                        }
                        placeholder="Name and status of each credential, if relevant"
                      />
                    </div>
                    <div className="field">
                      <label htmlFor="medical-systems">
                        Systems and tools you have used
                      </label>
                      <textarea
                        id="medical-systems"
                        rows="2"
                        maxLength="1200"
                        value={medicalSystems}
                        onChange={(event) =>
                          setMedicalSystems(event.target.value)
                        }
                        placeholder="For example, scheduling or records systems you actually used"
                      />
                    </div>
                    <div className="field">
                      <label htmlFor="medical-experience">
                        Relevant experience to highlight
                      </label>
                      <textarea
                        id="medical-experience"
                        rows="2"
                        maxLength="1600"
                        value={medicalExperience}
                        onChange={(event) =>
                          setMedicalExperience(event.target.value)
                        }
                        placeholder="Your real responsibilities, teamwork, or improvements"
                      />
                    </div>
                    <small>
                      Do not enter patient names, record numbers, or other
                      identifying patient details. We will not assume you hold a
                      license or performed clinical duties.
                    </small>
                  </div>
                )}
                <details
                  className="job-post-details"
                  defaultOpen={Boolean(jobLink || jobDescription)}
                >
                  <summary>Add job listing</summary>
                  <div className="field">
                    <label htmlFor="job-link">Job posting link</label>
                    <div className="job-link-row">
                      <input
                        id="job-link"
                        type="text"
                        inputMode="url"
                        value={jobLink}
                        onChange={(event) => setJobLink(event.target.value)}
                        onBlur={() => setJobLink(normalizeWebLink(jobLink))}
                        placeholder="linkedin.com/jobs/view/..."
                      />
                      <Button
                        secondary
                        onClick={importJobLink}
                        disabled={jobLoading || !jobLink.trim()}
                      >
                        Read link
                      </Button>
                    </div>
                  </div>
                  <div
                    className="field job-file-drop"
                    onDragOver={(event) => event.preventDefault()}
                    onDrop={(event) => {
                      event.preventDefault();
                      if (!jobLoading)
                        importJobFile(event.dataTransfer.files?.[0]);
                    }}
                  >
                    <label htmlFor="job-file">Upload a job listing</label>
                    <input
                      id="job-file"
                      type="file"
                      accept=".pdf,.docx,.doc,.png,.jpg,.jpeg,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/msword,image/png,image/jpeg"
                      disabled={jobLoading}
                      onChange={(event) =>
                        importJobFile(event.target.files?.[0])
                      }
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="job-description">
                      Job description text
                    </label>
                    <textarea
                      id="job-description"
                      rows="4"
                      maxLength="12000"
                      value={jobDescription}
                      onChange={(event) => {
                        setJobDescription(event.target.value);
                        setJobPrep(null);
                        setJobPrepError("");
                      }}
                      placeholder="Paste the job description here, or review the imported text."
                    />
                    {jobLoading && <small>Reading job listing…</small>}
                  </div>
                </details>
              </section>
              {technicalOptions && (
                <>
                  <section
                    className="card rewrite-card"
                    aria-labelledby="link-color-heading"
                  >
                    <h2 id="link-color-heading">Choose your link color</h2>
                    <div className="radio-grid">
                      {[
                        ["blue", "Original blue", "#0000EE"],
                        ["dark", "Dark blue", "#23476A"],
                      ].map(([value, label, color]) => (
                        <label
                          className={
                            "choice " + (linkColor === value ? "selected" : "")
                          }
                          key={value}
                        >
                          <input
                            type="radio"
                            name="linkColor"
                            checked={linkColor === value}
                            onChange={() => setLinkColor(value)}
                          />
                          <span>
                            <strong>{label}</strong>
                            <small
                              style={{
                                color,
                                textDecoration: "underline",
                                display: "block",
                              }}
                            >
                              LinkedIn · GitHub · Portfolio
                            </small>
                          </span>
                        </label>
                      ))}
                    </div>
                  </section>
                  <section
                    className="card rewrite-card"
                    aria-labelledby="project-format-heading"
                  >
                    <h2 id="project-format-heading">
                      Choose how project links appear
                    </h2>
                    <p>Only your supplied links will appear.</p>
                    <div className="radio-grid">
                      {[
                        [
                          "descriptive",
                          "Project-named links",
                          "EverPure Web Application | EverPure GitHub",
                        ],
                        ["compact", "Short links", "EverPure | Demo | Repo"],
                      ].map(([value, label, example]) => (
                        <label
                          className={
                            "choice " +
                            (projectFormat === value ? "selected" : "")
                          }
                          key={value}
                        >
                          <input
                            type="radio"
                            name="projectFormat"
                            checked={projectFormat === value}
                            onChange={() => setProjectFormat(value)}
                          />
                          <span>
                            <strong>{label}</strong>
                            <small
                              style={{
                                color:
                                  linkColor === "dark" ? "#23476A" : "#0000EE",
                                textDecoration: "underline",
                                display: "block",
                              }}
                            >
                              {example}
                            </small>
                          </span>
                        </label>
                      ))}
                    </div>
                  </section>
                </>
              )}
              {(experienceChoiceRequired ||
                (coding && projectOptions.length > 3)) && (
                <section className="card shortlist-card">
                  <h2>Choose what stays on your resume</h2>
                  <p>
                    {seniorExperience
                      ? "HIGHLY RECOMMENDED: Highlight four to six relevant jobs when available. You can keep a different number."
                      : "HIGHLY RECOMMENDED: Choose the jobs and projects most relevant to your goal. You can change our suggestions."}
                  </p>
                  {[
                    [
                      "Experience",
                      experienceOptions,
                      keptExperience,
                      selectedExperience,
                      setSelectedExperience,
                      experienceMax,
                    ],
                    [
                      "Projects",
                      projectOptions,
                      keptProjects,
                      selectedProjects,
                      setSelectedProjects,
                      3,
                    ],
                  ]
                    .filter(([label, items, , , , limit]) =>
                      label === "Experience"
                        ? experienceChoiceRequired
                        : coding && items.length > limit,
                    )
                    .map(
                      ([label, items, kept, selected, setSelected, limit]) => (
                        <div className="shortlist-group" key={label}>
                          <h3>
                            {label}{" "}
                            <small>
                              {kept.length} selected ·{" "}
                              {label === "Experience"
                                ? seniorExperience
                                  ? "choose 4–6"
                                  : "choose 3–4"
                                : "choose 3"}
                            </small>
                          </h3>
                          {label === "Experience" && (
                            <p className="microcopy">
                              {experienceYearsCovered(
                                kept.map((index) => items[index]),
                              ) === null
                                ? "Check the job dates yourself; we could not estimate the years shown."
                                : `Selected jobs show about ${experienceYearsCovered(kept.map((index) => items[index]))} years by their dates. Overlapping years count once.`}
                            </p>
                          )}
                          {label === "Projects" && (
                            <div className="project-selection-actions">
                              <p className="microcopy">
                                HIGHLY RECOMMENDED: These three match your
                                target role. You can choose more or fewer
                                projects.
                              </p>
                              <Button
                                secondary
                                onClick={() =>
                                  setSelectedProjects(
                                    suggestedProjectIndexes(items, targetRole),
                                  )
                                }
                              >
                                Use suggested three
                              </Button>
                              <Button
                                secondary
                                onClick={() => setSelectedProjects([0, 1, 2])}
                              >
                                Keep first three from my resume
                              </Button>
                            </div>
                          )}
                          <div className="shortlist-grid">
                            {items.map((item, index) => (
                              <label
                                className={
                                  "choice shortlist-choice " +
                                  (kept.includes(index) ? "selected" : "")
                                }
                                key={`${label}-${index}`}
                              >
                                <input
                                  type="checkbox"
                                  checked={kept.includes(index)}
                                  onChange={() =>
                                    toggleSelection(
                                      index,
                                      items,
                                      selected,
                                      setSelected,
                                      limit,
                                      label === "Experience",
                                    )
                                  }
                                />
                                <span>
                                  <strong>{item.split("\n")[0]}</strong>
                                  <small>
                                    {item
                                      .split("\n")
                                      .slice(1)
                                      .join(" ")
                                      .slice(0, 150)}
                                  </small>
                                </span>
                              </label>
                            ))}
                          </div>
                        </div>
                      ),
                    )}
                </section>
              )}
              {technicalOptions && (
                <section
                  className="card profile-help"
                  aria-label="Suggested links"
                >
                  <h2>Suggested links</h2>
                  {coding && (
                    <p className="highly-recommended">
                      Highly recommended for tech resumes
                    </p>
                  )}
                  <p>
                    All links are optional. We’ll use matching public
                    repositories to fill missing project links after
                    customization. You can review and edit them.
                  </p>
                  <div className="field">
                    <label htmlFor="custom-github">
                      GitHub / Project Code URL · Suggested · Optional
                    </label>
                    <input
                      id="custom-github"
                      type="text"
                      inputMode="url"
                      onBlur={() =>
                        setProfileGithub(normalizeWebLink(profileGithub))
                      }
                      value={profileGithub}
                      onChange={(e) => setProfileGithub(e.target.value)}
                      placeholder="https://github.com/yourname"
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="custom-linkedin">
                      LinkedIn URL · Suggested · Optional
                    </label>
                    <input
                      id="custom-linkedin"
                      type="text"
                      inputMode="url"
                      onBlur={() =>
                        setProfileLinkedin(normalizeWebLink(profileLinkedin))
                      }
                      value={profileLinkedin}
                      onChange={(e) => setProfileLinkedin(e.target.value)}
                      placeholder="https://www.linkedin.com/in/yourname"
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="custom-portfolio">
                      Portfolio URL · Suggested · Optional
                    </label>
                    <input
                      id="custom-portfolio"
                      type="text"
                      inputMode="url"
                      onBlur={() =>
                        setProfilePortfolio(normalizeWebLink(profilePortfolio))
                      }
                      value={profilePortfolio}
                      onChange={(e) => setProfilePortfolio(e.target.value)}
                      placeholder="https://your-portfolio.example"
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="custom-demo">
                      Project Demo URL · Suggested · Optional
                    </label>
                    <textarea
                      id="custom-demo"
                      rows={5}
                      value={projectDemoUrl}
                      onChange={(e) => setProjectDemoUrl(e.target.value)}
                      placeholder={
                        "Paste one Project Demo URL per line\nhttps://your-first-project.example\nhttps://your-second-project.example"
                      }
                    />
                    <small>
                      Add as many project demos as you have, one URL per line.
                    </small>
                  </div>
                </section>
              )}
              {technicalOptions && (
                <ProjectSuggestions
                  profile={profileGithub}
                  projects={projectOptions}
                  target={`${targetRole} ${jobDescription}`}
                  disabled={demo || !coding}
                />
              )}
              {technicalOptions && projectOptions.length > 0 && (
                <section className="card profile-help">
                  <h2>Project demo and repo links</h2>
                  <p>
                    Autofill matching public GitHub links, then edit anything
                    you want. Neither link is required.
                  </p>
                  <Button
                    secondary
                    disabled={busy}
                    onClick={autofillProjectLinks}
                  >
                    Autofill from GitHub
                  </Button>
                  {linkDiscoveryNote && (
                    <p role="status">{linkDiscoveryNote}</p>
                  )}
                  {projectOptions
                    .map((project, index) => ({ project, index }))
                    .filter(
                      ({ index }) =>
                        projectOptions.length <= 3 ||
                        keptProjects.includes(index),
                    )
                    .map(({ project, index }) => (
                      <section
                        className="project-links-customization"
                        key={`project-links-${index}`}
                      >
                        <h3>{project.split("\n")[0].split("|")[0].trim()}</h3>
                        <ProjectLinkFields
                          id={`custom-project-${page}-${index}`}
                          value={
                            projectLinks[index] ??
                            projectLinkUrls(project).join(" | ")
                          }
                          onChange={(value) =>
                            setProjectLinks((current) => ({
                              ...current,
                              [index]: value,
                            }))
                          }
                        />
                      </section>
                    ))}
                </section>
              )}
              {experienceOptions
                .map((entry, index) => ({
                  entry,
                  index,
                  needed:
                    (keptExperience.length > 1 &&
                    keptExperience.indexOf(index) === keptExperience.length - 1
                      ? 2
                      : 3) - experienceBullets(entry).length,
                }))
                .filter(
                  ({ index, needed }) =>
                    needed > 0 &&
                    (!experienceChoiceRequired ||
                      keptExperience.includes(index)),
                )
                .map(({ entry, index, needed }) => (
                  <section
                    className="card shortlist-card"
                    key={`role-details-${index}`}
                  >
                    <h2>
                      More details for{" "}
                      {entry.split("\n").slice(0, 2).join(" — ")}
                    </h2>
                    <p>
                      Optional: add other real contributions, one per line. You
                      can continue with the details you already supplied.
                    </p>
                    <textarea
                      value={roleDetails[index] || ""}
                      rows={needed + 1}
                      onChange={(event) =>
                        setRoleDetails((current) => ({
                          ...current,
                          [index]: event.target.value,
                        }))
                      }
                      placeholder={
                        reviewLevel === "Educator"
                          ? "How did this experience help you teach, support learners, or prepare for education work? Describe only what you actually did or learned."
                          : reviewLevel === "Medical Field"
                            ? "What were your responsibilities and contributions? Include patient care only if it was part of your role; leave out private patient details."
                            : technicalOptions
                              ? "What did you build, improve, or deliver in this role?"
                              : "How did you help customers, support your team, or improve daily work?"
                      }
                    />
                  </section>
                ))}
              {experienceOptions
                .map((entry, index) => ({
                  entry,
                  index,
                  bullets: experienceBullets(entry),
                  limit: bulletLimitFor(index),
                }))
                .filter(
                  ({ index, bullets, limit }) =>
                    bullets.length > limit &&
                    (!experienceChoiceRequired ||
                      keptExperience.includes(index)),
                )
                .map(({ entry, index, bullets, limit }) => {
                  const choice = bulletChoices[index];
                  const selected =
                    choice?.mode === "select" ? choice.selected : [];
                  return (
                    <section
                      className="card shortlist-card"
                      key={`bullets-${index}`}
                    >
                      <h2>Choose bullets for {entry.split("\n")[0]}</h2>
                      <p>
                        Pick {limit}, or try combining your facts into {limit}{" "}
                        bullets. Each final bullet needs 15–30 words.
                      </p>
                      <label className="choice">
                        <input
                          type="checkbox"
                          checked={choice?.mode === "combine"}
                          onChange={(event) =>
                            setBulletChoices((current) => ({
                              ...current,
                              [index]: event.target.checked
                                ? { mode: "combine" }
                                : { mode: "select", selected: [] },
                            }))
                          }
                        />
                        <span>Try to combine all into {limit} bullets</span>
                      </label>
                      {choice?.mode !== "combine" &&
                        bullets.map((bullet, bulletIndex) => (
                          <label className="choice" key={bulletIndex}>
                            <input
                              type="checkbox"
                              checked={selected.includes(bulletIndex)}
                              onChange={() =>
                                setBulletChoices((current) => {
                                  const previous =
                                    current[index]?.mode === "select"
                                      ? current[index].selected
                                      : [];
                                  const next = previous.includes(bulletIndex)
                                    ? previous.filter((i) => i !== bulletIndex)
                                    : [...previous, bulletIndex].sort(
                                        (a, b) => a - b,
                                      );
                                  return {
                                    ...current,
                                    [index]: { mode: "select", selected: next },
                                  };
                                })
                              }
                            />
                            <span>{bullet}</span>
                          </label>
                        ))}
                      {choice?.mode === "select" && (
                        <small>
                          {selected.length} of {limit} bullets selected
                        </small>
                      )}
                    </section>
                  );
                })}
              <div className="preferences-layout">
                {preferenceGroups.map((g) => (
                  <section className="card preference-card" key={g.title}>
                    <h2>{g.title}</h2>
                    {g.items.map((item) => (
                      <label
                        className={
                          "choice " +
                          (preferences.includes(item) ? "selected" : "")
                        }
                        key={item}
                      >
                        <input
                          type="checkbox"
                          checked={preferences.includes(item)}
                          onChange={() =>
                            setPreferences((p) =>
                              p.includes(item)
                                ? p.filter((x) => x !== item)
                                : [...p, item],
                            )
                          }
                        />
                        <span>{item}</span>
                      </label>
                    ))}
                    {preferences.includes("Add another option") && (
                      <div className="field">
                        <label htmlFor="other-priority">
                          What else matters to you?
                        </label>
                        <input
                          id="other-priority"
                          maxLength={120}
                          value={otherPriority}
                          onChange={(event) =>
                            setOtherPriority(event.target.value)
                          }
                          placeholder="For example, career growth"
                        />
                      </div>
                    )}
                  </section>
                ))}
              </div>
              <section
                className="card rewrite-card font-card"
                aria-labelledby="font-heading"
              >
                <h2 id="font-heading">Choose your resume font</h2>
                <p>
                  Choose one. Each name below is shown in its own font. All six
                  meet our font requirement.
                </p>
                <div className="font-grid">
                  {fonts.map((name) => (
                    <label
                      className={
                        "choice font-choice " +
                        (font === name ? "selected" : "")
                      }
                      key={name}
                    >
                      <input
                        type="radio"
                        name="resumeFont"
                        value={name}
                        checked={font === name}
                        onChange={() => setFont(name)}
                        aria-label={name}
                      />
                      <img
                        src={`/font-samples/${name.toLowerCase().replaceAll(" ", "-")}.png`}
                        alt=""
                        aria-hidden="true"
                      />
                      <span className="sr-only">{name}</span>
                    </label>
                  ))}
                </div>
                <small>
                  Your choice is used in your resume preview and final PDF.
                </small>
              </section>
              <section
                className="card rewrite-card"
                aria-labelledby="date-heading"
              >
                <h2 id="date-heading">Where should the dates go?</h2>
                <div className="radio-grid">
                  {[
                    ["inline", "Beside the role or school"],
                    ["right", "Aligned to the far right"],
                  ].map(([value, label]) => (
                    <label
                      className={
                        "choice date-choice " +
                        (dates === value ? "selected" : "")
                      }
                      key={value}
                    >
                      <input
                        type="radio"
                        name="datePlacement"
                        checked={dates === value}
                        onChange={() => setDates(value)}
                      />
                      <span>
                        <strong>{label}</strong>
                        <span className={"date-example " + value}>
                          <span>Software Engineer</span>
                          <span>2024–Present</span>
                        </span>
                      </span>
                    </label>
                  ))}
                </div>
              </section>
              <section className="card rewrite-card">
                <h2>How much rewriting feels right?</h2>
                <div className="radio-grid">
                  {rewriteOptions.map((item) => (
                    <label
                      className={
                        "choice " + (rewrite === item ? "selected" : "")
                      }
                      key={item}
                    >
                      <input
                        name="rewrite"
                        type="radio"
                        checked={rewrite === item}
                        onChange={() => setRewrite(item)}
                      />
                      <span>{item}</span>
                    </label>
                  ))}
                </div>
                {rewrite === "Add another option" && (
                  <div className="field">
                    <label htmlFor="other-rewrite">
                      How should we handle the wording?
                    </label>
                    <input
                      id="other-rewrite"
                      maxLength={160}
                      value={otherRewrite}
                      onChange={(event) => setOtherRewrite(event.target.value)}
                      placeholder="Tell us what you prefer"
                    />
                  </div>
                )}
              </section>
              <Notice>
                {isTechnical(reviewLevel)
                  ? "HIGHLY RECOMMENDED: Aim for one page. Longer resumes can continue onto another page."
                  : "HIGHLY RECOMMENDED: Keep your resume concise. Longer resumes can continue onto another page."}
              </Notice>
              {error && <Notice error>{error}</Notice>}
              <div className="page-actions">
                <Button secondary onClick={() => go("invitation")}>
                  <ArrowLeft size={16} />
                  Back
                </Button>
                <Button onClick={() => beginBuild()} disabled={busy}>
                  {busy ? (
                    <LoaderCircle className="spin" size={17} />
                  ) : (
                    <>
                      Continue
                      <ArrowRight size={17} />
                    </>
                  )}
                </Button>
              </div>
            </>
          )}
          {page === "questions" && (
            <>
              <PageTitle
                eyebrow="A FEW DETAILS ONLY YOU KNOW"
                title="Help us fill in the gaps"
                subtitle="Share what you know. Skip what you don’t."
              />
              <form
                className="questions card"
                onSubmit={(e) => {
                  e.preventDefault();
                  build();
                }}
              >
              {technicalOptions && <NewProjectFields value={newProjects} onChange={setNewProjects} />}
              <NewExperienceFields value={newExperience} onChange={setNewExperience} hasExperience={experienceOptions.length > 0} />
                {projectOptions
                  .map((project, index) => ({ project, index }))
                  .filter(
                    ({ index }) =>
                      technicalOptions &&
                      coding &&
                      (projectOptions.length <= 3 ||
                        keptProjects.includes(index)),
                  )
                  .map(({ project, index }) => (
                    <section
                      className="card shortlist-card"
                      key={`stack-${index}`}
                    >
                      <ProjectLinkFields
                        id={`question-project-${index}`}
                        value={
                          projectLinks[index] ??
                          projectLinkUrls(project).join(" | ")
                        }
                        onChange={(value) =>
                          setProjectLinks((current) => ({
                            ...current,
                            [index]: value,
                          }))
                        }
                      />
                      <h2>
                        Technologies used in “
                        {project.split("\n")[0].split("|")[0].trim()}”
                      </h2>
                      <p>
                        Optional. Review the technologies found in your resume
                        or repository. Edit anything that does not describe your
                        work.
                      </p>
                      <input
                        aria-label={`Project ${index + 1} technologies`}
                        value={projectStacks[index] ?? projectStack(project)}
                        onChange={(event) =>
                          setProjectStacks((current) => ({
                            ...current,
                            [index]: event.target.value,
                          }))
                        }
                        placeholder="For example: Java, Spring Boot, React, PostgreSQL"
                      />
                      <button
                        type="button"
                        className="text-button"
                        disabled={Boolean(repoChecks[index]?.loading)}
                        onClick={() => checkProjectRepo(index, project)}
                      >
                        {repoChecks[index]?.loading
                          ? "Checking public repo…"
                          : "Check repo for missing technologies"}
                      </button>
                      {repoChecks[index]?.error && (
                        <p className="invalid">{repoChecks[index].error}</p>
                      )}
                      {repoChecks[index]?.technologies && (
                        <div className="repo-suggestion">
                          <p>
                            Found in the repo:{" "}
                            {repoChecks[index].technologies.join(", ")}. Confirm
                            what you used.
                          </p>
                          <button
                            type="button"
                            className="text-button"
                            onClick={() =>
                              setProjectStacks((current) => ({
                                ...current,
                                [index]: [
                                  ...new Set([
                                    ...(current[index] ?? projectStack(project))
                                      .split(",")
                                      .map((item) => item.trim())
                                      .filter(Boolean),
                                    ...repoChecks[index].technologies,
                                  ]),
                                ].join(", "),
                              }))
                            }
                          >
                            Add these to the editable field
                          </button>
                        </div>
                      )}
                    </section>
                  ))}
                {linkDiscoveryNote && <p role="status">{linkDiscoveryNote}</p>}
                {technicalOptions &&
                  projectOptions.length > 1 &&
                  projectDemoUrl
                    .split(/\r?\n/)
                    .map((url) => url.trim())
                    .filter(Boolean)
                    .map((demoUrl, demoIndex) => (
                      <div className="field" key={demoUrl}>
                        <label htmlFor={`demo-project-match-${demoIndex}`}>
                          Which project uses this Project Demo URL? · Optional
                        </label>
                        <p>{demoUrl}</p>
                        <select
                          id={`demo-project-match-${demoIndex}`}
                          defaultValue=""
                          onChange={(e) => {
                            if (e.target.value === "") return;
                            const index = Number(e.target.value);
                            setProjectLinks((current) => ({
                              ...current,
                              [index]: [
                                ...new Set([
                                  ...(
                                    current[index] ??
                                    projectLinkUrls(projectOptions[index]).join(
                                      " | ",
                                    )
                                  )
                                    .split("|")
                                    .map((url) => url.trim())
                                    .filter(Boolean),
                                  demoUrl,
                                ]),
                              ].join(" | "),
                            }));
                          }}
                        >
                          <option value="">Leave unassigned</option>
                          {projectOptions.map((project, index) => (
                            <option key={index} value={index}>
                              {project.split("\n")[0].split("|")[0]}
                            </option>
                          ))}
                        </select>
                      </div>
                    ))}
                <div className="improvement-question-grid">
                  {gapQuestions.slice(0, visibleQuestionCount).map((q, i) => (
                    <div className="question" key={q.id}>
                      <span className="question-number">{i + 1}</span>
                      <div className="field">
                        <label htmlFor={q.id}>
                          {q.question}
                          {q.required ? " · HIGHLY RECOMMENDED" : " · Optional"}
                        </label>
                        <p>{q.hint}</p>
                        <textarea
                          id={q.id}
                          value={answers[q.id] || ""}
                          onChange={(e) =>
                            setAnswers({ ...answers, [q.id]: e.target.value })
                          }
                          placeholder="Share what you know…"
                          rows={3}
                        />
                        {
                          <button
                            type="button"
                            className="text-button"
                            onClick={() =>
                              setAnswers({ ...answers, [q.id]: "Not sure" })
                            }
                          >
                            I’m not sure — leave this out
                          </button>
                        }
                      </div>
                    </div>
                  ))}
                </div>
                {visibleQuestionCount < gapQuestions.length && (
                  <button
                    type="button"
                    className="more-resume-questions"
                    onClick={() =>
                      setVisibleQuestionCount((count) => count + 4)
                    }
                  >
                    More questions to improve my resume
                  </button>
                )}
                {error && <Notice error>{error}</Notice>}
                <div className="page-actions">
                  <Button
                    type="button"
                    secondary
                    onClick={() => go("preferences")}
                  >
                    <ArrowLeft size={16} />
                    Back
                  </Button>
                  <Button disabled={busy}>
                    {busy ? (
                      <>
                        <LoaderCircle className="spin" size={17} />
                        Preparing your draft
                      </>
                    ) : (
                      <>
                        Create My Draft
                        <ArrowRight size={17} />
                      </>
                    )}
                  </Button>
                </div>
              </form>
            </>
          )}
          {page === "approval" && proposal && (
            <>
              <PageTitle
                eyebrow="YOU DECIDE WHAT CHANGES"
                title="Review the proposed changes"
                subtitle="Check the draft before accepting it."
              />
              <section className="card questions">
                {(proposal.approvalQuestions || []).map((q, i) => (
                  <div className="question" key={i}>
                    <div>
                      <h3>{q.question}</h3>
                      <p>{q.hint}</p>
                    </div>
                  </div>
                ))}
                {(proposal.protectedChanges || []).map((change) => (
                  <div
                    className="question"
                    key={change.section}
                    style={{ display: "block" }}
                  >
                    <h3>
                      {change.section === "experience"
                        ? "Work experience"
                        : "Certifications & training"}
                    </h3>
                    <h4>Original</h4>
                    <p style={{ whiteSpace: "pre-wrap" }}>
                      {change.before || "None"}
                    </p>
                    <h4>Proposed</h4>
                    <p style={{ whiteSpace: "pre-wrap" }}>
                      {change.after || "Omitted from this draft"}
                    </p>
                  </div>
                ))}
                <details>
                  <summary>View the full proposed draft</summary>
                  {resumeSections(reviewLevel).map(([key, label]) => (
                    <div key={key}>
                      <h3>{label}</h3>
                      <p style={{ whiteSpace: "pre-wrap" }}>
                        {key === "contact"
                          ? Object.values(proposal.resume.contact || {})
                              .filter(Boolean)
                              .join(" · ")
                          : proposal.resume[key] || "Not supplied"}
                      </p>
                    </div>
                  ))}
                </details>
                <label className="consent">
                  <input
                    type="checkbox"
                    checked={proposalApproved}
                    onChange={(e) => setProposalApproved(e.target.checked)}
                  />
                  I have reviewed the proposed draft and approve the changes
                  shown. I will correct missing or inaccurate details before
                  finishing.
                </label>
                <div className="page-actions">
                  <Button
                    secondary
                    onClick={() => {
                      setProposal(null);
                      setProposalApproved(false);
                      go(
                        review.questions?.length ? "questions" : "preferences",
                      );
                    }}
                  >
                    Keep Original & Go Back
                  </Button>
                  <Button
                    disabled={!proposalApproved}
                    onClick={() => {
                      if (!proposalApproved) return;
                      setResume(clone(proposal.resume));
                      if (coachEdition)
                        sendOwnerCopy("draft", proposal.resume, {
                          font,
                          dates,
                          linkColor,
                        });
                      setProposal(null);
                      setProposalApproved(false);
                      go("edit");
                    }}
                  >
                    Accept Draft & Continue Editing
                  </Button>
                </div>
              </section>
            </>
          )}
          {page === "edit" && (
            <>
              <PageTitle
                eyebrow="BUILD IT TOGETHER"
                title="Your resume, piece by piece"
                subtitle="Check the facts and edit the wording."
              />
              <div className="editor-layout">
                <section className="section-list">
                  {resumeSections(reviewLevel)
                    .filter(
                      ([k]) =>
                        !(
                          ["projects", "certifications"].includes(k) &&
                          !resume[k] &&
                          ["Senior", "Management"].includes(reviewLevel)
                        ),
                    )
                    .map(([key, label], i) => (
                      <article className="section-card card" key={key}>
                        <div className="section-card-title">
                          <span className="section-number">0{i + 1}</span>
                          <h3>{label}</h3>
                          <button
                            className="edit-button"
                            onClick={() => setEditSection(key)}
                          >
                            <PenLine size={15} />
                            Edit
                          </button>
                        </div>
                        <p className="section-excerpt">
                          {key === "contact"
                            ? Object.values(resume.contact)
                                .filter(Boolean)
                                .slice(0, 3)
                                .join(" · ")
                            : resume[key] || "Add your details"}
                        </p>
                        {key === "summary" && isTechnical(reviewLevel) && (
                          <small
                            className={
                              countWords(resume.summary) < 35 ||
                              countWords(resume.summary) > 45
                                ? "invalid"
                                : "muted"
                            }
                          >
                            {countWords(resume.summary)} words · aim for 35–45
                          </small>
                        )}
                        {key === "projects" &&
                          codingRequired(resume) &&
                          requiresProjects(reviewLevel) && (
                            <small
                              className={
                                projectList(resume.projects).length === 3
                                  ? "muted"
                                  : "invalid"
                              }
                            >
                              {projectList(resume.projects).length} / 3 projects
                            </small>
                          )}
                        {key === "projects" &&
                          projectList(resume.projects).some(
                            (project) => !projectLinkUrls(project).length,
                          ) && (
                            <small className="muted">
                              Tip: a repo or demo link helps people see your
                              work.
                            </small>
                          )}
                        {key === "projects" &&
                          projectList(resume.projects).length > 0 && (
                            <small
                              className={
                                projectList(resume.projects).some(
                                  (project) =>
                                    projectDescriptionWords(project) > 45,
                                )
                                  ? "invalid"
                                  : "muted"
                              }
                            >
                              Project descriptions:{" "}
                              {projectList(resume.projects)
                                .map((project) =>
                                  projectDescriptionWords(project),
                                )
                                .join(", ")}{" "}
                              words · Aim for 35–40; up to 45 for larger
                              projects
                            </small>
                          )}
                      </article>
                    ))}
                </section>
                <button
                  type="button"
                  className="mobile-preview-toggle"
                  aria-expanded={mobilePreviewOpen}
                  onClick={() => setMobilePreviewOpen(!mobilePreviewOpen)}
                >
                  <Eye size={18} />{" "}
                  {mobilePreviewOpen
                    ? "Hide live preview"
                    : "View live preview"}
                </button>
                <aside
                  className={`editor-preview ${mobilePreviewOpen ? "mobile-open" : ""}`}
                >
                  <DocumentSwitcher
                    resume={resume}
                    aiResume={aiResume}
                    level={reviewLevel}
                    font={font}
                    dates={dates}
                    linkColor={linkColor}
                    projectFormat={projectFormat}
                    coverLetter={coverLetter}
                    onEditCover={() => setCoverOpen(true)}
                    onViewJobPrep={() => {
                      setJobPrepOpen(true);
                      if (
                        jobDescription.trim() &&
                        jobPrep?.source !== jobDescription.trim() &&
                        !jobPrepLoading &&
                        !jobPrepError
                      )
                        makeJobPrep();
                    }}
                    showCover={!demo || demoMode}
                  />
                </aside>
              </div>
              {error && <Notice error>{error}</Notice>}
              <div className="page-actions">
                <Button secondary onClick={() => go("preferences")}>
                  <ArrowLeft size={16} />
                  Preferences
                </Button>
                {validateResume(resume, reviewLevel).length > 0 && (
                  <aside className="resume-recommendations">
                    <strong>HIGHLY RECOMMENDED</strong>
                    <ul>
                      {validateResume(resume, reviewLevel).map((tip) => (
                        <li key={tip}>{tip}</li>
                      ))}
                    </ul>
                    <p>You can continue with your current resume.</p>
                  </aside>
                )}
                <Button
                  onClick={() => {
                    setLayout(null);
                    go("layout");
                    if (aiSource !== aiInputKey) generateAiVersion(resume);
                  }}
                >
                  Compare My Resume Options
                  <ArrowRight size={17} />
                </Button>
              </div>
            </>
          )}
          {page === "complete" && layout && (
            <>
              <div className="complete-top-row">
                <PageTitle
                  eyebrow="READY FOR YOUR NEXT CHAPTER"
                  title="Your story, brought together"
                  subtitle={
                    layout === "original"
                      ? "Your version is ready."
                      : "Your PNB Paws version is ready."
                  }
                />
                {coachEdition && !demo && (
                  <ExperienceRatingPanel
                    beta={access.accessKind === "beta-pass"}
                    submitted={betaRated}
                    onSubmitted={() => setBetaRated(true)}
                  />
                )}
              </div>
              {demoMode && layout === "original" && (
                <p className="rerate-result">
                  <strong>Example final rating: 9.2/10</strong>
                  <span>
                    Illustrative demo score, not a live re-review. Your edits
                    are not automatically rescored.
                  </span>
                </p>
              )}
              <div className="complete-layout">
                <div className="finished-preview">
                  <DocumentSwitcher
                    resume={resume}
                    aiResume={aiResume}
                    level={reviewLevel}
                    font={font}
                    dates={dates}
                    linkColor={linkColor}
                    projectFormat={projectFormat}
                    coverLetter={coverLetter}
                    onEditCover={() => setCoverOpen(true)}
                    onViewJobPrep={() => {
                      setJobPrepOpen(true);
                      if (
                        jobDescription.trim() &&
                        jobPrep?.source !== jobDescription.trim() &&
                        !jobPrepLoading &&
                        !jobPrepError
                      )
                        makeJobPrep();
                    }}
                    initial={layout}
                    showCover={!demo || demoMode}
                  />
                </div>
                <div className="final-sidebar">
                  <aside className="final-actions card">
                    <div className="icon-orb">
                      <CheckCheck size={29} />
                    </div>
                    <h2>Send or download</h2>
                    {(!coachEdition || betaEdition) && (
                      <Button secondary className="resume-help-button" onClick={() => openDelivery("human")}>
                        <Mail size={16} />
                        {betaEdition
                          ? "Request Resume Help"
                          : "Ask a Human for Help"}
                      </Button>
                    )}
                    {(!demo || demoMode) && (
                      <div className="finished-document-tools">
                        <Button
                          secondary
                          className="final-action-emphasis"
                          onClick={() => setCoverOpen(true)}
                        >
                          Update Cover Letter
                        </Button>
                      </div>
                    )}
                    <button
                      type="button"
                      className="roast-image-button email-resume-button"
                      onClick={() => openDelivery("email")}
                      aria-label="Email me my resume"
                    >
                      <img src="/email-button.png" alt="" />
                    </button>

                    <button
                      type="button"
                      className="roast-image-button download-image-button"
                      onClick={downloadWord}
                      disabled={busy}
                      aria-label="Download Word document"
                    >
                      <img src="/download-word-button.png" alt="" />
                    </button>
                    <button
                      type="button"
                      className="roast-image-button download-image-button"
                      onClick={downloadPdf}
                      disabled={busy}
                      aria-label="Download PDF"
                    >
                      <img src="/download-pdf-button.png" alt="" />
                    </button>
                    {!demo && (
                      <>
                        <button
                          type="button"
                          className="roast-image-button glow-roast-button"
                          onClick={toggleGlowRoast}
                          disabled={Boolean(currentGlowRoast?.loading)}
                          aria-label="Glow-Up Roast"
                          aria-expanded={Boolean(currentGlowRoast?.open)}
                        >
                          <img src="/glow-up-roast-transparent.png" alt="" />
                        </button>
                        {currentGlowRoast?.loading && (
                          <p role="status">Writing your jokes…</p>
                        )}
                        {currentGlowRoast?.open &&
                          !currentGlowRoast?.loading && (
                            <div className="glow-roast" role="status">
                              {currentGlowRoast.error && (
                                <p>{currentGlowRoast.error}</p>
                              )}
                              {currentGlowRoast.jokes?.map((joke, index) => (
                                <p key={index}>{joke}</p>
                              ))}
                            </div>
                          )}
                      </>
                    )}
                    <hr />
                    <Button
                      secondary
                      className="final-action-emphasis"
                      onClick={() => go("layout")}
                    >
                      Compare Versions Again
                    </Button>
                    <button className="text-button" onClick={() => go("edit")}>
                      <PenLine size={16} />
                      Return to section editing
                    </button>
                    <p className="microcopy">
                      <ShieldCheck size={14} />
                      Nothing is emailed until you confirm.
                    </p>
                    {demo && (
                      <Notice>
                        This is the sample resume. Upload your own resume to
                        create your personal version.
                      </Notice>
                    )}
                    {error && <Notice error>{error}</Notice>}
                  </aside>
                </div>
              </div>
            </>
          )}
          {page === "experience-feedback" && coachEdition && !demo && (
            <>
              <PageTitle
                eyebrow="THANK YOU FOR YOUR FEEDBACK"
                title="One last thing"
                subtitle={
                  betaEdition
                    ? "Your review is complete. Tell us how the app and AI feedback worked for you."
                    : "Your review is complete. Tell us how helpful the AI feedback was."
                }
              />
              <ExperienceRatingPanel
                beta={access.accessKind === "beta-pass"}
                submitted={betaRated}
                onSubmitted={() => setBetaRated(true)}
              />
              <div className="page-actions">
                <Button secondary onClick={() => go("invitation")}>
                  Back to the resume builder
                </Button>
              </div>
            </>
          )}
        </main>
        <footer>
          <Divider />
          <div>
            A software product by <strong>PNB Paws LLC</strong>
          </div>
          <div className="footer-links">
            <LegalLinks onHowItWorks={() => setModal("how")} />
            {stage > 0 && stage < 9 && (
              <>
                <span>·</span>
                <button onClick={() => setModal("clear")}>
                  Clear my session
                </button>
              </>
            )}
          </div>
        </footer>
      </div>
      {modal === "how" && <HowItWorksDialog onClose={() => setModal(null)} />}
      {modal === "about" && <AboutDialog onClose={() => setModal(null)} />}
      {page === "preferences" && linkWarning.length > 0 && (
        <Modal title="Project links matter" onClose={() => setLinkWarning([])}>
          <div className="project-link-caution" role="alert">
            <AlertCircle size={25} aria-hidden="true" />
            <span>
              <strong>
                Entry and mid-level tech resumes need visible project proof.
              </strong>{" "}
              {linkWarning.join(", ")}{" "}
              {linkWarning.length === 1 ? "has" : "have"} no repo or demo link.
              This can lower the rating, but links are optional.
            </span>
          </div>
          <div className="page-actions">
            <Button secondary onClick={() => setLinkWarning([])}>
              Add a link
            </Button>
            <Button onClick={() => beginBuild(true)}>
              Continue without links
            </Button>
          </div>
        </Modal>
      )}
      {modal === "privacy" && (
        <Modal
          title="Your resume stays in your control"
          onClose={() => setModal(null)}
        >
          <div className="prose">
            <p>
              When you submit a resume, its readable text and available layout
              details are sent to the configured external review service.
              Text-based PDF reviews also send images of the first two pages;
              scanned PDFs send page images for text reading. For a PNG or JPG,
              the image is sent for text reading and review. Word documents send
              extracted text; their exact page count and visual formatting
              cannot be verified in this version.
            </p>
            <p>
              Resume files and drafts are generally processed for your current
              session. Progress saves automatically while you work, and Save
              progress saves immediately. Your file, answers, edits, and current
              step stay in your account until you replace the save or delete the
              account. Coaches can continue saved work for their last five
              clients and download their uploaded resumes, feedback, and
              finished documents from Recent reviews. If you request human
              resume help, an editable Word copy is stored behind a private link
              for up to seven days so the review team can access it.
            </p>
            <p>
              After a review, the app emails the owner the resume user’s entered
              email, any coach’s sign-in email, career level, and format and
              overall ratings. Starting the resume builder sends a separate
              notice with those details. In the coach and beta versions, the
              owner also receives the uploaded resume, AI feedback PDF, builder
              draft, and selected finished resume by email.
            </p>
            <p>
              The review provider may retain request data under its own data
              policy; app-level deletion does not remove provider logs.{" "}
              <a
                href="https://platform.openai.com/docs/guides/your-data"
                target="_blank"
                rel="noreferrer"
              >
                Read the provider’s data policy.
              </a>
            </p>
            <p>
              Email delivery sends your PDF through the configured mail
              provider.{" "}
              {!coachEdition &&
                "A human-review request shares it with the review team only after you confirm."}{" "}
              Delivered emails remain in their recipients’ mailboxes.
            </p>
          </div>
          <Button onClick={() => setModal(null)}>Got It</Button>
        </Modal>
      )}
      {modal === "clear" && (
        <Modal title="Clear this session?" onClose={() => setModal(null)}>
          <p>
            This will discard your uploaded file, draft, answers, and
            preferences from this browser tab. Emails already sent are
            unaffected.
          </p>
          <Button
            onClick={() => {
              setFile(null);
              setReview(null);
              setProposal(null);
              setProposalApproved(false);
              setResume(clone(demoMode ? demoResume : referenceResume));
              setEmail("");
              setEmailTo("");
              setProfession("");
              setLevel("");
              setAnswers({}); setNewExperience({}); setNewProjects([]);
              setPreferences([]);
              setTargetRole(demoMode ? demoJobTitle : "");
              setSelectedExperience(null);
              setSelectedProjects(null);
              setProjectStacks({});
              setRepoChecks({});
              setProjectLinks({});
              setLinkWarning([]);
              setRoleDetails({});
              setFont("Calibri");
              setLinkColor("blue");
              setProjectFormat("compact");
              setDates("right");
              setLayout("template");
              setConsent(false);
              setModal(null);
              go("home");
            }}
          >
            Clear My Session
          </Button>
        </Modal>
      )}
      {["email", "human"].includes(modal) && (
        <Modal
          title={
            modal === "email" ? "Email yourself a copy" : "A human helping hand"
          }
          onClose={() => setModal(null)}
        >
          <p>
            {modal === "email"
              ? "We’ll attach your completed resume as a PDF."
              : "We’ll send the configured review team your editable Word document, PDF, and a private Word download link that expires in 7 days."}
          </p>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              deliver(modal);
            }}
          >
            <div className="field">
              <label htmlFor="deliveryEmail">
                {modal === "human"
                  ? "Where should we reply?"
                  : "Send your PDF to"}
              </label>
              <input
                id="deliveryEmail"
                type="email"
                required
                readOnly={
                  Boolean(access.email) || (coachEdition && !betaEdition)
                }
                value={emailTo}
                onChange={(e) => setEmailTo(e.target.value)}
              />
            </div>
            <label className="check-row">
              <input
                type="checkbox"
                required
                checked={deliveryConsent}
                onChange={(e) => setDeliveryConsent(e.target.checked)}
              />
              <span>
                {modal === "human"
                  ? "I agree to share my Word document, PDF, reply email, and a private 7-day download link with the review team."
                  : "I’ve reviewed my resume and want it emailed to this address."}
              </span>
            </label>
            {error && <Notice error>{error}</Notice>}
            {message ? (
              <Notice>{message}</Notice>
            ) : (
              <Button
                disabled={busy || !deliveryConsent || !status?.emailReady}
              >
                {busy ? (
                  <LoaderCircle className="spin" size={17} />
                ) : (
                  <Mail size={17} />
                )}{" "}
                {modal === "email" ? "Send PDF" : "Request Human Help"}
              </Button>
            )}
            {!status?.emailReady && (
              <p className="microcopy">
                Email delivery will become available once the mail service is
                connected.
              </p>
            )}
          </form>
        </Modal>
      )}
      {jobPrepOpen && (
        <Modal
          title="Your job prep sheet"
          onClose={() => setJobPrepOpen(false)}
        >
          {!jobDescription.trim() ? (
            <p>
              Add a job posting while customizing your resume to create your job
              prep sheet.
            </p>
          ) : (
            <>
              <JobPrepSheet
                sheet={
                  jobPrep?.source === jobDescription.trim()
                    ? jobPrep.sheet
                    : null
                }
                loading={jobPrepLoading}
                error={jobPrepError}
                onRetry={makeJobPrep}
              />
              {jobPrepLoading && (
                <p role="status">
                  Researching company details and public interview reports in
                  two passes…
                </p>
              )}
              {jobPrep?.source === jobDescription.trim() && (
                <Button
                  secondary
                  disabled={jobPrepLoading}
                  onClick={() => makeJobPrep(true)}
                >
                  Refresh deep-dive research
                </Button>
              )}
              {jobPrep?.source === jobDescription.trim() && (
                <CoverLetterPreview
                  text={jobPrepDocumentText(jobPrep.sheet)}
                  label="Job prep sheet"
                  endpoint="/job-prep/document"
                  filename="job-prep-sheet.pdf"
                />
              )}
            </>
          )}
        </Modal>
      )}
      {coverOpen && (
        <Modal title="Your cover letter" onClose={() => setCoverOpen(false)}>
          <p>
            Build a natural, specific letter from your finished resume. Review
            and edit it before sending.
          </p>
          <label htmlFor="cover-job">Job description</label>
          <textarea
            id="cover-job"
            rows={4}
            value={jobDescription}
            onChange={(e) => setJobDescription(e.target.value)}
          />
          {coverLetter && !coverRemaking && (
            <Button secondary onClick={() => setCoverRemaking(true)}>
              Update cover letter
            </Button>
          )}
          {coverRemaking && (
            <>
              <label htmlFor="cover-suggestions">
                What would you like included or changed? (optional)
              </label>
              <textarea
                id="cover-suggestions"
                rows={3}
                maxLength={2000}
                value={coverSuggestions}
                onChange={(e) => setCoverSuggestions(e.target.value)}
                placeholder="For example, emphasize my teamwork and keep the tone warm and professional."
              />
            </>
          )}
          {(!coverLetter || coverRemaking) && (
            <Button
              disabled={coverBusy || !jobDescription.trim()}
              onClick={async () => {
                setCoverBusy(true);
                setCoverError("");
                try {
                  const result = await post("/cover-letter", {
                    resume: page === "edit" ? resume : selectedResume,
                    listing: jobDescription,
                    suggestions: coverSuggestions,
                  });
                  setCoverLetter(result.text);
                  setCoverRemaking(false);
                } catch (e) {
                  setCoverError(e.message);
                } finally {
                  setCoverBusy(false);
                }
              }}
            >
              {coverBusy
                ? "Writing your letter…"
                : coverRemaking
                  ? "Submit suggestions"
                  : "Create cover letter"}
            </Button>
          )}
          {coverError && <p role="alert">{coverError}</p>}
          {coverLetter && (
            <>
              <label htmlFor="cover-letter">Edit your letter</label>
              <textarea
                id="cover-letter"
                rows={14}
                value={coverLetter}
                onChange={(e) => setCoverLetter(e.target.value)}
              />
              <CoverLetterPreview
                text={coverLetter}
                contact={(page === "edit" ? resume : selectedResume).contact}
                onEdit={() => setCoverRemaking(true)}
              />
            </>
          )}
        </Modal>
      )}
      {editSection && (
        <EditModal
          level={reviewLevel}
          technicalOptions={technicalOptions}
          githubUrl={
            demoMode ? "" : profileGithub || resume.contact?.github || ""
          }
          section={editSection}
          resume={resume}
          onClose={() => setEditSection(null)}
          onSave={(next) => {
            if (editSection === "projects")
              setProjectLinks((current) => ({
                ...current,
                ...projectLinksFromEdits(
                  review?.resume?.projects || "",
                  next.projects,
                ),
              }));
            setResume(next);
            setEditSection(null);
          }}
        />
      )}
    </>
  );
}
function HowExample({ step }) {
  if (step === 0)
    return (
      <div className="how-example">
        <small>Accepted resume files</small>
        <strong>PDF · Word · JPG · PNG</strong>
        <p>Preview the pages or extracted text before the review starts.</p>
      </div>
    );
  if (step === 1 || step === 2) {
    const format = step === 1;
    return (
      <div className="how-example">
        <small>Illustrative example</small>
        <strong>{format ? "Format score" : "Overall score"}</strong>
        <b className="example-score">
          {format ? "8" : "7"}
          <span>/10</span>
        </b>
        <div className="example-meter" aria-hidden="true">
          <span style={{ width: format ? "80%" : "70%" }} />
        </div>
        <div className="example-feedback">
          <strong>Feedback</strong>
          <ul>
            {format ? (
              <>
                <li>Clear section structure</li>
                <li>Consistent formatting</li>
                <li>Tighten spacing and summary length</li>
              </>
            ) : (
              <>
                <li>Relevant technical experience</li>
                <li>Clear action verbs</li>
                <li>Clarify outcomes where supported by facts</li>
              </>
            )}
          </ul>
        </div>
      </div>
    );
  }
  if (step === 3)
    return (
      <div className="how-example example-checklist">
        <small>Example preferences</small>
        {[
          "Choose what to highlight",
          "Add optional profile and project links",
          "Tailor to a job listing (optional)",
          "Create a matching cover letter (optional)",
        ].map((label) => (
          <div key={label}>
            <Check size={16} aria-hidden="true" />
            <span>{label}</span>
          </div>
        ))}
      </div>
    );
  if (step === 4)
    return (
      <div className="how-example">
        <ShieldCheck size={23} aria-hidden="true" />
        <strong>Edit your resume and cover letter.</strong>
        <p>
          Review each section alongside the live resume preview. View your cover
          letter below it, or select Remake cover letter to submit suggestions.
        </p>
      </div>
    );
  return (
    <div className="how-example">
      <FileText size={23} aria-hidden="true" />
      <strong>Compare your customized resume and our wording option.</strong>
      <p>
        View them side by side, choose one, and download PDF or Word or email
        yourself a PDF.
      </p>
    </div>
  );
}
function PageTitle({ eyebrow, title, subtitle }) {
  return (
    <header className="page-title">
      <span className="eyebrow">{eyebrow}</span>
      <h1>{title}</h1>
      {subtitle && <p>{subtitle}</p>}
      <Divider />
    </header>
  );
}
function EditModal({
  section,
  resume,
  githubUrl,
  level,
  technicalOptions,
  onSave,
  onClose,
}) {
  const [draft, setDraft] = useState(clone(resume));
  const [education, setEducation] = useState(() =>
    parseEducation(resume.education),
  );
  const [educationError, setEducationError] = useState("");
  const [showDemoAdvice, setShowDemoAdvice] = useState(false);
  const [editLinks, setEditLinks] = useState(() =>
    Object.fromEntries(
      projectList(resume.projects).map((project, index) => [
        index,
        projectLinkUrls(project).join(" | "),
      ]),
    ),
  );
  const [projectError, setProjectError] = useState("");
  const [projectAutofillBusy, setProjectAutofillBusy] = useState(false);
  const [projectAutofillNote, setProjectAutofillNote] = useState("");
  const projectLinksEdited = useRef(new Set());
  const editorActive = useRef(true);
  async function autofillEditorProjectLinks() {
    setProjectAutofillBusy(true);
    try {
      const projects = projectList(draft.projects);
      const found = await discoverProjectLinks(
        projects,
        normalizeWebLink(githubUrl || draft.contact?.github || ""),
        "",
      );
      if (!editorActive.current) return;
      setEditLinks((current) => {
        const next = { ...current };
        for (const [index, links] of Object.entries(found))
          next[index] = mergeProjectLinkAutofill(
            current[index] ?? projectLinkUrls(projects[index]).join(" | "),
            links,
            projectLinksEdited.current.has(Number(index)),
          );
        return next;
      });
      setProjectAutofillNote(
        Object.keys(found).length
          ? "Matching public GitHub links filled in where available. Both fields are optional."
          : "No matching public links found. You can leave both fields blank.",
      );
    } catch {
      if (editorActive.current)
        setProjectAutofillNote(
          "Public GitHub links could not be checked. You can still save with both fields blank.",
        );
    } finally {
      if (editorActive.current) setProjectAutofillBusy(false);
    }
  }
  useEffect(() => {
    editorActive.current = true;
    if (section === "projects" && technicalOptions)
      autofillEditorProjectLinks();
    return () => {
      editorActive.current = false;
    };
  }, []);

  const updateEducation = (index, changes) =>
    setEducation((current) =>
      current.map((entry, position) =>
        position === index ? { ...entry, ...changes } : entry,
      ),
    );
  return (
    <Modal
      title={"Edit " + resumeSections(level).find(([k]) => k === section)[1]}
      onClose={onClose}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (section === "education") {
            const problem = education.map(educationEntryError).find(Boolean);
            if (problem) setEducationError(`HIGHLY RECOMMENDED: ${problem}`);
            onSave({ ...draft, education: formatEducation(education) });
          } else if (section === "projects" && technicalOptions) {
            const projects = projectList(draft.projects);
            const links = projects.map(
              (project, index) =>
                editLinks[index] ?? projectLinkUrls(project).join(" | "),
            );
            const problem = links
              .map((entry) => projectLinkError(entry))
              .find(Boolean);
            if (problem) setProjectError(`HIGHLY RECOMMENDED: ${problem}`);
            onSave({
              ...draft,
              projects: normalizeProjectHeadings(
                projects
                  .map((project, index) =>
                    replaceProjectLinks(project, links[index]),
                  )
                  .join("\n\n"),
              ),
            });
          } else onSave(draft);
        }}
      >
        {section === "contact" ? (
          <>
            {Object.entries(draft.contact).map(([k, v]) => (
              <div className="field" key={k}>
                <label htmlFor={"edit-" + k}>
                  {
                    {
                      name: "First and last name",
                      location: "City and state or region (no street address)",
                      email: "Email",
                      phone: "Phone",
                      linkedin: "LinkedIn URL",
                      github: "GitHub URL",
                      portfolio:
                        "Portfolio URL (optional, strongly recommended)",
                    }[k]
                  }
                </label>
                <input
                  id={"edit-" + k}
                  type={k === "email" ? "email" : "text"}
                  value={v}
                  onChange={(e) =>
                    setDraft({
                      ...draft,
                      contact: { ...draft.contact, [k]: e.target.value },
                    })
                  }
                />
              </div>
            ))}
            {!draft.contact.portfolio?.trim() && (
              <p className="microcopy">
                <strong>Strongly recommended:</strong> Add a portfolio or
                work-sample link so employers can see what you built. You can
                continue without one.
              </p>
            )}
          </>
        ) : section === "certifications" ? (
          <CertificationFields
            value={draft.certifications || ""}
            onChange={(value) => setDraft({ ...draft, certifications: value })}
          />
        ) : section === "education" ? (
          <div className="education-editor">
            <p className="microcopy">
              Add your school and degree or program. Dates are optional.
            </p>
            {education.map((entry, index) => (
              <div className="education-entry" key={index}>
                <div className="field">
                  <label htmlFor={`education-details-${index}`}>
                    School and degree or program
                  </label>
                  <textarea
                    id={`education-details-${index}`}
                    rows={1}
                    value={entry.details}
                    placeholder="School — degree or program"
                    onChange={(event) => {
                      const details = event.target.value;
                      const imported = parseEducation(details)[0];
                      updateEducation(
                        index,
                        imported?.year ? imported : { details },
                      );
                    }}
                  />
                </div>
                <div className="field">
                  <label htmlFor={`education-status-${index}`}>
                    Education status (optional)
                  </label>
                  <select
                    id={`education-status-${index}`}
                    value={entry.status}
                    onChange={(event) =>
                      updateEducation(index, {
                        status: event.target.value,
                        month: "",
                        year: "",
                      })
                    }
                  >
                    <option value="">Leave off resume</option>
                    <option value="graduated">Graduated</option>
                    <option value="progress">Still studying</option>
                    <option value="incomplete">Did not graduate</option>
                  </select>
                </div>
                {(entry.status === "graduated" ||
                  entry.status === "progress") && (
                  <div className="education-date">
                    <div className="field">
                      <label htmlFor={`education-month-${index}`}>
                        {entry.status === "graduated"
                          ? "Graduation month (optional)"
                          : "Expected graduation month (optional)"}
                      </label>
                      <select
                        id={`education-month-${index}`}
                        value={entry.month}
                        onChange={(event) =>
                          updateEducation(index, { month: event.target.value })
                        }
                      >
                        <option value="">No month</option>
                        {educationMonths.map((month) => (
                          <option key={month} value={month}>
                            {month}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="field">
                      <label htmlFor={`education-year-${index}`}>
                        {entry.status === "graduated"
                          ? "Graduation year (optional)"
                          : "Expected graduation year (optional)"}
                      </label>
                      <input
                        id={`education-year-${index}`}
                        type="text"
                        inputMode="numeric"
                        maxLength={4}
                        value={entry.year}
                        placeholder="YYYY"
                        onChange={(event) =>
                          updateEducation(index, {
                            year: event.target.value.replace(/\D/g, ""),
                          })
                        }
                      />
                    </div>
                  </div>
                )}
                {entry.status === "incomplete" && (
                  <p className="microcopy">
                    No graduation date will appear for this entry.
                  </p>
                )}
                {education.length > 1 && (
                  <button
                    type="button"
                    className="text-button"
                    onClick={() =>
                      setEducation((current) =>
                        current.filter((_, position) => position !== index),
                      )
                    }
                  >
                    Remove this school
                  </button>
                )}
              </div>
            ))}
            <button
              type="button"
              className="text-button"
              onClick={() =>
                setEducation((current) => [
                  ...current,
                  { details: "", status: "", month: "", year: "" },
                ])
              }
            >
              Add another school
            </button>
            {educationError && <Notice error>{educationError}</Notice>}
          </div>
        ) : (
          <>
            <textarea
              aria-label={"Edit " + section}
              className="section-textarea"
              rows={section === "summary" ? 5 : 13}
              value={draft[section]}
              onChange={(e) =>
                setDraft({ ...draft, [section]: e.target.value })
              }
            />
            {section === "summary" && (
              <small>{countWords(draft.summary)} words · aim for 35–45</small>
            )}
            {section === "projects" && technicalOptions && (
              <div className="project-link-guidance">
                <p className="microcopy">
                  Separate projects with a blank line. Matching public GitHub
                  links fill automatically when available. Repo and Demo are
                  both optional.
                </p>
                <button
                  type="button"
                  className="secondary button"
                  disabled={projectAutofillBusy}
                  onClick={autofillEditorProjectLinks}
                >
                  {projectAutofillBusy
                    ? "Checking GitHub…"
                    : "Check GitHub again"}
                </button>
                {projectAutofillNote && (
                  <p role="status">{projectAutofillNote}</p>
                )}
                {projectList(draft.projects).map((project, index) => (
                  <div className="field" key={index}>
                    <h4>
                      Links for “{project.split("\n")[0].split("|")[0].trim()}”
                    </h4>
                    <ProjectLinkFields
                      id={`edit-project-links-${index}`}
                      value={
                        editLinks[index] ?? projectLinkUrls(project).join(" | ")
                      }
                      onChange={(value) => {
                        projectLinksEdited.current.add(index);
                        setEditLinks((current) => ({
                          ...current,
                          [index]: value,
                        }));
                        setProjectError("");
                      }}
                    />
                  </div>
                ))}
                {projectError && <p className="invalid">{projectError}</p>}
                {projectList(draft.projects).some(
                  (project) => !projectDemo(project),
                ) && (
                  <p className="microcopy">
                    <strong>Recommended:</strong> If you cannot share a live
                    demo, add a viewable cloud link to a screenshot or short
                    recording of the project running. This helps reviewers see
                    what you built.
                  </p>
                )}
                <button
                  type="button"
                  className="text-button"
                  aria-expanded={showDemoAdvice}
                  onClick={() => setShowDemoAdvice((current) => !current)}
                >
                  I don't have a demo link yet
                </button>
                {showDemoAdvice && (
                  <p className="microcopy">
                    Run your project locally and capture a short screen
                    recording or screenshot. Upload it to cloud storage and
                    paste a viewable link here. You can also host a suitable web
                    project with GitHub Pages. A repo or demo link helps
                    reviewers see the work, but neither is required.
                  </p>
                )}
              </div>
            )}
          </>
        )}
        <div className="page-actions">
          <Button type="button" secondary onClick={onClose}>
            Cancel
          </Button>
          <Button>
            Save Section
            <Check size={17} />
          </Button>
        </div>
      </form>
    </Modal>
  );
}

document.documentElement.toggleAttribute(
  "data-native-webview",
  /Android/i.test(navigator.userAgent) && /\bwv\b/.test(navigator.userAgent),
);
createRoot(document.getElementById("root")).render(
  <>
    <App />
    <a
      className="contact-support"
      href="mailto:resume.prospective@pnbpaws.com?subject=Resume%20Prospective%20Support"
    >
      <Mail size={16} aria-hidden="true" />
      Contact Support
    </a>
  </>,
);
