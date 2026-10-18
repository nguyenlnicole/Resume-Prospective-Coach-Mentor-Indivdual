export function readableJobPrepText(value = "") {
  return String(value)
    .replace(/\s*\(\s*\[[^\]]+\]\(https?:\/\/[^)]+\)\s*\)/g, "")
    .replace(/\[([^\]]+)\]\(https?:\/\/[^)]+\)/g, "$1")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/\s+/g, " ")
    .trim();
}

export function jobPrepDocumentText(sheet) {
  if (!sheet) return "";
  const list = (items) =>
    items
      .map((item, index) => `${index + 1}. ${readableJobPrepText(item)}`)
      .join("\n\n");
  const facts = sheet.facts
    .map(
      (item, index) =>
        `${index + 1}. ${readableJobPrepText(item.fact)}\nSource: ${readableJobPrepText(item.sourceTitle || "Source")}\n${item.sourceUrl}`,
    )
    .join("\n\n");
  const evidence = (item) =>
    `${readableJobPrepText(item?.text || "Not verified from available public sources.")}${item?.sourceUrl ? `\nSource: ${readableJobPrepText(item.sourceTitle || "Source")}\n${item.sourceUrl}` : ""}`;
  const company = sheet.company
    ? `COMPANY AT A GLANCE\n${readableJobPrepText(sheet.company.name)}\n\nFounders: ${evidence(sheet.company.founders)}\n\nFounded (year and month): ${evidence(sheet.company.founded)}\n\nAbout the company: ${evidence(sheet.company.bio)}\n\nEmployees: ${evidence(sheet.company.employees)}\n\n`
    : "";
  const questions = sheet.interviewQuestions?.length
    ? `FOUR LIKELY INTERVIEW QUESTIONS\nThese are practice predictions, not guaranteed interview questions.\n\n${sheet.interviewQuestions.map((item, index) => `${index + 1}. ${readableJobPrepText(item.question)}\nWhy: ${readableJobPrepText(item.whyLikely)}\nPrepare: ${readableJobPrepText(item.howToPrepare)}\n${readableJobPrepText(item.basis)}${item.sources?.length ? `\n${item.sources.map(evidence).join("\n")}` : ""}`).join("\n\n")}\n\n`
    : "";
  return `JOB PREP SHEET\n\n${company}THINGS TO STUDY\n${list(sheet.study)}\n\nWAYS TO PREPARE\n${list(sheet.prepare)}\n\n${questions}OUTSIDE FACTS AND SOURCES\nThese facts come from linked sources, not the job posting.\n\n${facts || "No outside facts were verified. We left out anything we could not verify."}${sheet.researchLimits ? `\n\nRESEARCH LIMITS\n${readableJobPrepText(sheet.researchLimits)}` : ""}`;
}
