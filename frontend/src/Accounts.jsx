import React, { useEffect, useState } from "react";
import { request, post } from "./api";
export function AccountEntry({ individual, refresh }) {
  const [mode, setMode] = useState(""),
    [flow, setFlow] = useState(""),
    [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [code, setCode] = useState(""),
    [busy, setBusy] = useState(false),
    [message, setMessage] = useState("");
  const [keepLoggedIn, setKeepLoggedIn] = useState(false);
  const edition = individual ? "individual" : "coach";
  const [resetToken, setResetToken] = useState(
    () =>
      new URLSearchParams(window.location.hash.slice(1)).get("account-reset") ||
      "",
  );
  useEffect(() => {
    if (resetToken) setMode("reset");
  }, []);
  function choose(next) {
    setMode(next);
    setFlow(next);
    setPassword("");
    setCode("");
    setMessage("");
  }
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      if (mode === "trial" || mode === "signup") {
        await post(mode === "trial" ? "/accounts/trial" : "/accounts/signup", {
          email,
          password,
          edition,
        });
        setPassword("");
        setMode("verify");
        setMessage(
          mode === "trial"
            ? "Check your email for your trial code. No owner approval is needed."
            : individual
              ? "Verify your email to finish submitting your access request."
              : "Verify your email first. The owner will then review your account request.",
        );
      } else if (mode === "code") {
        await post("/coach/code", {
          email,
          edition: individual ? "beta" : "coach",
        });
        setMode("verify");
        setMessage("Enter the code sent to your email.");
      } else if (mode === "verify") {
        const verified = await post("/coach/verify", {
          email,
          code: code.trim(),
          keepLoggedIn,
          edition: individual ? "beta" : "coach",
        });
        setCode("");
        await refresh(verified);
        setMessage(
          verified.allowed
            ? "Email verified. Opening your workspace."
            : verified.state === "pending"
              ? individual
                ? "Your email is verified. Your access request is being reviewed."
                : "Your email is verified. This account request is awaiting owner approval."
              : "Your email is verified, but access is not active. Please check the account status below.",
        );
      } else if (mode === "forgot") {
        await post("/accounts/forgot-password", { email });
        setMessage(
          "If this email has an eligible account, a password-reset link is on its way. Check your inbox and spam folder.",
        );
      } else if (mode === "reset") {
        await post("/accounts/reset-password", { token: resetToken, password });
        setPassword("");
        setResetToken("");
        window.history.replaceState(
          null,
          "",
          window.location.pathname + window.location.search,
        );
        setMode("login");
        setMessage("Password updated. Log in with your new password.");
      } else {
        await post("/accounts/login", { email, password, keepLoggedIn });
        setPassword("");
        await refresh();
      }
    } catch (e) {
      setMessage(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="card auth-card account-entry">
      <img
        className="signin-card-robot"
        src="/pnb-robot-transparent.png"
        alt=""
      />
      <div className="account-entry-options">
        {[
          ["trial", "Try Free Trial"],
          ["login", "Log In"],
          ["signup", "Create an Account"],
        ].map(([key, label]) => (
          <button
            key={key}
            type="button"
            className={`button account-choice account-choice-${key}`}
            aria-pressed={flow === key}
            disabled={busy}
            onClick={() => choose(key)}
          >
            {label}
          </button>
        ))}
      </div>
      {mode && (
        <>
          <h2>
            {mode === "forgot"
              ? "Reset your password"
              : mode === "reset"
                ? "Choose a new password"
                : mode === "trial"
                  ? "Try your free review and build"
                  : mode === "signup"
                    ? "Create your account"
                    : mode === "verify"
                      ? "Verify your email"
                      : mode === "code"
                        ? "Log in with an email code"
                        : "Log In"}
          </h2>
          <p>
            {mode === "trial"
              ? "Two free reviews and builds per email across the app and both websites. Verify with an email code; no approval needed."
              : mode === "signup"
                ? individual
                  ? "Create a password and verify your email to request access."
                  : "Create a password and verify your email. Ongoing access begins only after owner approval."
                : "Return to your account or continue your existing trial."}
          </p>
          <form onSubmit={submit}>
            {mode !== "reset" && (
              <label>
                Email
                <input
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </label>
            )}
            {mode === "verify" && (
              <label>
                Email code
                <input
                  required
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  pattern="[0-9]{6}"
                  maxLength={16}
                  onInvalid={() =>
                    setMessage(
                      "Enter the complete six-digit code from your email. You can paste it here.",
                    )
                  }
                  value={code}
                  onChange={(e) =>
                    setCode(e.target.value.replace(/\D/g, "").slice(0, 6))
                  }
                />
              </label>
            )}
            {(mode === "signup" || mode === "login" || mode === "reset") && (
              <label>
                Password
                <input
                  type="password"
                  required
                  minLength={mode !== "login" ? 8 : undefined}
                  maxLength={128}
                  autoComplete={
                    mode === "login" ? "current-password" : "new-password"
                  }
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                {mode !== "login" && (
                  <small>At least 8 characters, 2 digits, and 1 symbol.</small>
                )}
              </label>
            )}
            {["login", "code", "verify"].includes(mode) && (
              <label className="check-row keep-signin">
                <input
                  type="checkbox"
                  checked={keepLoggedIn}
                  onChange={(e) => setKeepLoggedIn(e.target.checked)}
                />
                <span>
                  Keep me logged in
                  <small>
                    Until you sign out or your access ends. Use only on your own
                    device.
                  </small>
                </span>
              </label>
            )}
            <button className="primary button" disabled={busy}>
              {busy
                ? "Please wait…"
                : mode === "forgot"
                  ? "Email Reset Link"
                  : mode === "reset"
                    ? "Save New Password"
                    : mode === "signup"
                      ? "Request account"
                      : mode === "trial" || mode === "code"
                        ? "Email Me a Code"
                        : mode === "verify"
                          ? "Verify Code"
                          : "Log In"}
            </button>
          </form>
          {mode === "login" && (
            <button
              type="button"
              className="text-button"
              onClick={() => choose("forgot")}
            >
              Forgot password?
            </button>
          )}
          {mode === "login" && (
            <button
              type="button"
              className="text-button"
              onClick={() => choose("code")}
            >
              Use an email code instead / Continue my free trial
            </button>
          )}
          {mode === "verify" && (
            <button
              type="button"
              className="text-button"
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                try {
                  await post("/coach/code", {
                    email,
                    edition: individual ? "beta" : "coach",
                  });
                  setMessage("A new code was sent.");
                } catch (e) {
                  setMessage(e.message);
                } finally {
                  setBusy(false);
                }
              }}
            >
              Resend Code
            </button>
          )}
        </>
      )}
      {message && <p role="status">{message}</p>}
    </section>
  );
}
export function SubscriptionRequest({
  access,
  refresh,
  onRestore,
  individual = false,
}) {
  if (access.plan === "appsumo")
    return (
      <section className="account-plan">
        <p>
          <strong>AppSumo access {access.allowed ? "active" : "ended"}</strong>{" "}
          | {access.allowed ? "Expires" : "Ended"}{" "}
          {new Date(access.expiresAt).toLocaleDateString()}
        </p>
        <p>
          All features on the web and Samsung/Android app, using the same
          account
        </p>
      </section>
    );
  return (
    <StandardSubscriptionRequest
      individual={individual}
      access={access}
      refresh={refresh}
      onRestore={onRestore}
    />
  );
}
function StandardSubscriptionRequest({
  access,
  refresh,
  onRestore,
  individual,
}) {
  const [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false),
    [creating, setCreating] = useState(false),
    [newPassword, setNewPassword] = useState("");
  if (!access.registered || access.plan === "subscribed") return null;
  return (
    <section className="account-plan">
      <p>
        {access.plan === "subscribed"
          ? "Subscription approved"
          : access.plan === "trial"
            ? "Free trial: two reviews and builds"
            : access.state === "verify-email"
              ? "Verify your email using an email code below."
              : individual
                ? "Your access request is being reviewed."
                : "Your account is awaiting owner approval."}
      </p>
      {access.plan === "trial" && (
        <p>
          {access.reviewsRemaining ?? 0} reviews and{" "}
          {access.buildsRemaining ?? 0} builds remaining.
        </p>
      )}
      {access.reviewUsed && onRestore && (
        <button className="secondary button" onClick={onRestore}>
          Resume saved trial
        </button>
      )}
      {access.plan !== "subscribed" && (
        <button
          className="secondary button"
          disabled={
            busy ||
            access.subscriptionRequested ||
            access.state === "verify-email"
          }
          onClick={async () => {
            setBusy(true);
            try {
              await post("/accounts/subscribe", {});
              await refresh();
              setMessage(
                individual
                  ? "Your request for continued access was sent. No payment is collected."
                  : "Your subscription request was sent for approval. No payment is collected.",
              );
            } catch (e) {
              setMessage(e.message);
            } finally {
              setBusy(false);
            }
          }}
        >
          {access.subscriptionRequested
            ? individual
              ? "Access request pending"
              : "Subscription awaiting approval"
            : individual
              ? "Request continued access"
              : "Subscribe"}
        </button>
      )}
      {access.plan !== "subscribed" && access.state !== "verify-email" && (
        <button
          type="button"
          className="text-button"
          onClick={() => setCreating(!creating)}
        >
          Create an Account / Set Password
        </button>
      )}
      {creating && (
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            setBusy(true);
            try {
              await post("/accounts/create-password", {
                password: newPassword,
              });
              setNewPassword("");
              setCreating(false);
              await refresh();
              setMessage(
                individual
                  ? "Password saved. Your access request is being reviewed."
                  : "Password saved. Your account request is awaiting owner approval.",
              );
            } catch (e) {
              setMessage(e.message);
            } finally {
              setBusy(false);
            }
          }}
        >
          <label>
            New password
            <input
              type="password"
              required
              minLength={8}
              maxLength={128}
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </label>
          <small>At least 8 characters, 2 digits, and 1 symbol.</small>
          <button disabled={busy}>
            {individual ? "Create my account" : "Request Account Approval"}
          </button>
        </form>
      )}
      {message && <p role="status">{message}</p>}
    </section>
  );
}
