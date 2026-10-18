package com.pnbpaws.resume;

import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api")
class CoachController {
  @org.springframework.beans.factory.annotation.Autowired RegisteredAccounts accounts;
  @org.springframework.beans.factory.annotation.Autowired RememberedSignIn remembered;
  private final CoachAccess access;
  private final AdminService admin;
  private final OwnerActivityNotifications ownerNotifications;
  private final BetaPasses betaPasses;
  private final CoachApprovalLinks approvals;
  CoachController(CoachAccess access, AdminService admin, OwnerActivityNotifications ownerNotifications,
      BetaPasses betaPasses, CoachApprovalLinks approvals) {
    this.access = access; this.admin = admin; this.ownerNotifications = ownerNotifications;
    this.betaPasses = betaPasses;
    this.approvals = approvals;
  }
  record Address(String email, String edition) {}
  record Code(String email, String code, String edition, boolean keepLoggedIn) {}
  record Approval(String email, String status) {}
  record Activation(boolean confirm) {}
  @PostMapping("/coach/request-access") Map<String, Boolean> requestAccess(@RequestBody Address input, HttpServletRequest request) {
    admin.limit("coach-request-ip:" + request.getRemoteAddr(), 10);
    if ("beta".equals(input.edition())) {
      String email = access.email(input.email());
      admin.limit("beta-request-email:" + email, 3);
      boolean approved = betaPasses.eligibleForSignIn(email);
      if (approved) access.sendCode(email);
      else ownerNotifications.betaAccessRequested(email,
          "https://crp.pnbpaws.com/coach-approve.html?token=" + approvals.create(email, "beta"));
      request.getSession().setAttribute("pendingCoachEmail", email);
      request.getSession().setAttribute("pendingAccessEdition", "beta");
      return Map.of("ok", true, "approved", approved);
    }
    request.getSession().setAttribute("pendingAccessEdition", "coach");
    return requestCoach(input.email(), request);
  }
  private Map<String, Boolean> requestCoach(String address, HttpServletRequest request) {
    String email = access.email(address);
    if (access.requestAccess(email)) {
      String link = "https://crp.pnbpaws.com/coach-approve.html?token=" + approvals.create(email);
      ownerNotifications.coachAccessRequested(email, link);
    }
    request.getSession().setAttribute("pendingCoachEmail", email);
    boolean approved = access.isApproved(email);
    if (approved) access.sendCode(email);
    return Map.of("ok", true, "approved", approved);
  }
  @GetMapping("/coach/request-status") Map<String, Boolean> requestStatus(HttpSession session) {
    String email = Objects.toString(session.getAttribute("pendingCoachEmail"), "");
    return Map.of("approved", !email.isBlank() && ("beta".equals(session.getAttribute("pendingAccessEdition"))
        ? betaPasses.eligibleForSignIn(email) : access.isApproved(email)));
  }
  @GetMapping("/access") Map<String, Object> state(HttpSession session) { var result=new HashMap<>(access.state(session)); result.put("keepLoggedIn",Boolean.TRUE.equals(session.getAttribute("keepLoggedIn"))); return result; }
  @PostMapping("/coach/code") Map<String, Boolean> code(@RequestBody Address input, HttpServletRequest request) {
    admin.limit("coach-code-ip:" + request.getRemoteAddr(), 10);
    if (accounts != null && accounts.exists(access.email(input.email()))) { access.sendCode(input.email()); return Map.of("ok",true); }
    if ("beta".equals(input.edition()) && !betaPasses.eligibleForSignIn(input.email()))
      throw new ResponseStatusException(FORBIDDEN, "This email has not been assigned resume access. Ask the app owner to assign it.");
    if (!"beta".equals(input.edition())) access.requireCodeEligibility(input.email());
    access.sendCode(input.email());
    return Map.of("ok", true, "pending", false);
  }
  @PostMapping("/coach/verify") Map<String, Object> verify(@RequestBody Code input, HttpServletRequest request, HttpServletResponse response) {
    admin.limit("coach-verify:" + request.getRemoteAddr(), 15);
    String token = access.verify(input.email(), input.code());
    if (token == null) throw new ResponseStatusException(UNAUTHORIZED, "This code is invalid or expired. Request a new code if needed.");
    var session = request.getSession();
    request.changeSessionId();
    session.removeAttribute("adminVersion");
    session.removeAttribute("reviewedLevel");
    session.setMaxInactiveInterval(15 * 60);
    session.setAttribute("coachEmail", access.email(input.email()));
    session.setAttribute("coachToken", token);
    if (accounts != null && accounts.exists(access.email(input.email()))) accounts.verified(access.email(input.email()));
    else if ("beta".equals(input.edition())) betaPasses.activateAssigned(session);
    remembered.choose(request,response,input.keepLoggedIn());
    var state = state(session);
    ownerNotifications.signedIn(access.email(input.email()), "beta".equals(input.edition()) ? "User" : "Coach or mentor");
    return state;
  }
  @PostMapping("/coach/logout") Map<String, Boolean> logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) { access.signOut(session); remembered.clear(request,response); return Map.of("ok", true); }
  @GetMapping("/admin/coaches") Map<String, Object> dashboard(HttpSession session) { access.requireAdmin(session); return access.dashboard(); }
  @PostMapping("/admin/coaches") Map<String, Object> approve(@RequestBody Approval input, HttpSession session) {
    access.requireAdmin(session);
    access.setStatus(input.email(), input.status());
    if ("approved".equals(input.status())) access.sendCode(input.email());
    return access.dashboard();
  }
}
