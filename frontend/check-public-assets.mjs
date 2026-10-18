import { readdirSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
const publicDir = fileURLToPath(new URL("./public/", import.meta.url));
function inspect(dir) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const file = join(dir, entry.name);
    if (entry.isSymbolicLink())
      throw new Error(
        "Public assets must not link to private files: " + entry.name,
      );
    if (
      /resume.*\.(pdf|docx?|png|jpe?g)$/i.test(entry.name) &&
      !/^pnb-resume-prospective-(icon|logo)\.png$/i.test(entry.name)
    )
      throw new Error(
        "Resume documents and reference images must stay outside public assets: " +
          entry.name,
      );
    if (entry.isDirectory()) inspect(file);
  }
}
inspect(publicDir);
console.log(
  "Privacy check passed: no resume documents or reference images in public assets.",
);
