const test = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");
const {
  serviceOrigin,
  assetPath,
  externalUrl,
  resumeDownload,
  createApiProxy,
} = require("../transport.cjs");

test("coach desktop saves named PDF and Word resumes", () => {
  assert.equal(
    resumeDownload(
      "blob:pnb-app://desktop/123",
      "application/pdf",
      "Jane-Doe-PNBRP-1026.pdf",
    ).name,
    "Jane-Doe-PNBRP-1026.pdf",
  );
  assert.equal(
    resumeDownload(
      "blob:pnb-app://desktop/123",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "Jane-Doe-PNBRP-1026.docx",
    ).extension,
    "docx",
  );
  assert.equal(
    resumeDownload(
      "blob:pnb-app://desktop/123",
      "application/pdf",
      "../bad.pdf",
    ).name,
    "Professional Resume.pdf",
  );
  assert.equal(
    resumeDownload(
      "https://elsewhere.example/resume.pdf",
      "application/pdf",
      "resume.pdf",
    ),
    null,
  );
  assert.equal(
    resumeDownload(
      "blob:pnb-app://desktop/123",
      "application/x-msdownload",
      "bad.exe",
    ),
    null,
  );
});

test("shared builds require HTTPS and never accept credentials or paths", () => {
  assert.equal(
    serviceOrigin("https://resume.example.com"),
    "https://resume.example.com",
  );
  for (const url of [
    "http://example.com",
    "http://localhost:8080",
    "https://user:secret@example.com",
    "https://example.com/api",
    "https://example.com?key=secret",
    "https://localhost",
  ])
    assert.throws(() => serviceOrigin(url));
  assert.equal(
    serviceOrigin("http://127.0.0.1:8080", true),
    "http://127.0.0.1:8080",
  );
});
test("asset requests cannot escape the bundled interface", () => {
  const root = path.resolve("ui");
  assert.equal(
    assetPath(root, "pnb-app://desktop/"),
    path.join(root, "index.html"),
  );
  for (const url of [
    "pnb-app://other/index.html",
    "pnb-app://desktop/%2e%2e%2fsecret",
    "pnb-app://desktop/%5cWindows%5csecret",
    "pnb-app://desktop/C:%5csecret",
    "pnb-app://desktop/%00",
  ])
    assert.equal(assetPath(root, url), null);
  assert.equal(externalUrl("file:///C:/Windows/cmd.exe"), false);
  assert.equal(externalUrl("javascript:alert(1)"), false);
  assert.equal(externalUrl("https://github.com/example"), true);
});
test("proxy keeps the server session private and rotates it on login", async () => {
  const calls = [];
  const proxy = createApiProxy(
    { apiOrigin: "https://resume.example.com" },
    async (url, options) => {
      calls.push({ url, options });
      return Response.json(
        { ok: true },
        {
          headers: {
            "Set-Cookie": `JSESSIONID=session-${calls.length}; HttpOnly; Secure; SameSite=Strict`,
          },
        },
      );
    },
  );
  const a = await proxy(new Request("pnb-app://desktop/api/csrf"));
  assert.equal(a.headers.has("set-cookie"), false);
  await proxy(
    new Request("pnb-app://desktop/api/admin/login", {
      method: "POST",
      body: "{}",
      headers: { "X-CSRF-TOKEN": "csrf", Authorization: "untrusted" },
    }),
  );
  await proxy(new Request("pnb-app://desktop/api/status"));
  assert.equal(calls[1].options.headers.get("cookie"), "JSESSIONID=session-1");
  assert.equal(calls[2].options.headers.get("cookie"), "JSESSIONID=session-2");
  assert.equal(calls[1].options.headers.get("x-csrf-token"), "csrf");
  assert.equal(calls[1].options.headers.has("authorization"), false);
  assert.equal(calls[1].options.redirect, "error");
});
test("offline service and foreign requests fail without exposing internal errors", async () => {
  const proxy = createApiProxy(
    { apiOrigin: "https://resume.example.com" },
    async () => {
      throw Error("private details");
    },
  );
  const offline = await proxy(new Request("pnb-app://desktop/api/status"));
  assert.equal(offline.status, 503);
  assert.ok(!(await offline.text()).includes("private details"));
  assert.equal(
    (await proxy(new Request("pnb-app://other/api/status"))).status,
    400,
  );
});
