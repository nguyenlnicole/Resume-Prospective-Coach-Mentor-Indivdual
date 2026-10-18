package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class ReferenceFormattingTest {
  ObjectNode resume() throws Exception {
    var r = (ObjectNode) new ObjectMapper().readTree(getClass().getResourceAsStream("/approved-reference.json"));
    r.put("projects", "Care Portal | [Demo](https://care.example/demo) | [GitHub Repo](https://github.com/example/care) — Java, React\nBuilt a care portal with clear records and access controls.");
    r.put("education", "Example University — Bachelor of Science in Software Engineering\nRelevant coursework: databases");
    r.put("certifications", "Example Award – Scrum.org | January 2024");
    return r;
  }

  static class Styles extends PDFTextStripper {
    final StringBuilder text = new StringBuilder();
    final List<String> fonts = new ArrayList<>();
    Styles() throws IOException { }
    @Override protected void processTextPosition(TextPosition position) {
      String value = position.getUnicode();
      text.append(value);
      for (int i = 0; i < value.length(); i++) fonts.add(position.getFont().getName().toLowerCase());
      super.processTextPosition(position);
    }
    void assertStyle(String value, boolean bold, boolean italic) {

      String compact = text.toString().replace(" ", "");
      List<String> compactFonts = new ArrayList<>();
      for(int i=0;i<text.length();i++) if(text.charAt(i)!=' ') compactFonts.add(fonts.get(i));
      String target=value.replace(" ", ""); int start=compact.indexOf(target);
      assertTrue(start >= 0, value);
      for(int i=start;i<start+target.length();i++) {
        assertEquals(bold,compactFonts.get(i).contains("bold"),value);
        assertEquals(italic,compactFonts.get(i).contains("italic") || compactFonts.get(i).contains("oblique"),value);
      }
    }
  }

  @Test void pdfUsesReferenceEmphasisAndPreservesAllLinkTargets() throws Exception {
    PdfService service = new PdfService("C:/Windows/Fonts/calibri.ttf", "C:/Windows/Fonts/calibrib.ttf");
    try (PDDocument pdf = Loader.loadPDF(service.create(resume(), "Senior", "Calibri", "right"))) {
      assertEquals(1,pdf.getNumberOfPages());
      Styles styles=new Styles();styles.getText(pdf);
      styles.assertStyle("Cedar Example Labs",true,false);
      styles.assertStyle("Lead Engineer",false,true);
      styles.assertStyle("TECHNICAL SKILLS",true,false);
      styles.assertStyle("Languages:",true,false);
      styles.assertStyle("Care Portal",true,false);
      styles.assertStyle("Built a care portal",false,false);
      styles.assertStyle("Example Award",false,false);
      styles.assertStyle("Scrum.org",false,true);
      styles.assertStyle("January 2024",true,false);
      Set<String> links=new HashSet<>();
      for(var annotation:pdf.getPage(0).getAnnotations())
        if(annotation instanceof PDAnnotationLink link && link.getAction() instanceof PDActionURI action) links.add(action.getURI());
      assertTrue(links.contains("https://care.example/demo"));
      assertTrue(links.contains("https://github.com/example/care"));
      assertTrue(links.contains("mailto:jordan@example.com"));
      String output=System.getProperty("reference.preview.output");
      if(output!=null) javax.imageio.ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0,120),"png",Path.of(output).toFile());
    }
    assertEquals("", PdfService.formattedProjects(""), "No phantom bullet/section for missing optional projects");
  }

  @Test void wordUnderlinesHeadingsAndDoesNotBoldProjectDescriptions() throws Exception {
    String document="",relationships="";
    try(var zip=new ZipInputStream(new ByteArrayInputStream(new DocxService().create(resume(),"Senior","Calibri","right","blue")))) {
      for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) {
        String text=new String(zip.readAllBytes(),StandardCharsets.UTF_8);
        if(entry.getName().equals("word/document.xml"))document=text;
        if(entry.getName().equals("word/_rels/document.xml.rels"))relationships=text;
      }
    }
    assertTrue(document.matches("(?s).*<w:b/><w:u w:val=\"single\"/></w:rPr><w:t xml:space=\"preserve\">TECHNICAL PROJECTS</w:t>.*"));
    assertTrue(document.contains("<w:b/></w:rPr><w:t xml:space=\"preserve\">Care Portal</w:t>"));
    assertTrue(document.contains("<w:sz w:val=\"20\"/></w:rPr><w:t xml:space=\"preserve\">Built a care portal with clear records and access controls.</w:t>"));
    assertFalse(document.contains("Relevant coursework"));
    assertTrue(relationships.contains("https://care.example/demo"));
    assertTrue(relationships.contains("https://github.com/example/care"));
    assertTrue(relationships.contains("mailto:jordan@example.com"));
  }
}
