package com.pnbpaws.resume;

import jakarta.servlet.http.*;
import java.time.Clock;
import java.util.*;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class RememberedSignIn {
  @org.springframework.beans.factory.annotation.Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies;
  static final String COOKIE = "PNB_REMEMBER";
  static final int DAYS = 90;
  private final JdbcTemplate db;
  private final CoachAccess access;
  private final Clock clock;
  RememberedSignIn(JdbcTemplate db, CoachAccess access, Clock clock) {
    this.db=db; this.access=access; this.clock=clock;
    db.execute("ALTER TABLE coach ADD COLUMN IF NOT EXISTS remember_until BIGINT");
  }
  void choose(HttpServletRequest request, HttpServletResponse response, boolean remember) {
    var session=request.getSession();
    String email=Objects.toString(session.getAttribute("coachEmail"), "");
    String token=Objects.toString(session.getAttribute("coachToken"), "");
    boolean enabled=remember && Boolean.TRUE.equals(access.state(session).get("allowed"));
    db.update("UPDATE coach SET remember_until=? WHERE email=? AND session_hash=?",
        enabled ? clock.millis()+DAYS*86400000L : null, email, AdminService.digest(token));
    session.setAttribute("keepLoggedIn", enabled);
    cookie(request,response,enabled?token:"",enabled?DAYS*86400:0);
  }
  void check(HttpServletRequest request, HttpServletResponse response) {
    if (!request.getRequestURI().startsWith("/api/")) return;
    var session=request.getSession(false);
    if(session!=null && access.isAdmin(session)) return;
    String token=Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
        .filter(c->COOKIE.equals(c.getName())).map(Cookie::getValue).findFirst().orElse("");
    if ((session==null || session.getAttribute("coachToken")==null) && !token.isBlank()) {
      var rows=token.matches("[A-Za-z0-9_-]{43}") ? db.queryForList(
          "SELECT email FROM coach WHERE session_hash=? AND remember_until>?",AdminService.digest(token),clock.millis()) : List.<Map<String,Object>>of();
      if(rows.isEmpty()) { clear(request,response); return; }
      session=request.getSession(true);
      request.changeSessionId();
      session.setMaxInactiveInterval(15*60);
      session.setAttribute("coachEmail",rows.getFirst().get("EMAIL"));
      session.setAttribute("coachToken",token);
      session.setAttribute("keepLoggedIn",true);
    }
    if(session==null || session.getAttribute("coachToken")==null) return;
    var state=access.state(session);
    if(List.of("expired","revoked","deleted","sign-in").contains(state.get("state"))) {
      access.signOut(session);
      clear(request,response);
    } else if(Boolean.TRUE.equals(session.getAttribute("keepLoggedIn"))) {
      choose(request,response,true);
    }
  }
  void clear(HttpServletRequest request,HttpServletResponse response) { cookie(request,response,"",0); }
  private void cookie(HttpServletRequest request,HttpServletResponse response,String value,int maxAge) {
    response.addHeader("Set-Cookie",ResponseCookie.from(COOKIE,value).httpOnly(true)
        .secure(secureCookies || request.isSecure()).sameSite("Strict").path("/api").maxAge(maxAge).build().toString());
  }
}
