package com.pnbpaws.resume;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coach/approval")
class CoachApprovalController {
  private final CoachApprovalLinks approvals;

  CoachApprovalController(CoachApprovalLinks approvals) { this.approvals = approvals; }

  @GetMapping("/{token}")
  ResponseEntity<Map<String, Object>> details(@PathVariable String token) {
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(approvals.details(token));
  }

  record Approval(String token) {}
  @PostMapping
  Map<String, String> approve(@RequestBody Approval input) {
    return Map.of("email", approvals.approve(input.token()));
  }
}
