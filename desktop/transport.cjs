const path = require("node:path");

const origin = "pnb-app://desktop";
const csp =
  "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: blob:; connect-src 'self'; frame-src 'self' blob:; object-src 'none'; base-uri 'self'; form-action 'self'";

function serviceOrigin(value, preview = false) {
  const url = new URL(value);
  const local = ["127.0.0.1", "localhost", "[::1]"].includes(url.hostname);
  if (
    url.username ||
    url.password ||
    url.search ||
    url.hash ||
    url.pathname !== "/" ||
    !(
      url.protocol === "https:" ||
      (preview && local && url.protocol === "http:")
    ) ||
    (local && !preview)
  ) {
    throw new Error(
      "Use an HTTPS service address without a path or credentials. Local HTTP is for preview builds only.",
    );
  }
  return url.origin;
}

function assetPath(root, requestUrl) {
  const url = new URL(requestUrl);
  if (`${url.protocol}//${url.host}` !== origin) return null;
  let name;
  try {
    name = decodeURIComponent(url.pathname);
  } catch {
    return null;
  }
  if (name.includes("\\") || name.includes("\0") || name.includes(":"))
    return null;
  const file = path.resolve(root, "." + (name === "/" ? "/index.html" : name));
  const relative = path.relative(root, file);
  if (!relative || relative.startsWith("..") || path.isAbsolute(relative))
    return null;
  return file;
}

function externalUrl(value) {
  try {
    const url = new URL(value);
    return (
      ["https:", "http:", "mailto:"].includes(url.protocol) &&
      !url.username &&
      !url.password
    );
  } catch {
    return false;
  }
}

function resumeDownload(url, mimeType, filename) {
  if (!url.startsWith(`blob:${origin}/`)) return null;
  const formats = {
    "application/pdf": { extension: "pdf", label: "PDF resume" },
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": {
      extension: "docx",
      label: "Word resume",
    },
  };
  const format = formats[mimeType?.toLowerCase()];
  if (!format) return null;
  const name =
    typeof filename === "string" &&
    /^[a-z0-9][a-z0-9 _.-]{0,120}$/i.test(filename) &&
    filename.toLowerCase().endsWith(`.${format.extension}`)
      ? filename
      : `Professional Resume.${format.extension}`;
  return { name, ...format };
}

function createApiProxy(config, fetcher = fetch) {
  const upstream = serviceOrigin(config.apiOrigin, config.preview);
  let cookie = "";
  return async (request) => {
    const url = new URL(request.url);
    if (
      `${url.protocol}//${url.host}` !== origin ||
      !/^\/api\/[a-z0-9/-]+$/i.test(url.pathname)
    )
      return Response.json({ message: "Unknown request." }, { status: 400 });
    if (request.initiatorOrigin && request.initiatorOrigin !== origin)
      return Response.json(
        { message: "This request is not allowed." },
        { status: 403 },
      );
    if (!["GET", "POST"].includes(request.method))
      return new Response(null, { status: 405 });
    const headers = new Headers();
    for (const name of ["content-type", "x-csrf-token"]) {
      if (request.headers.has(name))
        headers.set(name, request.headers.get(name));
    }
    if (cookie) headers.set("cookie", cookie);
    try {
      const body =
        request.method === "GET" ? undefined : await request.arrayBuffer();
      if (body && body.byteLength > 11 * 1024 * 1024)
        return Response.json(
          { message: "Please choose a PDF smaller than 10 MB." },
          { status: 413 },
        );
      const response = await fetcher(upstream + url.pathname + url.search, {
        method: request.method,
        headers,
        body,
        redirect: "error",
        signal: AbortSignal.timeout(150000),
      });
      for (const setCookie of response.headers.getSetCookie()) {
        const match = setCookie.match(/^JSESSIONID=([^;]*)/i);
        if (match) cookie = match[1] ? `JSESSIONID=${match[1]}` : "";
      }
      const returned = new Headers({ "cache-control": "no-store" });
      for (const name of ["content-type", "content-disposition"]) {
        if (response.headers.has(name))
          returned.set(name, response.headers.get(name));
      }
      return new Response(response.body, {
        status: response.status,
        headers: returned,
      });
    } catch {
      return Response.json(
        {
          message: config.preview
            ? "The local preview service is not connected. Start the Java service to use the resume tools."
            : "We couldn’t reach the PNB service. Check your internet connection and try again.",
        },
        { status: 503 },
      );
    }
  };
}

module.exports = {
  origin,
  csp,
  serviceOrigin,
  assetPath,
  externalUrl,
  resumeDownload,
  createApiProxy,
};
