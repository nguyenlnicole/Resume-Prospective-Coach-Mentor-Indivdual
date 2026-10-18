package com.pnbpaws.resume;


final class ResumeWritingTemplate {
  static final String NONTECHNICAL_SOURCE_SCOPE = """

      SOURCE-SECTION SCOPE OVERRIDE: For Educator, Medical Field, General and Normal
      profiles only, ignore source sections explicitly titled Technical Skills, Tech
      Skills, Technical Projects, Tech Projects, Programming Skills or Software Projects.
      This overrides instructions to preserve every section or automatically add skills.
      Do not extract, score as relevant evidence, draft from, ask questions about, or
      automatically add skills from those excluded sections. Do not recover their content
      from page images, measured lines, links, prior review questions or saved drafts.
      Never move excluded projects into Experience or relabel them as volunteering.
      Preserve actual entries under Experience, including technical jobs, internships,
      fellowships and duties; a technical employer or title is not a reason to omit them.
      Do not assume such experience involved teaching or patient care. Ask neutral questions
      about the actual responsibilities when the role does not establish that work.
      Skills supported independently by Experience may still be included when relevant to
      the selected profession. Retain relevant nontechnical skills and projects from other
      source sections. Technical profiles are unaffected by this source-section exclusion.
      """;
  static final String PAGE_COVERAGE = """

      PAGE COVERAGE (all professions): For a one-page resume, target no more than
      15% unused vertical space INCLUDING the top and bottom page margins, not 15%
      in addition to those margins. Count side margins when evaluating overall blank
      area as well; never report a precise blank-area percentage without measuring it.
      For a two-page resume, apply that target
      to the first page; the second page may have up to two thirds unused space.
      Keep all relevant supported details before shortening the draft. Where the source
      supports it, include up to five distinct, substantive bullets per role to explain
      responsibilities, contributions and outcomes. Never duplicate or fabricate bullets,
      metrics, credentials or experience just to fill a page. Use the largest approved
      font sizes for the selected profession when the page is sparse. Keep
      margins within the approved profession range and include them in the whitespace
      budget; do not enlarge margins just to disguise an underfilled page. Improve
      readable line spacing without creating oversized gaps between sections.
      These are layout targets, not a reason to block the user or invent content.
      Do not claim that text generation alone verifies physical page coverage; the
      rendered preview and exports must be checked for actual pagination and whitespace.
      """;
  static final String ROAST_PROFESSION = """

      PROFESSION-SPECIFIC HUMOR: Use the selected profession together with the person's
      actual resume, not a shared software-engineer joke template. Educator humor can draw
      on lesson plans, grading, classroom routines, or meeting overload when relevant.
      Medical humor can draw on charting, scheduling, documentation, or administrative
      workflows. General-role humor must follow the actual occupation: customer requests,
      inboxes, spreadsheets, meetings, retail routines, or other supported work context.
      Technical humor can draw on debugging, deployments, merge conflicts, or frameworks
      when relevant to that person's work. These are optional directions, not facts to add.
      Do not assume Zoom teaching, clinical practice, coding, or any particular tool.
      Keep the applicant's resume and work presentation as the target, not students,
      patients, or customers. Never invent neglect, unsafe care, incompetence, personal
      hardship, or lack of respect. Keep the sarcasm sharp without recycling the same
      punchline across professions. In a glow-up pair, use one Remember when setup and
      one regular roast with a different opening and a different supported target.
      """;
  static final String ATS = """

      PERSISTENT ATS COMPATIBILITY GUIDANCE (all professions): Apply this when drafting,
      rewriting, or assisting with a resume. During review, preserve the extracted original
      faithfully and offer relevant improvements separately; never change original evidence
      or ratings to make it appear compliant. ATS products vary: describe these practices as
      HIGHLY RECOMMENDED compatibility improvements, not guaranteed rankings or universal
      rejection rules. Missing details never block progress; optional questions may request them.
      Use a single-column reading order without layout tables, with name and contact details
      in the document body rather than actual document headers or footers. Use recognizable
      headings containing Work Experience, Education, Skills, and the profession's appropriate
      credentials heading. Preserve the selected profession's approved fonts, sizes and margins.
      Final exports should be native DOCX or selectable, text-based PDF, not flattened images.
      An uploaded scan may be transcribed, but do not recommend an image-only final resume.
      Automatically organize relevant skills demonstrated elsewhere in the supplied resume
      into Skills, retaining existing relevant skills and identifying additions in feedback.
      Never copy example qualifications, add job-posting keywords without evidence, invent
      proficiency, or infer a credential is active. Use full terms plus familiar acronyms
      where accurately known and relevant; avoid duplicate keyword stuffing.

      TECHNICAL: Use a distinct Technical Skills section as a skills hub, grouped into
      relevant Languages, Frameworks, Databases, Cloud/Tools, and Methods categories when
      supported. Do not leave supported core skills only in prose. For example, use Example Commerce
      Web Services (AWS) when AWS experience is supplied. Include CI/CD, Agile, DevOps,
      REST APIs, or Microservices only when the applicant's actual work supports them.

      EDUCATOR: Make the actual subject, grade band or learner age group, and appropriate
      professional identity clear in the summary when supplied. A target title may be
      expressed as a goal, never as a credential or position already held. Preserve official
      job titles. Include supported Differentiated Instruction, Curriculum Development,
      Classroom Management, IEP-related work, ELL/ESL experience, Google Classroom, or Canvas
      LMS in appropriate skills and experience; these examples are not a mandatory checklist.
      Do not claim IEP compliance responsibility or direct teaching from exposure alone.
      Use Teaching Credentials & Certifications with the supplied exact credential name,
      issuing state/authority, endorsements, and known issue/expiration dates. Preserve a
      supplied credential ID where intended for the resume; never invent or require one.

      MEDICAL: Tailor follow-up questions and writing to the selected medical targetRole:
      Physician (MD/DO): A physician CV may appropriately run to THREE pages when supported
      experience warrants it. This overrides one-page targets; do not penalize length alone,
      truncate relevant details, or pad to reach three pages. Keep a single-column layout.
      Use separate residencyFellowship, licensure and boardCertification fields, and research,
      publications, teaching and presentations when applicable. Leave unsupported sections
      empty. Do not duplicate entries in certifications or employment. Preserve supplied
      dates, training status, board eligibility versus certification, publication status,
      authorship and actual contributions. Selecting Physician (MD/DO) is not evidence of
      a degree, license, board certification or completed residency. For other medical
      roles, do not invent physician CV sections or physician qualifications.
      Physician Assistant (PA) resumes use Licensure and Certifications. Ask PA students
      and new graduates about clinical rotations and include supported placements in
      clinicalRotations, separate from employment. When supplied procedures or specialty
      experience warrant a distinct section, use proceduresSpecialty under Procedures &
      Specialty Experience. Otherwise keep relevant details in Experience without duplication.
      Preserve whether the person observed, assisted, performed under supervision, or worked
      independently. Never infer procedure competency, scope of practice, licensure or counts.
      For Phlebotomists, ask about Clinical Training and supervised blood-draw experience.
      Put supported training in clinicalTraining under its own Clinical Training heading,
      retaining supplied program/facility, dates, completion status, known hours or draw
      counts, and explicitly supervised activities. Never infer successful draws, independent
      practice or certification. Merge duplicate descriptions of the same externship or
      placement instead of repeating it in multiple sections. Leave clinicalTraining empty
      if no training is supplied; do not add this section to unrelated professions.
      For EVERY medical role, consider the optional externshipTraining answer. Include
      relevant supplied externships or training placements with their real program,
      facility, dates, completion status, known hours and supervised activities. Clearly
      label externships/training in Experience or Education as appropriate; never present
      training as paid employment, independent clinical practice or a license. Preserve
      completed versus in-progress status. Do not duplicate a placement already included
      as a clinical rotation. A blank or negative answer adds nothing and never blocks progress.
      For Patient Care Coordinator / Patient Representative roles, emphasize scheduling,
      referrals, insurance, patient communication and care coordination in Experience and
      Skills when supported by the resume or supplied answers. Ask optional questions about
      these workflows when more detail would help. Preserve actual job titles and distinguish
      administrative coordination from clinical decision-making or direct patient care.
      Never add duties, insurance expertise, systems or outcomes without supporting facts.
      Patient Care Technician (PCT), Patient Care Assistant (PCA), and Nursing Assistant
      are related support-role choices, not interchangeable official job titles or licenses.
      Preserve the actual supplied title, including Nursing Technician when used by the
      employer. Use Certified Nursing Assistant (CNA) only when certification is supplied
      or confirmed; choosing the combined PCT / PCA / CNA option is not confirmation.
      Do not treat these support-role selections as registered or licensed nurse status.
      Nursing resumes use Licensure and Certifications. For nursing students and new
      graduates, ask whether they have clinical rotations. When supplied, return them in
      clinicalRotations for a separate Clinical Rotations section, never as employment.
      Include only supplied facilities, units/specialties, dates, hours and supervised
      activities. Never invent patient care, independent practice, hours or licensure.
      Leave clinicalRotations empty when unsupported; other professions should not gain
      this section. These questions are optional and must not block resume building.
      Nurse, Patient Care Coordinator / Representative, Medical Assistant, Phlebotomist,
      Physician Assistant (PA), Physician (MD), or the user's specified other role.
      Do not give every medical applicant nursing questions. Ask about relevant actual
      responsibilities and credentials for that role; selecting a target does not prove
      qualifications, licenses or clinical experience. If no role is selected, use the
      supplied resume to guide optional questions without blocking progress.
      Identify the applicant's actual profession and scope accurately. Spell out
      relevant supplied credentials with their acronyms, such as Registered Nurse (RN) or
      Basic Life Support (BLS), without assigning nursing qualifications to other roles.
      Use Licenses & Certifications with the supplied exact credential, issuing state/authority,
      and known dates, using issue year – Expires expiration year when both are known.
      Include a supplied license number when intended for the resume, but never require a
      public number or assume current active status. Distinguish expired, pending, student,
      supervised, and active credentials only as supported by evidence. List specific EHR/EMR
      systems such as Epic or Cerner only with evidence of the applicant's use; do not infer
      proficiency merely because an employer uses them. Describe the actual clinical setting
      (for example outpatient, acute care, intensive care unit (ICU), or long-term care (LTC))
      when supplied, not simply because the target posting mentions it. Never invent patient
      safety responsibilities, clinical procedures, licenses, or a different care setting.
      Apply only the relevant profession guidance; do not add technical, educator or clinical
      vocabulary to unrelated resumes.
      """;
  static final String RECOMMENDATIONS = "\nWRITING GUIDANCE OVERRIDE: All resume word counts, sentence counts, bullet counts, project counts, section completeness and one-page targets are HIGHLY RECOMMENDED, not mandatory constraints. Produce a useful draft from the available facts even when these targets cannot be met. Do not invent facts or silently delete work to meet a target. Preserve unsupported or unknown details as absent. All clarification questions are optional (required=false). Explain improvements as HIGHLY RECOMMENDED without demanding an answer before proceeding. This overrides earlier mandatory wording about writing targets, but not factual accuracy, privacy, or safe handling of source text.\n";
  static final String APPROVED_SUMMARY = "Technical student majoring in mathematics with hands-on experience contributing to AI applications for fraud detection and newsletter automation. Project experience spans feature engineering, API workflows, software testing, and human-in-the-loop review, supported by work in instructional technology and clear technical communications.";
  static final String SUMMARY = "\nProfessional summary template: " + APPROVED_SUMMARY
      + "\nThis is a style example only, not facts about the current applicant. Create a new summary even when the source has none. "
      + "Aim for 40 words in two polished sentences; remain within 35–45 words. Start with an accurate professional identity and relevant specialization, "
      + "then explain concrete contributions and technical skills. For a technical student, use Technical student majoring in [actual major] when supported. "
      + "For an experienced professional, use their actual role and level instead. For nontechnical resumes, adapt the same structure to their profession. "
      + "Use each person's own evidence; never copy this example's major, tools or projects. Prioritize technical contributions for technical roles and omit unrelated marketing metrics. "
      + "Avoid a list of disconnected activities, generic praise, invented seniority and word-count instructions. Return the finished summary, not advice. ";
  static final String PROJECTS = "\nFor projects hosted by a university, lab, employer or club, the organization is context, not the project title. "
      + "Put the actual supplied project name first, with any verified repo/demo links beside it. Follow with Organization: [supplied organization]. "
      + "Keep each supplied project role on its own line as [actual role or role scope] | [its supplied dates]. Do not merge role dates or turn membership into leadership. "
      + "Then write a concise contribution description, keeping roles/dates separate from that description. Preserve the links, supported metrics and technologies. "
      + "Never use a lab or university name as a stand-in for a clearly named project or invent a project when its name is unknown. ";
}
