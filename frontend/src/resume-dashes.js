export const resumeDashes = (value) =>
  String(value ?? "")
    .replace(/—/g, "–")
    .replace(/ - /g, " – ");
