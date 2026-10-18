package com.pnbpaws.resume;

import jakarta.servlet.http.*;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class BetaPassController {
  private final BetaPasses passes;
  private final CoachAccess access;
  private final AdminService admin;
  BetaPassController(BetaPasses passes, CoachAccess access, AdminService admin) {
    this.passes = passes; this.access = access; this.admin = admin;
  }
  record Issue(int count, int days, List<String> emails) {}
  record Assign(String id, String email) {}
  record Redeem(String code) {}
  record Revoke(String id) {}
  @GetMapping("/admin/beta-passes") Map<String, Object> list(HttpSession session, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return passes.dashboard(session);
  }
  @PostMapping("/admin/beta-passes") Map<String, Object> issue(@RequestBody Issue input, HttpSession session, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return passes.issue(session, input.count(), input.days(), input.emails());
  }
  @PostMapping("/admin/beta-passes/assign") Map<String, Object> assign(@RequestBody Assign input, HttpSession session, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return passes.assign(session, input.id(), input.email());
  }
  @PostMapping("/admin/beta-passes/revoke") Map<String, Object> revoke(@RequestBody Revoke input, HttpSession session) {
    passes.revoke(session, input.id()); return passes.dashboard(session);
  }
  @PostMapping("/beta/pass/redeem") Map<String, Object> redeem(@RequestBody Redeem input, HttpServletRequest request) {
    admin.limit("beta-pass:" + request.getRemoteAddr(), 10);
    passes.redeem(request.getSession(), input.code());
    return access.state(request.getSession());
  }
  @PostMapping("/beta/pass/activate") Map<String, Object> activate(HttpSession session) {
    passes.activateAssigned(session);
    return access.state(session);
  }
}
