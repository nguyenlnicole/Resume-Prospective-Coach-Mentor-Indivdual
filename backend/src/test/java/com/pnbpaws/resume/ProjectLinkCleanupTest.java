package com.pnbpaws.resume;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProjectLinkCleanupTest {
 @Test void removesDuplicateStandaloneMarkdownLinksForExports() {
  String url="https://github.com/example/controller";
  var resume=new ObjectMapper().createObjectNode().put("projects","Controller | [Repo]("+url+")\n[GitHub Repo]("+url+")\nBuilt a controller.");
  var text=ResumeCleanup.normalize(resume).path("projects").asText();
  assertEquals(1,text.split(java.util.regex.Pattern.quote(url),-1).length-1);
  assertFalse(text.contains("GitHub Repo"));assertTrue(text.contains("Built a controller."));
 }
}
