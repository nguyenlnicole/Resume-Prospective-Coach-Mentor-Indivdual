package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

final class JobPrepEvidence {
  static void verify(ObjectMapper mapper,ObjectNode sheet,Set<String> sources){
    ObjectNode company=mapper.createObjectNode().put("name",sheet.path("company").path("name").asText());
    for(String key:List.of("founders","founded","bio","employees")){
      JsonNode supplied=sheet.path("company").path(key);
      ObjectNode item=mapper.createObjectNode();
      if(valid(supplied,sources))item.put("text",supplied.path("text").asText().strip()).put("sourceUrl",supplied.path("sourceUrl").asText()).put("sourceTitle",supplied.path("sourceTitle").asText());
      else item.put("text","Not verified from the available public sources.").put("sourceUrl","").put("sourceTitle","");
      company.set(key,item);
    }
    sheet.set("company",company);
    if(sheet.has("interviewQuestions")){
      ArrayNode questions=mapper.createArrayNode();
      for(JsonNode question:sheet.path("interviewQuestions")){
        if(question.path("question").asText().isBlank() || questions.size()>=4)continue;
        ObjectNode item=mapper.createObjectNode().put("question",question.path("question").asText()).put("whyLikely",question.path("whyLikely").asText()).put("howToPrepare",question.path("howToPrepare").asText());
        ArrayNode evidence=mapper.createArrayNode();
        for(JsonNode source:question.path("sources"))if(valid(source,sources))evidence.add(source.deepCopy());
        item.set("sources",evidence);
        item.put("basis",evidence.isEmpty()?"Practice question inferred from the posting; no verified interview report attached.":"Likely question informed by public sources; it is not guaranteed to be asked.");
        questions.add(item);
      }
      if(questions.size()!=4)throw new ResponseStatusException(BAD_GATEWAY,"The four interview questions need another research pass. Please try again.");
      sheet.set("interviewQuestions",questions);
    }
  }
  private static boolean valid(JsonNode item,Set<String> sources){
    String url=item.path("sourceUrl").asText();
    return !item.path("text").asText().isBlank() && sources.contains(url) && PdfService.safeUrl(url);
  }
}
