package com.pnbpaws.resume;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
class SecurityConfig {
  @Bean
  SecurityFilterChain security(HttpSecurity http, RememberedSignIn remembered) throws Exception {
    return http.authorizeHttpRequests(x -> x.anyRequest().permitAll())
        .addFilterBefore(new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
              FilterChain chain) throws ServletException, IOException {
            String host = request.getServerName();
            String path = request.getRequestURI();
            if (("crp.pnbpaws.com".equalsIgnoreCase(host)
                    || "betarp.pnbpaws.com".equalsIgnoreCase(host)
                    || "resumep.pnbpaws.com".equalsIgnoreCase(host))
                && (path.equals("/admin") || path.startsWith("/admin/")
                    || path.startsWith("/api/admin/"))) {
              response.sendError(HttpServletResponse.SC_NOT_FOUND);
              return;
            }
            if ("adminrp.pnbpaws.com".equalsIgnoreCase(host) && path.equals("/")) {
              response.setStatus(HttpServletResponse.SC_FOUND);
              response.setHeader("Location", "/admin/index.html");
              return;
            }
            chain.doFilter(request, response);
          }
        }, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new OncePerRequestFilter() {
          @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
              FilterChain chain) throws ServletException, IOException {
            remembered.check(request, response);
            chain.doFilter(request, response);
          }
        }, org.springframework.security.web.csrf.CsrfFilter.class)
        .formLogin(x -> x.disable())
        .httpBasic(x -> x.disable())
        .headers(
            x ->
                x.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'"
                                + " 'unsafe-inline' https://fonts.googleapis.com; font-src 'self'"
                                + " https://fonts.gstatic.com; img-src 'self' data: blob:;"
                                + " connect-src 'self' https://api.github.com; frame-src 'self' blob:; frame-ancestors"
                                + " 'none'; object-src 'none'; base-uri 'self'; form-action"
                                + " 'self'")))
        .build();
  }
}
