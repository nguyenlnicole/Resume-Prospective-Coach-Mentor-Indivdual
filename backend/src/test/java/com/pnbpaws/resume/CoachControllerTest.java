package com.pnbpaws.resume;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class CoachControllerTest {
  @Test void codeVerificationSetsFifteenMinuteIdleTimeoutAndNotifiesOwner() {
    CoachAccess access = mock(CoachAccess.class);
    AdminService admin = mock(AdminService.class);
    OwnerActivityNotifications notifications = mock(OwnerActivityNotifications.class);
    BetaPasses passes = mock(BetaPasses.class);
    when(access.verify("tester@example.com", "123456")).thenReturn("session-token");
    when(access.email("tester@example.com")).thenReturn("tester@example.com");
    when(access.state(any(HttpSession.class))).thenReturn(Map.of("role", "coach", "allowed", true));

    var request = new MockHttpServletRequest();
    var controller = new CoachController(access, admin, notifications, passes, mock(CoachApprovalLinks.class));
    controller.remembered=mock(RememberedSignIn.class);
    controller.verify(new CoachController.Code("tester@example.com", "123456", "beta", false), request, new org.springframework.mock.web.MockHttpServletResponse());

    assertEquals(15 * 60, request.getSession().getMaxInactiveInterval());
    verify(notifications).signedIn("tester@example.com", "User");
    verify(passes).activateAssigned(request.getSession());
  }

  @Test void betaCodeIsOnlySentToAnAssignedEmail() {
    CoachAccess access = mock(CoachAccess.class);
    BetaPasses passes = mock(BetaPasses.class);
    var controller = new CoachController(access, mock(AdminService.class),
        mock(OwnerActivityNotifications.class), passes, mock(CoachApprovalLinks.class));
    var request = new MockHttpServletRequest();
    var input = new CoachController.Address("tester@example.com", "beta");
    assertThrows(ResponseStatusException.class, () -> controller.code(input, request));
    verify(access, never()).sendCode(any());
    when(passes.eligibleForSignIn("tester@example.com")).thenReturn(true);
    controller.code(input, request);
    verify(access).sendCode("tester@example.com");
  }
  @Test void newCoachRequestNotifiesOwnerButSendsNoCodeUntilApproval() {
    CoachAccess access = mock(CoachAccess.class);
    AdminService admin = mock(AdminService.class);
    OwnerActivityNotifications notifications = mock(OwnerActivityNotifications.class);
    CoachApprovalLinks links = mock(CoachApprovalLinks.class);
    when(access.email("new@example.com")).thenReturn("new@example.com");
    when(access.requestAccess("new@example.com")).thenReturn(true);
    when(links.create("new@example.com")).thenReturn("approval-token");
    var controller = new CoachController(access, admin, notifications, mock(BetaPasses.class), links);
    var request = new MockHttpServletRequest();
    controller.requestAccess(new CoachController.Address("new@example.com", "coach"), request);
    verify(notifications).coachAccessRequested("new@example.com",
        "https://crp.pnbpaws.com/coach-approve.html?token=approval-token");
    verify(access, never()).sendCode(any());
    assertEquals("new@example.com", request.getSession().getAttribute("pendingCoachEmail"));
  }
}
