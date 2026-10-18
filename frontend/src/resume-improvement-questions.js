export function resumeImprovementQuestions(review, level = "") {
  const resume = review?.resume || {};
  const nontechnical = [
    "General",
    "Normal",
    "Medical Field",
    "Educator",
  ].includes(level);
  const codingTerms =
    /\b(coding|programming|source code|software development|software engineering|api|apis|frameworks?|databases?|deployment|debugging|unit tests?|react|java|python|sql)\b/i;
  const existing = (review?.questions || [])
    .filter(
      (q) =>
        !/\b(url|link|github|portfolio|repository)\b/i.test(
          `${q.question || ""} ${q.id || ""}`,
        ),
    )
    .filter(
      (q) =>
        !nontechnical ||
        !codingTerms.test(`${q.question || ""} ${q.hint || ""}`) ||
        codingTerms.test(resume[q.section] || ""),
    )
    .map((q) => ({
      ...q,
      hint:
        q.section === "summary" || /summary/i.test(q.question || "")
          ? "Highly recommended: share any focus you want included. You can skip this; we’ll draft a professional summary from the facts in your resume."
          : q.hint?.replace(/\brequires?\b/gi, "recommends"),
    }))
    .map((q) => {
      if (q.hint === undefined) delete q.hint;
      return q;
    });
  const hasExperience = Boolean(resume.experience?.trim());
  const candidates = [];
  const add = (id, question, hint, topic, section = "experience") =>
    candidates.push({
      id: `improve_${id}`,
      question,
      hint,
      required: false,
      section,
      topic,
    });
  add(
    "certifications",
    "Do you have any relevant certifications that are not listed?",
    "Share the certification name and issuer. Dates are optional. Does it expire? If so, include the date earned and expiration date when known, for example 2019 – Expires 2029. If you are still working on it, say in progress. Skip if none apply.",
    /certificat(?:ion|e)s?/i,
    "certifications",
  );
  add(
    "courses",
    "Have you taken any relevant courses or training that are not listed?",
    "Share the course or training name and provider. Dates are optional. Say whether it is completed or still in progress. Skip if none apply.",
    /\bcourses?\b|coursework|training/i,
    "certifications",
  );
  if (hasExperience) {
    add(
      "impact",
      "What improved because of your work in one of your listed roles?",
      "Name the role and describe the before-and-after result. Numbers are optional; use only facts you know.",
      /before.and.after|outcome|impact|what improved|achievement/i,
    );
    add(
      "scope",
      "What was the size or scope of work you handled?",
      "Name the role. Examples include customers helped, records handled, projects delivered, or team size. Leave out figures you cannot support.",
      /scope|how many|volume|team size/i,
    );
    add(
      "ownership",
      "Which part of a listed accomplishment did you personally own?",
      "Name the role and explain your contribution. Separate your work from the team's work.",
      /personally|ownership|own contribution/i,
    );
    add(
      "collaboration",
      "Who did you work with to solve a problem or finish a task?",
      "Describe one example and your contribution. You do not need to name individual people.",
      /collaborat|work with|cross.functional|stakeholder/i,
    );
    add(
      "tools",
      "Which tools or methods did you actually use in a listed role?",
      "Name the role, the tool or method, and what you used it for. Include only hands-on experience.",
      /tools or methods|which tools|technologies.*use/i,
      "skills",
    );
    add(
      "recognition",
      "Is there a relevant achievement or recognition missing from your resume?",
      "Examples include an award, a process improvement, or positive customer feedback. Skip this if none applies.",
      /recognition|award|missing achievement/i,
    );
  }
  if (resume.projects?.trim()) {
    add(
      "project_problem",
      "What problem did one of your projects solve, and who was it for?",
      "Name the project and describe a real user need. Do not claim users, sales, or adoption you have not verified.",
      /project.*problem|who.*project|project.*solve/i,
      "projects",
    );
    add(
      "project_challenge",
      "What difficult issue did you solve in one of your projects?",
      "Name the project, the issue, and your fix. Include only work you personally performed.",
      /difficult|challenge|technical issue|project.*fix/i,
      "projects",
    );
  }
  if (level === "Medical Field")
    add(
      "care",
      "How did your work support patient care, safety, or accurate records?",
      "Use a general example without patient names, identifiers, or private health details.",
      /patient care|patient safety|accurate records/i,
    );
  if (level === "Educator")
    add(
      "learning",
      "How did you adapt your teaching or support to help learners?",
      "Describe your action and an observed result. Do not include student names or private records.",
      /learners|student.*outcome|adapt.*teach/i,
    );
  const tailored =
    level === "Educator"
      ? {
          impact: [
            "What improved for learners because of your work?",
            "Name the role and a change you observed in learning, participation, or access to support. Do not include private student records.",
          ],
          scope: [
            "Which learners, subjects, or learning settings did you support?",
            "Describe only the groups and subjects you actually worked with. Include group size only if known.",
          ],
          tools: [
            "Which teaching methods, learning resources, or education tools did you use?",
            "For your actual role, examples include small-group lessons, exit tickets, reading checks, Google Classroom, or Seesaw. Explain how they informed instruction or support. Distinguish training completed from instruction delivered.",
          ],
          collaboration: [
            "How did you work with educators, families, or support staff?",
            "Describe your contribution to lesson planning, learner support, or communication. Leave out identifying student details.",
          ],
          project_problem: [
            "What was the purpose of a listed education project or volunteer activity?",
            "Name the activity, who it supported, and what you personally contributed. Keep volunteering distinct from employment.",
          ],
          project_challenge: [
            "What challenge did you address in that education project or activity?",
            "Describe your action and an observed result, without inventing learning outcomes.",
          ],
        }
      : level === "Medical Field"
        ? {
            impact: [
              "How did your work improve care, service, safety, or accurate records?",
              "Use an example appropriate to your actual clinical or nonclinical role. Do not include patient identifiers.",
            ],
            scope: [
              "Which healthcare setting and responsibilities did your role involve?",
              "Describe your actual duties and team. Do not claim clinical responsibilities outside your role.",
            ],
            tools: [
              "Which healthcare systems, procedures, or administrative tools did you use?",
              "Depending on your actual role, examples include EHR documentation, patient intake, scheduling, insurance checks, or referral tracking. Name only what you used. Training alone does not establish clinical experience.",
            ],
            collaboration: [
              "How did you coordinate with colleagues to support care or healthcare operations?",
              "Describe a handover, scheduling task, records workflow, or other relevant example without patient details.",
            ],
            project_problem: [
              "What was the purpose of a listed healthcare project or volunteer activity?",
              "Explain who it supported and your role. Do not imply clinical care if you provided administrative or community support.",
            ],
            project_challenge: [
              "What challenge did you help address in that healthcare activity?",
              "Describe your contribution and a supported result. Leave out private patient details.",
            ],
          }
        : nontechnical
          ? {
              impact: [
                "How did your work help customers, coworkers, or day-to-day operations?",
                "Name the role, what you did, and what changed. A clear example is enough; numbers are optional.",
              ],
              scope: [
                "What responsibilities did you handle in your day-to-day work?",
                "For service work, mention the phone, email, chat, or in-person channels you used and workload if known. Other examples include schedules, records, stock, or teammates supported. Use details relevant to your actual job.",
              ],
              tools: [
                "Which practical tools, equipment, or work methods did you use?",
                "Name the role and purpose, such as a support ticket system, POS, Excel, scheduling, or inventory. Explain how you followed through on issues or kept records accurate. Only list tools you actually used.",
              ],
              project_problem: [
                "Who did a listed project or volunteer activity support?",
                "Name the activity and explain your contribution, such as organizing an event, helping a community group, or improving a work process.",
              ],
              project_challenge: [
                "What challenge did you help resolve during that activity?",
                "Describe what happened, what you did, and the result you observed.",
              ],
            }
          : {};
  for (const candidate of candidates) {
    const wording = tailored[candidate.id.replace("improve_", "")];
    if (wording) [candidate.question, candidate.hint] = wording;
  }
  if (level === "Medical Field" || level === "Educator") {
    const credential = candidates.find(
      (q) => q.id === "improve_certifications",
    );
    credential.question =
      level === "Educator"
        ? "Do you hold any relevant teaching credentials or certifications not listed?"
        : "Do you hold any relevant healthcare licenses or certifications not listed?";
    credential.hint +=
      " Include only credentials you actually hold; not every role requires a license.";
  }
  const available = candidates.filter(
    (candidate) =>
      !existing.some((q) =>
        candidate.topic.test(`${q.question} ${q.hint || ""}`),
      ),
  );
  const credentials = available.filter((candidate) =>
    ["improve_certifications", "improve_courses"].includes(candidate.id),
  );
  const other = available.filter(
    (candidate) => !credentials.includes(candidate),
  );
  other.sort(
    (a, b) =>
      Number(["improve_care", "improve_learning"].includes(b.id)) -
      Number(["improve_care", "improve_learning"].includes(a.id)),
  );
  const preferred =
    hasExperience && nontechnical
      ? other.filter((q) =>
          [
            "improve_care",
            "improve_learning",
            "improve_impact",
            "improve_scope",
            "improve_tools",
          ].includes(q.id),
        )
      : [];
  const remaining = other.filter((q) => !preferred.includes(q));
  const additions = [
    ...preferred.slice(0, 4),
    ...credentials,
    ...preferred.slice(4),
    ...remaining,
  ].slice(
    0,
    Math.max(
      credentials.length + preferred.slice(0, 4).length,
      10 - existing.length,
    ),
  );
  return [...existing, ...additions.map(({ topic, ...question }) => question)];
}
