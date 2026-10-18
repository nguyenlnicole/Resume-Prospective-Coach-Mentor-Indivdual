import medical from "./medical-reference-resume.json" with { type: "json" };
import educator from "./educator-reference-resume.json" with { type: "json" };
import technical from "./reference-resume.json" with { type: "json" };
import general from "./general-reference-resume.json" with { type: "json" };

function example(name, fields) {
  return {
    softwareEngineering: false,
    contact: {
      name,
      location: "Austin, TX",
      phone: "(202) 555-0147",
      email: `${name.toLowerCase().replaceAll(" ", ".")}@example.com`,
      github: "",
      linkedin: "",
      portfolio: "",
    },
    projects: "",
    certifications: "",
    ...fields,
  };
}
const junior = example("Alex Rivera", {
  softwareEngineering: true,
  summary:
    "Junior software developer with hands-on experience building web applications using Java, Spring Boot, React, and PostgreSQL. Contributes to API development, interface updates, and automated tests, with project work focused on accessible forms, reliable data handling, and clear documentation.",
  experience:
    "Example Digital – Software Development Intern | May 2025 – August 2025\n• Implemented form validation and error messages for a React registration page, incorporating code review feedback and checking keyboard navigation.\n• Added unit tests for assigned Spring Boot endpoints and documented setup steps, helping teammates reproduce issues and verify fixes.",
  skills:
    "Languages: Java, JavaScript, SQL, HTML, CSS\nFrameworks and Databases: Spring Boot, React, PostgreSQL\nTools: Git, JUnit, API testing",
  projects:
    "Study Planner – React, JavaScript\nBuilt a study planner with task filters, due dates, and saved browser preferences, helping students organize assignments and revisit their schedules. Tested form validation and keyboard navigation across the main screens.\n\nLibrary Catalog – Java, Spring Boot, PostgreSQL\nCreated a library catalog with book searches and borrowing records, connecting REST endpoints to a PostgreSQL database. Added validation and unit tests for requests that create, update, and return borrowed books.\n\nCommunity Events – HTML, CSS, JavaScript\nDeveloped a responsive events page with category filters and accessible registration forms, allowing visitors to browse local activities. Documented setup instructions and tested the layout across desktop and mobile screen sizes.",
  education:
    "Example State University – Bachelor of Science in Computer Science | May 2026",
});
const mid = example("Casey Parker", {
  softwareEngineering: true,
  summary:
    "Software engineer experienced in delivering web application features with TypeScript, React, Java, and PostgreSQL. Owns implementation, testing, and release support for assigned services, collaborating with design and product teams to resolve defects, improve performance, and maintain dependable user workflows.",
  experience:
    "Example Systems – Software Engineer | July 2023 – Present\n• Delivered account management features across React interfaces and Java services, coordinating API contracts with teammates and verifying releases through integration tests.\n• Improved slow reporting queries by reviewing execution plans and adding targeted indexes, reducing response times in representative load tests.\n• Investigated production defects, documented root causes, and added regression tests to protect corrected workflows during subsequent releases.\n\nExample Digital – Junior Software Developer | June 2021 – June 2023\n• Implemented interface updates and API validation for assigned features, collaborating with designers and incorporating reviewer feedback before release.\n• Maintained automated tests and troubleshooting documentation, helping support colleagues reproduce reported issues and verify fixes in staging.",
  skills:
    "Languages: TypeScript, Java, SQL\nFrameworks and Databases: React, Spring Boot, PostgreSQL\nPractices: Integration testing, API design, debugging, code review",
  projects:
    "Appointment Manager – React, Spring Boot, PostgreSQL\nBuilt a scheduling application with appointment availability, booking validation, and cancellation workflows. Connected React screens to Spring Boot services and added integration tests to check conflicting bookings and changes to existing reservations.\n\nInventory Dashboard – TypeScript, React, PostgreSQL\nDeveloped an inventory dashboard with stock filters, adjustment history, and low-stock summaries. Added validation for stock updates and documented the reporting queries so maintainers could understand calculations and investigate discrepancies.\n\nSupport Queue – Java, Spring Boot\nCreated a support request service with assignment, status tracking, and searchable history. Added role-based access checks and automated tests for request transitions, helping teams keep ownership and follow-up actions visible.",
  education:
    "Example State University – Bachelor of Science in Computer Science | May 2021",
});
const management = example("Riley Morgan", {
  softwareEngineering: true,
  summary:
    "Engineering manager experienced in guiding software teams through planning, delivery, and operational improvement. Partners with product and engineering leads to set priorities, manage dependencies, and develop engineers, combining technical judgment with clear communication and practical support for reliable releases.",
  experience:
    "Example Systems – Engineering Manager | January 2023 – Present\n• Manage a team of eight engineers, aligning delivery plans with product priorities and reviewing dependencies, staffing needs, and release risks.\n• Hold regular development conversations and coordinate mentoring opportunities, helping engineers set specific goals and expand ownership of team responsibilities.\n• Introduced shared release readiness reviews with product and support teams, clarifying outstanding risks and ownership before customer-facing changes.\n\nExample Digital – Technical Lead | June 2020 – December 2022\n• Led implementation planning for account services, breaking requirements into deliverable work and coordinating API decisions across engineering teams.\n• Reviewed designs and supported incident investigations, translating findings into testing improvements and clearer operational documentation for the team.",
  skills:
    "Leadership: Team development, hiring, coaching, delivery planning, stakeholder communication\nTechnical Practice: Architecture reviews, incident management, API design, release planning\nTools: Jira, GitHub, Java, PostgreSQL",
  education:
    "Example State University – Bachelor of Science in Computer Science | May 2016",
  certifications:
    "Professional Scrum Master I – Scrum.org | 2021\nProject Management Professional – PMI | 2024 – Expires 2027",
});
export function resumeExampleFor(profession, level) {
  const category = profession === "Technical" ? level : profession || level;
  const resume =
    {
      General: general,
      Normal: general,
      "Medical Field": medical,
      Educator: educator,
      "Entry / Junior": junior,
      "Mid-level": mid,
      Senior: technical,
      Management: management,
    }[category] || general;
  return {
    resume,
    level: category || "General",
    label: `${resume.contact.name} · ${category === "Normal" ? "General" : category || "General"} resume example`,
  };
}
