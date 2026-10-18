package com.pnbpaws.resume;

import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import jakarta.servlet.http.*;

@Configuration
class AccessConfig {
  @Bean Clock accessClock() { return Clock.systemUTC(); }
  @Bean WebMvcConfigurer accessGate(CoachAccess access) {
    return new WebMvcConfigurer() {
      @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
          @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            access.requireTools(request.getSession());
            response.setHeader("Cache-Control", "no-store");
            return true;
          }
        }).addPathPatterns("/api/review", "/api/review/finished", "/api/review/email-feedback", "/api/job-prep", "/api/cover-letter", "/api/review/owner-copy", "/api/builder/start", "/api/build", "/api/resume/**", "/api/job-description/**", "/api/feedback/**", "/api/delivery", "/api/beta/resume-copy", "/api/beta/app-rating", "/api/coach/ai-rating");
      }
    };
  }
}
