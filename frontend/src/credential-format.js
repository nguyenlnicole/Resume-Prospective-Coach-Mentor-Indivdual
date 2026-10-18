export function credentialParts(value = "") {
  const clean = value
    .replace(/&#(?:x20|32);|&nbsp;/gi, " ")
    .replace(/[*_]/g, "")
    .replace(/\s+/g, " ")
    .trim();
  const dated = clean.match(/^(.*?)\s*\|\s*([^|]*\b(?:19|20)\d{2}[^|]*)$/);
  const title = dated ? dated[1].trim() : clean;
  const separator = title.search(/,\s+|\s+[—–-]\s+/);
  if (separator < 0)
    return { name: title, issuer: "", date: dated?.[2].trim() || "" };
  const after = title.slice(separator).replace(/^(?:,\s+|\s+[—–-]\s+)/, "");
  return {
    name: title.slice(0, separator),
    issuer: after,
    date: dated?.[2].trim() || "",
  };
}
export function normalizeCredentials(value = "") {
  return value
    .split(/\r?\n/)
    .map((line) => {
      const { name, issuer, date } = credentialParts(line);
      return name + (issuer ? `, ${issuer}` : "") + (date ? ` | ${date}` : "");
    })
    .join("\n");
}
