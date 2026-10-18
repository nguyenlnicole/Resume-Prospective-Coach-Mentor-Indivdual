package com.pnbpaws.resume;

import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class AccessErrors {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, String>> expected(ResponseStatusException error) {
    return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", Objects.toString(error.getReason(), "The request could not be completed.")));
  }
}
