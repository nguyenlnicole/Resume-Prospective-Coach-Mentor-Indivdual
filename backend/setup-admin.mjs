import { pbkdf2Sync, randomBytes } from "node:crypto";
import { writeFileSync, existsSync } from "node:fs";
if (existsSync(".local.properties"))
  throw Error(
    "Local configuration already exists. Use password recovery or edit the local setup deliberately.",
  );
process.stdout.write("Enter the initial admin password through stdin.\n");
let input = "";
process.stdin.setEncoding("utf8");
process.stdin.on("data", (chunk) => {
  input += chunk;
  if (input.includes("\n")) {
    const password = input.trimEnd();
    if (password.length < 12) throw Error("Use at least 12 characters.");
    const salt = randomBytes(16);
    const hash =
      "pbkdf2$600000$" +
      salt.toString("base64") +
      "$" +
      pbkdf2Sync(password, salt, 600000, 32, "sha256").toString("base64");
    writeFileSync(".local.properties", `app.admin-password-hash=${hash}\n`, {
      mode: 0o600,
    });
    input = "";
    process.stdout.write(
      "Admin password hash saved to ignored local configuration.\n",
    );
    process.exit(0);
  }
});
