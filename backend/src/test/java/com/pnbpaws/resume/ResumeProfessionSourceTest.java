package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ResumeProfessionSourceTest {
  @Test void physicianPageExceptionDoesNotApplyToPaOrUnrelatedRoles() {
    var resume = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
    resume.put("summary", "Physician assistant with outpatient experience.");
    assertFalse(ResumeProfiles.physicianCv("Medical Field", resume));
    resume.put("summary", "Physician with outpatient experience.");
    assertTrue(ResumeProfiles.physicianCv("Medical Field", resume));
    assertFalse(ResumeProfiles.physicianCv("General", resume));
  }
  @Test void excludesTechnicalSectionsButPreservesExperienceAndOtherSections() {
    String source = "EXPERIENCE\nDoIT — AI Fellow\nBuilt an API\nTECHNICAL SKILLS\nJava, Python\nTECHNICAL PROJECTS\nFraud detection\nLEADERSHIP & CAMPUS INVOLVEMENT\nCampus volunteer\nEDUCATION\nMathematics degree";
    for (String level : new String[]{"Educator", "Medical Field", "General", "Normal"}) {
      String result = ResumeProfiles.professionSource(source, level);
      assertTrue(result.contains("DoIT — AI Fellow\nBuilt an API"));
      assertTrue(result.contains("Campus volunteer"));
      assertTrue(result.contains("Mathematics degree"));
      assertFalse(result.contains("Java, Python"));
      assertFalse(result.contains("Fraud detection"));
    }
    assertEquals(source, ResumeProfiles.professionSource(source, "Senior"));
  }
  @Test void keepsGeneralSkillsAndProjects() {
    String source = "Skills\nLesson planning\nProjects & Volunteer Experience\nReading program\nTech Projects:\nChat app\nWork Experience\nTutor";
    String result = ResumeProfiles.professionSource(source, "Educator");
    assertTrue(result.contains("Lesson planning"));
    assertTrue(result.contains("Reading program"));
    assertTrue(result.contains("Tutor"));
    assertFalse(result.contains("Chat app"));
  }
}
