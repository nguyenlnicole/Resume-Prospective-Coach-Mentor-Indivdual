package com.pnbpaws.resume;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LinkedinImportControllerTest {
  @Test void restrictsFetchToPublicProfileUrls() {
    assertEquals("https://www.linkedin.com/in/person",LinkedinImportController.profileUrl("https://linkedin.com/in/person?tracking=1"));
    for(String url:new String[]{"http://linkedin.com/in/person","https://linkedin.com.evil.test/in/person","https://localhost/in/person","https://www.linkedin.com/jobs/123","https://user@linkedin.com/in/person"})
      assertThrows(ResponseStatusException.class,()->LinkedinImportController.profileUrl(url));
  }
  @Test void blockedPageOffersPasteFallbackAndDeniedAccountDoesNotFetch() {
    var access=mock(CoachAccess.class);var pages=mock(JobDescriptionService.class);
    var controller=new LinkedinImportController(access,mock(AdminService.class),pages);
    var request=new MockHttpServletRequest();
    when(pages.fromUrl("https://www.linkedin.com/in/person")).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));
    var error=assertThrows(ResponseStatusException.class,()->controller.read(new LinkedinImportController.Input("https://www.linkedin.com/in/person"),request));
    assertTrue(error.getReason().contains("Paste"));
    reset(pages);
    doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED)).when(access).requireTools(request.getSession());
    assertThrows(ResponseStatusException.class,()->controller.read(new LinkedinImportController.Input("https://www.linkedin.com/in/person"),request));
    verifyNoInteractions(pages);
  }
}
