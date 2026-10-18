const fs = require("node:fs");
const path = require("node:path");
const { serviceOrigin } = require("./transport.cjs");
const edition = process.env.PNB_EDITION || "coach";
if (!["coach", "beta"].includes(edition))
  throw new Error("Choose coach or beta (Individual) edition.");
const preview = process.env.PNB_PREVIEW_BUILD === "1";
const address =
  process.env.PNB_API_ORIGIN || (preview ? "http://127.0.0.1:8080" : "");
if (!address)
  throw new Error(
    "Set PNB_API_ORIGIN to your shared HTTPS service. For a local owner preview, set PNB_PREVIEW_BUILD=1.",
  );
const config = { apiOrigin: serviceOrigin(address, preview), preview, edition };
const root = __dirname;
const ui = path.join(root, "..", "frontend", `dist-${edition}`);
if (!fs.existsSync(path.join(ui, "index.html")))
  throw new Error(
    "Build the frontend first: npm --prefix ../frontend run build",
  );
fs.mkdirSync(path.join(root, "resources"), { recursive: true });
const staged = path.resolve(root, "ui");
if (path.dirname(staged) !== root || path.basename(staged) !== "ui")
  throw new Error("Invalid staging directory.");
fs.rmSync(staged, { recursive: true, force: true });
fs.cpSync(ui, staged, { recursive: true });
fs.copyFileSync(
  path.join(ui, "pnb-paws-logo-transparent.png"),
  path.join(root, "resources", "icon.png"),
);
fs.writeFileSync(
  path.join(root, "desktop-config.json"),
  JSON.stringify(config, null, 2),
);
console.log(
  preview
    ? "Prepared local desktop preview. This build is not for coach distribution."
    : "Prepared desktop client for the shared service.",
);
