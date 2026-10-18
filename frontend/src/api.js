import { demoMode, demoRequest } from "./demo-mode.js";
let csrf = "";
export async function request(path, options = {}, retried = false) {
  if (demoMode) return demoRequest(path, options);
  if (options.method && options.method !== "GET" && !csrf) {
    const r = await fetch("/api/csrf");
    if (!r.ok)
      throw Error(
        "The Java service is not running yet. You can explore the sample review.",
      );
    csrf = (await r.json()).token;
  }
  const headers = {
    ...(options.body instanceof FormData
      ? {}
      : { "Content-Type": "application/json" }),
    ...(csrf ? { "X-CSRF-TOKEN": csrf } : {}),
    ...options.headers,
  };
  let response;
  try {
    response = await fetch("/api" + path, { ...options, headers });
  } catch {
    throw Error("We couldn’t connect. Please try again.");
  }
  const type = response.headers.get("content-type") || "";
  const result = type.includes("json") ? await response.json() : null;

  if (
    response.status === 403 &&
    !result?.message &&
    options.method &&
    options.method !== "GET" &&
    !retried
  ) {
    csrf = "";
    return request(path, options, true);
  }
  if (
    !type.includes("json") &&
    !type.includes("pdf") &&
    !type.includes("wordprocessingml.document") &&
    !type.includes("image/png")
  )
    throw Error(
      "The Java service is not running yet. You can explore the sample review.",
    );
  if (
    type.includes("pdf") ||
    type.includes("image/png") ||
    type.includes("wordprocessingml.document")
  ) {
    if (!response.ok) throw Error("The resume file could not be created.");
    return response.blob();
  }
  if (!response.ok) {
    if (
      [401, 403].includes(response.status) &&
      /^\/(review|build|resume|feedback|delivery)/.test(path)
    )
      window.dispatchEvent(new Event("pnb-access-changed"));
    throw Error(
      result?.message ||
        (response.status === 403
          ? "Your session changed or expired. Refresh the page and sign in again."
          : "Something went wrong. Please try again."),
    );
  }
  return result;
}
export const post = (path, data) =>
  request(path, { method: "POST", body: JSON.stringify(data) });
