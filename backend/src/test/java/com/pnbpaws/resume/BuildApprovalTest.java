package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class BuildApprovalTest {
  final ObjectMapper mapper = new ObjectMapper();
  ObjectNode source() {
    ObjectNode data = mapper.createObjectNode();
    data.putObject("resume").put("experience", "Example — Engineer | 2020–Present\n• Built a service")
        .put("certifications", "Example Certificate | 2021");
    return data;
  }
  ObjectNode proposal(ObjectNode data) {
    ObjectNode result = mapper.createObjectNode();
    result.set("resume", data.path("resume").deepCopy());
    result.putArray("approvalQuestions");
    return result;
  }
  @Test void clarificationReturnsAProposalInsteadOfBlocking() {
    var data = source(); var result = proposal(data);
    result.withArray("approvalQuestions").addObject().put("question", "May we omit this credential?");
    assertSame(result, AiService.prepareProposal(data, result));
    assertEquals(1, result.path("approvalQuestions").size());
    assertTrue(result.path("protectedChanges").isEmpty());
  }
  @Test void removedHistoryAndCredentialsRequireExplicitReviewEvenWithoutModelQuestions() {
    var data = source(); var unchanged = data.deepCopy(); var result = proposal(data);
    ((ObjectNode) result.path("resume")).put("experience", "").put("certifications", "");
    AiService.prepareProposal(data, result);
    assertEquals(2, result.path("protectedChanges").size());
    assertEquals(data.path("resume").path("experience").asText(), result.path("protectedChanges").get(0).path("before").asText());
    assertEquals("", result.path("protectedChanges").get(0).path("after").asText());
    assertEquals(unchanged, data);
  }
  @Test void ordinaryBulletEditsKeepProtectedHistory() {
    var data = source(); var result = proposal(data);
    ((ObjectNode) result.path("resume")).put("experience", "Example — Engineer | 2020–Present\n• Implemented the service");
    AiService.prepareProposal(data, result);
    assertTrue(result.path("protectedChanges").isEmpty());
  }
  @Test void missingDraftIsRejected() {
    assertThrows(ResponseStatusException.class, () -> AiService.prepareProposal(source(), mapper.createObjectNode()));
  }
  @Test void inventedEmployerCannotEnterProposedExperience() {
    var data = source(); var result = proposal(data);
    ((ObjectNode) result.path("resume")).put("experience",
        "Flex\nSoftware Engineer | 2024–Present\n• Built an invented system");
    AiService.prepareProposal(data, result);
    assertEquals(data.path("resume").path("experience").asText(),
        result.path("resume").path("experience").asText());
    assertEquals("Flex", AiService.unverifiedEmployer(
        "Flex\nSoftware Engineer | 2024–Present\n• Built a system", "Example — Engineer | 2020–Present"));
    assertNull(AiService.unverifiedEmployer(
        "Example\nSoftware Engineer | 2024–Present\n• Built a system", "Example — Engineer | 2020–Present"));
  }
}
