import React, { useEffect, useState } from "react";
import { request, post } from "./api";

export function AppSumoRedemption({ access, refresh }) {
  const [open, setOpen] = useState(
    window.location.pathname.startsWith("/redeem"),
  );
  const [code, setCode] = useState(""),
    [busy, setBusy] = useState(false),
    [message, setMessage] = useState("");
  if (
    access.role === "admin" ||
    (access.allowed && !window.location.pathname.startsWith("/redeem"))
  )
    return null;
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      await post("/appsumo/redeem", { code: code.trim() });
      setCode("");
      setMessage(
        "Your code is redeemed. Use this same account on the web and Samsung/Android app.",
      );
      await refresh();
    } catch (e) {
      setMessage(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="card appsumo-redemption">
      {!open ? (
        <button className="text-button" onClick={() => setOpen(true)}>
          Redeem an AppSumo code
        </button>
      ) : (
        <>
          <h2>Redeem your AppSumo code</h2>
          <p>
            One code gives one account 30 days of access to all features on the
            web and Samsung/Android app. Your access starts when you redeem it.
          </p>
          <p>
            Resume ratings, assistant resume building, detailed feedback,
            positive feedback, roasts, cover letters, interview prep, and
            PDF/Word downloads are included, with no review or build quota.
            Normal request limits still apply.
          </p>
          {!access.registered ? (
            <p>
              Create an account and verify your email above, or log in to your
              existing account, then enter your purchase code here.
            </p>
          ) : access.state === "verify-email" ? (
            <p>Verify your email before redeeming your code.</p>
          ) : access.plan === "appsumo" ? (
            <p>
              Your AppSumo code is linked to this account. Codes cannot be
              stacked or reused to extend access.
            </p>
          ) : (
            <form onSubmit={submit}>
              <label htmlFor="appsumo-code">AppSumo purchase code</label>
              <input
                id="appsumo-code"
                autoComplete="off"
                spellCheck={false}
                required
                minLength={16}
                maxLength={200}
                value={code}
                onChange={(e) => setCode(e.target.value)}
              />
              <button className="primary button" disabled={busy}>
                {busy ? "Redeeming…" : "Activate 30-day access"}
              </button>
            </form>
          )}
          <p>
            Need help?{" "}
            <a href="mailto:resume.prospective@pnbpaws.com">
              resume.prospective@pnbpaws.com
            </a>
          </p>
        </>
      )}
      {message && <p role="status">{message}</p>}
    </section>
  );
}
