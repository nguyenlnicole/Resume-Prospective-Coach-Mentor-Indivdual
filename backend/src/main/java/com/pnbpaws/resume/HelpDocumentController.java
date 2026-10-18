package com.pnbpaws.resume;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
class HelpDocumentController {
  private final HelpDocuments documents;
  HelpDocumentController(HelpDocuments documents) { this.documents = documents; }
  @GetMapping("/api/help-documents/{token}") ResponseEntity<byte[]> download(@PathVariable("token") String token) {
    var document = documents.get(token);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(HelpDocuments.DOCX_TYPE))
        .header("Content-Disposition", ContentDisposition.attachment().filename(document.filename()).build().toString())
        .header("Cache-Control", "no-store").header("Referrer-Policy", "no-referrer")
        .body(document.bytes());
  }
}
