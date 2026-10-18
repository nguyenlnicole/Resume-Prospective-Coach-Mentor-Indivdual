import React, { useEffect, useRef, useState } from "react";
import { Check, ShieldCheck } from "lucide-react";
import "./resume-credits.css";

const sdkUrl =
  "https://www.paypal.com/sdk/js?client-id=BAAWMC4qwRiProwIl0nrqPToN5ovpFAFbNfi5lQDvkO6GJsG-yYjjBtBnCznhNM3XN_jCNS3Q8dawMxZnw&components=hosted-buttons&enable-funding=venmo&currency=USD";
let sdkPromise;
let nextMountId = 0;
function loadPayPal() {
  if (window.paypal?.HostedButtons) return Promise.resolve(window.paypal);
  if (!sdkPromise)
    sdkPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = sdkUrl;
      script.async = true;
      script.onload = () =>
        window.paypal?.HostedButtons
          ? resolve(window.paypal)
          : reject(new Error("PayPal checkout could not load."));
      script.onerror = () => {
        script.remove();
        reject(new Error("PayPal checkout could not load."));
      };
      document.head.appendChild(script);
    }).catch((error) => {
      sdkPromise = null;
      throw error;
    });
  return sdkPromise;
}

export default function ResumeCreditsCard() {
  const container = useRef(null);
  const dialog = useRef(null);
  const [checkout, setCheckout] = useState(false);
  const [status, setStatus] = useState("loading");
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    if (!checkout) return;
    let active = true;
    const mount = document.createElement("div");
    mount.id = `resume-credits-paypal-${++nextMountId}`;
    container.current.appendChild(mount);
    setStatus("loading");
    loadPayPal()
      .then(async (paypal) => {
        if (!active) return;
        await paypal
          .HostedButtons({ hostedButtonId: "8S5KUL4BUPENW" })
          .render(`#${mount.id}`);
        if (active) setStatus("ready");
      })
      .catch(() => {
        if (active) setStatus("error");
      });
    return () => {
      active = false;
      mount.remove();
    };
  }, [retry, checkout]);
  return (
    <section
      className="resume-credits-card"
      aria-labelledby="resume-credits-title"
    >
      <div className="credits-mascot">
        <img src="/pnb-robot-transparent.png" alt="" />
      </div>
      <p className="credits-eyebrow">YOUR NEXT CHAPTER</p>
      <h1 id="resume-credits-title">
        A little boost.
        <br />
        Ten fresh starts.
      </h1>
      <p className="credits-product">10 Resume Credits</p>
      <p className="credits-price">
        <strong>$5.50</strong>
        <span>USD · one-time payment</span>
      </p>
      <ul className="credits-benefits">
        {[
          "All features included",
          "Credits never expire",
          "Buy 10 more anytime",
        ].map((text) => (
          <li key={text}>
            <Check size={17} aria-hidden="true" />
            {text}
          </li>
        ))}
      </ul>
      <p className="credits-details">
        AI reviews & feedback · Guided & AI resume building · Cover letters ·
        Job prep · Saved progress · Jokes · PDF/Word exports
      </p>
      <button
        type="button"
        className="credits-buy-button"
        aria-haspopup="dialog"
        onClick={() => {
          setCheckout(true);
          dialog.current.showModal();
        }}
      >
        Buy 10 Resume Credits <span>— $5.50</span>
      </button>
      <dialog
        ref={dialog}
        className="credits-dialog"
        aria-label="Buy 10 resume credits with PayPal"
        onClose={() => setCheckout(false)}
      >
        <button
          type="button"
          className="credits-close"
          onClick={() => dialog.current.close()}
          aria-label="Close checkout"
        >
          Close ×
        </button>
        <h2>10 Resume Credits</h2>
        <div className="credits-checkout">
          {status === "loading" && (
            <p role="status">Loading secure checkout…</p>
          )}
          <div ref={container} />
          {status === "error" && (
            <div role="alert">
              <p>Checkout couldn’t load. Please try again.</p>
              <button
                type="button"
                onClick={() => setRetry((value) => value + 1)}
              >
                Retry checkout
              </button>
            </div>
          )}
        </div>
      </dialog>
      <p className="credits-payment-note">
        <ShieldCheck size={16} aria-hidden="true" /> Secure checkout with PayPal
      </p>
      <p className="credits-no-renewal">
        No subscription. No automatic renewal.
      </p>
    </section>
  );
}
