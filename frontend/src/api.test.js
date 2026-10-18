import test from "node:test";
import assert from "node:assert/strict";

let run = 0;
async function scenario(responses, action) {
  const priorFetch = globalThis.fetch;
  const priorWindow = globalThis.window;
  const calls = [],
    events = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    assert.ok(responses.length, "Unexpected extra request");
    return responses.shift();
  };
  globalThis.window = { location: {search:""}, dispatchEvent: (event) => events.push(event.type) };
  try {
    const api = await import(`./api.js?test=${++run}`);
    await action(api, calls, events);
    assert.equal(responses.length, 0);
  } finally {
    globalThis.fetch = priorFetch;
    globalThis.window = priorWindow;
  }
}
const json = (value, status = 200) =>
  new Response(JSON.stringify(value), {
    status,
    headers: { "content-type": "application/json" },
  });

test("JSON CSRF rejection refreshes the token and retries admin sign-in once", async () => {
  await scenario(
    [
      json({ token: "old" }),
      json({ status: 403, error: "Forbidden" }, 403),
      json({ token: "fresh" }),
      json({ ok: true }),
    ],
    async (api, calls, events) => {
      assert.deepEqual(
        await api.post("/admin/code", { email: "admin@example.com" }),
        { ok: true },
      );
      assert.equal(calls[1].options.headers["X-CSRF-TOKEN"], "old");
      assert.equal(calls[3].options.headers["X-CSRF-TOKEN"], "fresh");
      assert.equal(calls[1].options.body, calls[3].options.body);
      assert.deepEqual(events, []);
    },
  );
});
test("HTML CSRF rejection is still retried", async () => {
  await scenario(
    [
      json({ token: "old" }),
      new Response("Forbidden", { status: 403 }),
      json({ token: "fresh" }),
      json({ ok: true }),
    ],
    async (api) => {
      assert.deepEqual(await api.post("/admin/code", {}), { ok: true });
    },
  );
});
test("explicit access rejection is not retried or bypassed", async () => {
  await scenario(
    [
      json({ token: "token" }),
      json({ message: "Please sign in to continue." }, 403),
    ],
    async (api, calls, events) => {
      await assert.rejects(api.post("/review", {}), /Please sign in/);
      assert.equal(calls.length, 2);
      assert.deepEqual(events, ["pnb-access-changed"]);
    },
  );
});
test("repeated security rejection stops after one retry with a useful error", async () => {
  await scenario(
    [
      json({ token: "old" }),
      json({ status: 403 }, 403),
      json({ token: "fresh" }),
      json({ status: 403 }, 403),
    ],
    async (api) => {
      await assert.rejects(
        api.post("/admin/code", {}),
        /session changed or expired/,
      );
    },
  );
});
test("expired authentication is not retried as a CSRF failure", async () => {
  await scenario(
    [json({ token: "token" }), json({ message: "Sign in again." }, 401)],
    async (api, calls, events) => {
      await assert.rejects(api.post("/build", {}), /Sign in again/);
      assert.equal(calls.length, 2);
      assert.deepEqual(events, ["pnb-access-changed"]);
    },
  );
});
test("PDF responses remain readable blobs", async () => {
  await scenario(
    [
      json({ token: "token" }),
      new Response("%PDF-test", {
        headers: { "content-type": "application/pdf" },
      }),
    ],
    async (api) => {
      const blob = await api.post("/resume/pdf", {});
      assert.equal(await blob.text(), "%PDF-test");
    },
  );
});
