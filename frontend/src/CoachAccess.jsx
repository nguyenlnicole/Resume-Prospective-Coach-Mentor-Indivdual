import { AppSumoRedemption } from "./AppSumo";
import { AccountEntry, SubscriptionRequest } from "./Accounts";
import { AboutButton, HowButton } from "./AboutUs";
import React, { useEffect, useState } from "react";
import { request, post } from "./api";
import LegalLinks from "./LegalLinks";
import { storedEmail, saveRememberedEmail } from "./remember-email";

export function CoachSignIn({
  access,
  refresh,
  onHowItWorks,
  onAbout,
  features,
}) {
  const betaHost =
    import.meta.env.VITE_APP_EDITION === "beta" ||
    ["betarp.pnbpaws.com", "resumep.pnbpaws.com"].includes(
      window.location.hostname,
    );
  const emailKey = betaHost
    ? "pnb-remember-beta-email"
    : "pnb-remember-coach-email";
  const [email, setEmail] = useState(() => storedEmail(emailKey));
  const [rememberEmail, setRememberEmail] = useState(() =>
    Boolean(storedEmail(emailKey)),
  );
  useEffect(
    () => saveRememberedEmail(emailKey, email, rememberEmail),
    [emailKey, email, rememberEmail],
  );
  const [code, setCode] = useState("");
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [requested, setRequested] = useState(false);
  useEffect(() => {
    if (!requested) return;
    let active = true;
    const check = async () => {
      try {
        const state = await request("/coach/request-status");
        if (active && state.approved) {
          setRequested(false);
          setSent(true);
        }
      } catch {}
    };
    const timer = setInterval(check, 8000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [requested]);
  const messages = {
    pending: betaHost
      ? "Your email is verified, but this address has not been assigned resume access yet."
      : "Your email is verified. Your coach or mentor account is waiting for the administrator’s approval.",
    "not-started": betaHost
      ? "Your resume access has not been assigned yet."
      : "You’re approved. Ask the administrator to assign your access.",
    expired: `Your ${betaHost ? "individual" : "coach and mentor"} access period has ended. Resume tools are no longer available.`,
    revoked: `Your ${betaHost ? "individual" : "coach and mentor"} access has been revoked. Please contact the app owner.`,
    offline:
      "We couldn’t connect to the PNB service. Check your connection and try again.",
  };
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (!sent) {
        await post("/coach/code", {
          email,
          edition: betaHost ? "beta" : "coach",
        });
        setSent(true);
      } else {
        await post("/coach/verify", {
          email,
          code,
          edition: betaHost ? "beta" : "coach",
        });
        setCode("");
        await refresh();
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function checkAccess() {
    setBusy(true);
    setError("");
    try {
      if (betaHost && access.role === "coach")
        await post("/beta/pass/activate", {});
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function requestCoachAccess() {
    if (!email.trim()) {
      setError("Enter your email first.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const result = await post("/coach/request-access", {
        email: email.trim(),
        edition: betaHost ? "beta" : "coach",
      });
      if (result.approved) setSent(true);
      else setRequested(true);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="account-page">
      <div className={`coach-screen${betaHost ? " beta-sign-in" : ""}`}>
        <img
          className="coach-resume-mascot"
          src="/pnb-resume-prospective-icon.png"
          alt=""
        />
        <img
          className="resume-top-icon"
          src="/pnb-resume-prospective-icon.png"
          alt=""
        />
        {betaHost && <p className="eyebrow">Individual</p>}
        <h1>Resume Prospective</h1>
        <p className="coach-subtitle">
          <em>Ai Resume Rater &amp; Assistant Builder</em>
        </p>
        <p className="ai-provider-note">AI assistance powered by OpenAI.</p>
        <div className="signin-entry-row">
          <AboutButton onClick={onAbout} />
          <AccountEntry individual={betaHost} refresh={refresh} />
          <HowButton className="signin-how-control" onClick={onHowItWorks} />
        </div>
        {!betaHost && <AppSumoRedemption access={access} refresh={refresh} />}
        <SubscriptionRequest
          individual={betaHost}
          access={access}
          refresh={refresh}
        />
        {access.registered && (
          <button
            className="text-button"
            onClick={async () => {
              await post("/coach/logout", {});
              await refresh();
            }}
          >
            Sign Out
          </button>
        )}

        <a className="walkthrough-entry" href="/?walkthrough=sample">
          Explore Demo
        </a>
        {features}
      </div>
      <footer className="account-footer">
        <p>A software product by PNB Paws LLC</p>
        <LegalLinks className="coach-legal-links" onHowItWorks={onHowItWorks} />
      </footer>
    </div>
  );
}
