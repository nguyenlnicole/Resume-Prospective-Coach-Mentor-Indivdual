package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class CoverLetterDocumentsTest {
  final CoverLetterDocuments letters=new CoverLetterDocuments("C:/Windows/Fonts/calibri.ttf");
  @Test void pdfPreservesEditedLetterAndContact()throws Exception{
    var contact=new ObjectMapper().createObjectNode().put("name","Example Candidate").put("email","example@example.com");
    byte[] bytes=letters.create("Dear Hiring Team,\n\nI build reliable services and support testing.\n\nThank you,\nExample Candidate",contact);
    try(var doc=Loader.loadPDF(bytes)){
      assertEquals(1,doc.getNumberOfPages());
      String text=new PDFTextStripper().getText(doc);
      assertTrue(text.contains("example@example.com"));assertTrue(text.contains("Dear Hiring Team"));
      assertTrue(text.contains("I build reliable services and support testing."), text);
    }
  }
  @Test void longLettersCreatePagesWithoutDroppingTheEnding()throws Exception{
    String text=("I contribute supported experience in software development and testing.\n\n").repeat(70)+"Final sentence retained.";
    try(var doc=Loader.loadPDF(letters.create(text,null))){assertTrue(doc.getNumberOfPages()>1);assertTrue(new PDFTextStripper().getText(doc).contains("Final sentence retained."));}
  }
  @Test void emptyAndOversizedLettersAreRejected(){assertThrows(Exception.class,()->letters.create("",null));assertThrows(Exception.class,()->letters.create("a".repeat(12001),null));}
  @Test void prepPdfPreservesTopicsFactsAndClickableSources()throws Exception{
    String source="https://example.com/company/source";
    byte[] bytes=letters.create("JOB PREP SHEET\n\nTHINGS TO STUDY\n1. Review the role.\n\nWAYS TO PREPARE\n1. Prepare a real example.\n\nOUTSIDE FACTS AND SOURCES\n1. A supported company fact.\nSource: Employer\n"+source,null,"job prep sheet");
    try(var doc=Loader.loadPDF(bytes)){
      String text=new PDFTextStripper().getText(doc);
      for(String value:java.util.List.of("JOB PREP SHEET","Review the role.","Prepare a real example.","A supported company fact.",source))assertTrue(text.contains(value),value);
      assertTrue(doc.getPage(0).getAnnotations().stream().anyMatch(a->a instanceof org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink link && link.getAction() instanceof org.apache.pdfbox.pdmodel.interactive.action.PDActionURI action && source.equals(action.getURI())));
    }
  }
}
