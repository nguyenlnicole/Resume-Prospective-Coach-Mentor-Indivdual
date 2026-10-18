package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JobPrepEvidenceTest {
  @Test void unsearchedCompanyClaimsBecomeUnknownAndQuestionsKeepEvidenceLimits()throws Exception{
    var mapper=new ObjectMapper();var sheet=mapper.createObjectNode();
    var company=sheet.putObject("company").put("name","Example Company");
    company.putObject("founded").put("text","2025; month not verified").put("sourceUrl","https://example.com/about").put("sourceTitle","About");
    company.putObject("employees").put("text","999 employees").put("sourceUrl","https://invented.example/count");
    var questions=sheet.putArray("interviewQuestions");
    for(int i=0;i<4;i++){
      var question=questions.addObject().put("question","Practice question "+i).put("whyLikely","Related to the posting").put("howToPrepare","Prepare a real example");
      question.putArray("sources").addObject().put("text","Reported interview theme").put("sourceUrl",i==0?"https://example.com/review":"https://invented.example/review").put("sourceTitle","Review");
    }
    JobPrepEvidence.verify(mapper,sheet,Set.of("https://example.com/about","https://example.com/review"));
    assertEquals("2025; month not verified",sheet.path("company").path("founded").path("text").asText());
    assertTrue(sheet.path("company").path("employees").path("text").asText().startsWith("Not verified"));
    assertEquals(4,sheet.path("interviewQuestions").size());
    assertEquals(1,sheet.path("interviewQuestions").get(0).path("sources").size());
    assertEquals(0,sheet.path("interviewQuestions").get(1).path("sources").size());
    assertTrue(sheet.path("interviewQuestions").get(1).path("basis").asText().contains("inferred from the posting"));
  }
}
