package com.pnbpaws.resume;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProjectWordingControllerTest {
  @Test void rejectsBlankBeforeCallingWriter() {
    var ai=mock(AiService.class);
    var controller=new ProjectWordingController(mock(CoachAccess.class),mock(AdminService.class),ai);
    assertThrows(ResponseStatusException.class, () -> controller.shorten(new ProjectWordingController.Input(" "),new MockHttpServletRequest()));
    verifyNoInteractions(ai);
  }
  @Test void returnsSuggestionWithoutReplacingSource() {
    var ai=mock(AiService.class); var access=mock(CoachAccess.class);
    var controller=new ProjectWordingController(access,mock(AdminService.class),ai);
    var request=new MockHttpServletRequest();
    when(ai.shortenProject("Original project details")).thenReturn("Suggested wording");
    assertEquals("Suggested wording",controller.shorten(new ProjectWordingController.Input("Original project details"),request).get("text"));
    verify(access).requireTools(request.getSession());
  }
}
