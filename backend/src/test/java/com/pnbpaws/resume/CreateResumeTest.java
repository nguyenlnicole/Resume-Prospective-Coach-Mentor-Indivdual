package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CreateResumeTest {
  @Test void newResumeUsesTheExistingAccountCreditAndReviewSessionPath() throws Exception {
    var mapper = new ObjectMapper();
    var resume = mapper.createObjectNode();
    resume.putObject("contact").put("name", "Alex Rivera");
    var result = mapper.createObjectNode().set("resume", resume);
    var pdf = mock(PdfService.class);
    var access = mock(CoachAccess.class);
    var accounts = mock(RegisteredAccounts.class);
    var notifications = mock(OwnerActivityNotifications.class);
    var history = mock(CoachReviewHistory.class);
    var api = new ApiController(mock(AdminService.class),mock(MailService.class),pdf,mock(FeedbackPdfService.class),mock(ResumeUploadService.class),mock(DocxService.class),mock(AiService.class),mock(JobDescriptionService.class),mock(HelpDocuments.class),access,notifications,history);
    api.accounts=accounts;
    byte[] bytes={1,2,3};
    when(pdf.create(eq(resume),eq("Educator"),anyString(),anyString(),anyString())).thenReturn(bytes);
    when(access.state(any())).thenReturn(Map.of("role","individual"));
    when(accounts.runTrial(any(),eq("review"),eq("Educator"),eq("alex@example.com"),any())).thenReturn(result);
    var request = new MockHttpServletRequest();
    assertSame(result, api.createResume(new ApiController.CreateResumeInput(resume,"Educator","alex@example.com",true),request));
    verify(accounts).runTrial(eq(request.getSession()),eq("review"),eq("Educator"),eq("alex@example.com"),any());
    assertEquals("Educator",request.getSession().getAttribute("reviewedLevel"));
    verifyNoInteractions(history);
    verify(notifications,never()).betaOriginal(anyString(),anyString(),any(),any());
    assertThrows(ResponseStatusException.class,()->api.createResume(new ApiController.CreateResumeInput(resume,"Educator","alex@example.com",false),request));
    verify(pdf,times(1)).create(any(),anyString(),anyString(),anyString(),anyString());
  }
}
