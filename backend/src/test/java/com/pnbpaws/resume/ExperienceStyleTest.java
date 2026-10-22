package com.pnbpaws.resume;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ExperienceStyleTest {
 @Test void longWrappedRoleKeepsItalicFontAndBoldEmployer() throws Exception {
  var r=new ObjectMapper().createObjectNode();
  r.putObject("contact").put("name","Sample Candidate");
  r.put("experience","Lawrence Berkeley National Laboratory — Research Intern, Experience in Research Program | Summer 2023\n• Presented findings to the laboratory research community.");
  var service=new PdfService("C:/Windows/Fonts/calibri.ttf","C:/Windows/Fonts/calibrib.ttf");
  byte[] bytes=service.create(r,"Medical Field","Calibri","right");
  try(var pdf=Loader.loadPDF(bytes)) {
   List<String> runs=new ArrayList<>();
   new PDFTextStripper(){protected void writeString(String text,List<TextPosition> positions) {
    for(var pos:positions) runs.add(pos.getUnicode()+":"+pos.getFont().getName());
   }}.getText(pdf);
   assertTrue(runs.stream().anyMatch(x->x.endsWith("Calibri-Italic")),runs.toString());
   assertTrue(runs.stream().anyMatch(x->x.endsWith("Calibri-Bold")),runs.toString());
  }
  var output=System.getProperty("style.pdf.output");
  if(output!=null) java.nio.file.Files.write(java.nio.file.Path.of(output),bytes);
 }
 @Test void volunteerRoleStaysInExperienceAndProjectsHeadingIsPlain() {
  var r=new ObjectMapper().createObjectNode().put("projects","Community Lab — Volunteer | May 2024 - Present\n• Organized research materials.");
  var clean=ResumeCleanup.normalize(r);
  assertTrue(clean.path("experience").asText().contains("Community Lab — Volunteer"));
  assertTrue(clean.path("projects").asText().isBlank());
  assertEquals("PROJECTS",ResumeProfiles.projectsHeading("Medical Field"));
 }
 @Test void bioinformaticsKeepsTechnicalContentAndResearchHeadings() throws Exception {
  String source="Biochemistry student\nTECHNICAL SKILLS\nPython, data analysis\nTECHNICAL PROJECTS\nMachine learning for dengue forecasting";
  assertEquals(source,ResumeProfiles.professionSource(source,"Medical Field"));
  var r=new ObjectMapper().createObjectNode().put("summary","Bioinformatics student with research experience.")
      .put("experience","Research Lab — Intern | May 2024 - May 2025\n• Analyzed supplied research data.")
      .put("skills","Programming: Python").put("projects","Forecasting\nBuilt a forecasting model.");
  r.putObject("contact").put("name","Research Candidate");
  var pdf=new PdfService("C:/Windows/Fonts/calibri.ttf","C:/Windows/Fonts/calibrib.ttf");
  String text=pdf.extract(pdf.create(r,"Medical Field","Calibri","right")).text();
  assertTrue(text.contains("RESEARCH & PROFESSIONAL EXPERIENCE"));
  assertTrue(text.contains("TECHNICAL SKILLS"));
  assertTrue(text.contains("RESEARCH & TECHNICAL PROJECTS"));
  assertFalse(text.contains("CLINICAL & PROFESSIONAL SKILLS"));
  assertFalse(ResumeProfiles.bioinformatics("Medical Field",new ObjectMapper().createObjectNode().put("summary","Registered nurse providing patient care.")));
 }
}
