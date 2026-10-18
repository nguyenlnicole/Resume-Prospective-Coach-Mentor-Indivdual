const { spawnSync } = require("node:child_process");
const path = require("node:path");
const edition = process.argv[2];
if (!["coach", "beta"].includes(edition))
  throw new Error("Choose coach or beta (Individual).");
const label = edition === "beta" ? "Individual" : "Coach-Mentor";
const env = { ...process.env, PNB_EDITION: edition };
function run(args) {
  const result = spawnSync(process.execPath, args, {
    cwd: __dirname,
    env,
    stdio: "inherit",
  });
  if (result.status !== 0) process.exit(result.status || 1);
}
run(["prepare.cjs"]);
run([
  "node_modules/electron-builder/cli.js",
  ...(process.argv.includes("--dir") ? ["--dir"] : ["--win", "nsis"]),
  "--x64",
  "--publish",
  "never",
  "-c.electronDist=node_modules/electron/dist",
  "-c.win.signExecutable=false",
  `-c.appId=com.pnbpaws.resumeprospective.${edition}`,
  `-c.productName=PNB Resume Prospective ${label}`,
  `-c.nsis.shortcutName=PNB Resume Prospective ${label}`,
  `-c.directories.output=release-${edition}`,
  `-c.artifactName=PNB-Resume-Prospective-${label}-${env.PNB_PREVIEW_BUILD === "1" ? "Preview" : "Setup"}.exe`,
]);
