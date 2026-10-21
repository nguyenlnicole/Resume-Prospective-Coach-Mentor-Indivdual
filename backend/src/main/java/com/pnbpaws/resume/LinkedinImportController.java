package com.pnbpaws.resume;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
@RestController
@RequestMapping("/api/linkedin")
class LinkedinImportController {
  private final CoachAccess access;
  private final AdminService admin;
  private final JobDescriptionService pages;
  LinkedinImportController(CoachAccess access, AdminService admin, JobDescriptionService pages) {this.access=access;this.admin=admin;this.pages=pages;}
  record Input(String url) {}
  static String profileUrl(String value) {
    try {
      URI uri=URI.create(Objects.toString(value, "").strip());
      if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo()!=null || uri.getPort()!=-1
          || !("www.linkedin.com".equalsIgnoreCase(uri.getHost()) || "linkedin.com".equalsIgnoreCase(uri.getHost()))
          || !uri.getPath().matches("/in/[A-Za-z0-9%_-]+/?")) throw new IllegalArgumentException();
      return "https://www.linkedin.com"+uri.getPath();
    } catch(Exception e){throw new ResponseStatusException(BAD_REQUEST,"Enter a LinkedIn profile link or paste the Experience section.");}
  }
  @PostMapping("/experience-text") Map<String,String> read(@RequestBody Input input,HttpServletRequest request) {
    access.requireTools(request.getSession());
    admin.limit("linkedin-import:"+request.getRemoteAddr(),6);
    String url=profileUrl(input.url());
    try { return Map.of("text",pages.fromUrl(url)); }
    catch(ResponseStatusException error){throw new ResponseStatusException(BAD_REQUEST,"This profile could not be read publicly. Paste your LinkedIn Experience section instead.");}
  }
}
