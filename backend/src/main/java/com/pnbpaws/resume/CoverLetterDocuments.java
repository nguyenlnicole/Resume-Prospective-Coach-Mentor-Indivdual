package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
class CoverLetterDocuments {
  private final String font;
  CoverLetterDocuments(@Value("${app.pdf-font}") String font){this.font=font;}
  byte[] create(String text,JsonNode contact) {
    return create(text,contact,"cover letter");
  }
  byte[] create(String text,JsonNode contact,String label) {
    if(text==null || text.isBlank() || text.length()>12000 || (contact!=null && contact.toString().length()>2000))
      throw new ResponseStatusException(BAD_REQUEST,"Add a " + label + " of up to 12,000 characters.");
    Path file=List.of(font,"C:/Windows/Fonts/calibri.ttf","/usr/share/fonts/truetype/crosextra/Carlito-Regular.ttf","/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")
        .stream().map(Path::of).filter(Files::isRegularFile).findFirst().orElseThrow(()->new ResponseStatusException(SERVICE_UNAVAILABLE,"The document font is unavailable."));
    try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){
      PDFont regular=PDType0Font.load(doc,file.toFile());
      boolean prep=label.equals("job prep sheet");
      PDFont bold=regular;
      if(prep){
        var boldFile=List.of(file.resolveSibling("calibrib.ttf"),file.resolveSibling("Carlito-Bold.ttf"),file.resolveSibling("DejaVuSans-Bold.ttf")).stream().filter(Files::isRegularFile).findFirst();
        if(boldFile.isPresent())bold=PDType0Font.load(doc,boldFile.get().toFile());
      }
      float bodySize=prep?12:11;
      String heading=contact==null?"":contact.path("name").asText();
      String details=contact==null?"":String.join(" | ",List.of("location","phone","email").stream().map(k->contact.path(k).asText()).filter(v->!v.isBlank()).toList());
      var lines=new ArrayList<String>();
      var sourceLinks=new HashMap<Integer,String>();
      String content=(heading.isBlank()?"":heading+"\n"+details+"\n\n")+text;
      for(String paragraph:content.replace("\r","").split("\n",-1)){
        int paragraphStart=lines.size();
        String line="";
        for(String word:paragraph.split("\\s+")){
          if(word.isBlank())continue;

          for(int start=0;start<word.length();){
            int end=start;
            while(end<word.length() && regular.getStringWidth(word.substring(start,end+1))/1000*bodySize<504)end++;
            if(end==start)end++;
            String piece=word.substring(start,end),candidate=line.isEmpty()?piece:line+" "+piece;
            if(!line.isEmpty() && regular.getStringWidth(candidate)/1000*bodySize>504){lines.add(line);line=piece;}else line=candidate;
            start=end;
          }
        }
        lines.add(line);
        if(prep && paragraph.trim().matches("https?://\\S+"))
          for(int index=paragraphStart;index<lines.size();index++)sourceLinks.put(index,paragraph.trim());
      }
      PDPageContentStream stream=null;float y=0;
      try{
        int lineIndex=0;
        for(String line:lines){
          if(stream==null || y<54){if(stream!=null)stream.close();var page=new PDPage(PDRectangle.LETTER);doc.addPage(page);stream=new PDPageContentStream(doc,page);y=738;}
          boolean section=prep && Set.of("JOB PREP SHEET","COMPANY AT A GLANCE","THINGS TO STUDY","WAYS TO PREPARE","FOUR LIKELY INTERVIEW QUESTIONS","OUTSIDE FACTS AND SOURCES","RESEARCH LIMITS").contains(line);
          float size=section?(line.equals("JOB PREP SHEET")?16:13):bodySize;
          if(!line.isEmpty()){stream.beginText();stream.setFont(section?bold:regular,size);stream.newLineAtOffset(54,y);if(line.codePoints().allMatch(c -> c < 256 || (c >= 0x2000 && c <= 0x206f))) {

            for(int cp : line.codePoints().toArray()) stream.showText(new String(Character.toChars(cp)));
          } else stream.showText(line);
          stream.endText();}
          if(sourceLinks.containsKey(lineIndex)){
            var annotation=new PDAnnotationLink();
            annotation.setRectangle(new PDRectangle(54,y-2,Math.min(504,regular.getStringWidth(line)/1000*bodySize),16));
            var action=new PDActionURI();action.setURI(sourceLinks.get(lineIndex));annotation.setAction(action);
            doc.getPage(doc.getNumberOfPages()-1).getAnnotations().add(annotation);
          }
          lineIndex++;y-=prep?18:15;
        }
      }finally{if(stream!=null)stream.close();}
      doc.save(out);return out.toByteArray();
    }catch(IOException|IllegalArgumentException e){throw new ResponseStatusException(BAD_REQUEST,"The " + label + " PDF could not be created. Check the document for unsupported characters.");}
  }
}

@RestController
class CoverLetterDocumentController {
  private final CoverLetterDocuments letters;
  private final PdfService pdf;
  private final CoachAccess access;
  CoverLetterDocumentController(CoverLetterDocuments letters,PdfService pdf,CoachAccess access){this.letters=letters;this.pdf=pdf;this.access=access;}
  record Input(String text,JsonNode contact){}
  @PostMapping("/api/job-prep/document")
  Map<String,Object> jobPrepDocument(@RequestBody Input input,jakarta.servlet.http.HttpSession session,jakarta.servlet.http.HttpServletResponse response){
    access.requireTools(session);response.setHeader("Cache-Control","no-store");
    byte[] bytes=letters.create(input.text(),input.contact(),"job prep sheet");
    return Map.of("pdf",Base64.getEncoder().encodeToString(bytes),"pages",pdf.uploadPreview(bytes));
  }
  @PostMapping("/api/cover-letter/document")
  Map<String,Object> document(@RequestBody Input input,jakarta.servlet.http.HttpSession session,jakarta.servlet.http.HttpServletResponse response){
    access.requireTools(session);response.setHeader("Cache-Control","no-store");
    byte[] bytes=letters.create(input.text(),input.contact());
    return Map.of("pdf",Base64.getEncoder().encodeToString(bytes),"pages",pdf.uploadPreview(bytes));
  }
}
