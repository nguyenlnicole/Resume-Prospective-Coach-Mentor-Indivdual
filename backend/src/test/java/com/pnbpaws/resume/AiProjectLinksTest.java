package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiProjectLinksTest {
  @Test
  void hiddenPdfLinksReturnToTheMatchingProjectOnly() {
    ObjectNode resume = new ObjectMapper().createObjectNode();
    resume.put("projects", "CareTrack | Demo | GitHub\nTech: Java\nBuilt care tools.\n\nFitToGo\nTech: React\nBuilt fitness tools.");
    AiService.restoreProjectLinks(resume, List.of(
        Map.of("label", "GitHub", "context", "CareTrack | Demo | GitHub",
            "url", "https://github.com/example/caretrack"),
        Map.of("label", "GitHub", "context", "Example Candidate | GitHub",
            "url", "https://github.com/example-person")));
    String projects = resume.path("projects").asText();
    assertTrue(projects.contains("CareTrack | https://github.com/example/caretrack"), projects);
    assertFalse(projects.contains("https://github.com/example-person"));
    assertFalse(projects.split("\\n\\s*\\n")[1].contains("github.com"));
  }
  @Test void proposalKeepsProjectLinkHeadingsWhenAiOmitsThem() throws Exception {
    var mapper=new ObjectMapper();var original=mapper.createObjectNode();
    original.put("projects","Virus Blasters | https://game.example | https://github.com/me/virus — C#\nOriginal description.\n\nTriller | https://github.com/me/triller — Angular\nOriginal description.");
    var input=mapper.createObjectNode();input.set("resume",original);
    var result=mapper.createObjectNode();result.putObject("resume").put("projects","Virus Blasters — C#\nReworded description.\n\nTriller — Angular\nOther rewording.");
    var projects=AiService.prepareProposal(input,result).path("resume").path("projects").asText();
    assertTrue(projects.contains("Virus Blasters | https://game.example | https://github.com/me/virus — C#\nReworded description."));
    assertTrue(projects.contains("Triller | https://github.com/me/triller — Angular\nOther rewording."));
    assertEquals(projects,ResumeProjectLinks.preserve(original.path("projects").asText(),projects));
  }
}
