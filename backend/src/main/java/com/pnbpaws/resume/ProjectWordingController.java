package com.pnbpaws.resume;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/projects")
class ProjectWordingController {
  private final CoachAccess access;
  private final AdminService admin;
  private final AiService ai;
  ProjectWordingController(CoachAccess access, AdminService admin, AiService ai) {
    this.access = access; this.admin = admin; this.ai = ai;
  }
  record Input(String description, String title, String experience) { Input(String description) { this(description, null, null); } Input(String description, String title) { this(description, title, null); } }
  @PostMapping("/shorten") Map<String, String> shorten(@RequestBody Input input, HttpServletRequest request) {
    access.requireTools(request.getSession());
    if (input.description() == null || input.description().isBlank() || input.description().length() > 6000 || (input.title() != null && input.title().length() > 1500) || (input.experience() != null && input.experience().length() > 20000))
      throw new ResponseStatusException(BAD_REQUEST, "Provide a project description under 6,000 characters.");
    admin.limit("project-wording:" + request.getRemoteAddr(), 12);
    return Map.of("text", ai.rewriteProject(input.description(), input.title(), input.experience()));
  }
}
