package com.pnbpaws.resume;

import static org.springframework.http.HttpStatus.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
class AppSumoController {
  private final AppSumoCodes codes;
  private final CoachAccess access;
  private final AdminService admin;
  AppSumoController(AppSumoCodes codes,CoachAccess access,AdminService admin){this.codes=codes;this.access=access;this.admin=admin;}
  record Redemption(String code){}
  record Import(String csv){}
  record Activation(String id,boolean enabled){}
  record Revocation(String email,boolean revoked){}
  @PostMapping("/appsumo/redeem") Map<String,Object> redeem(@RequestBody Redemption input,HttpServletRequest req,HttpServletResponse res){
    res.setHeader("Cache-Control","no-store");
    admin.limit("appsumo-redeem:"+req.getRemoteAddr(),10);
    var state=access.state(req.getSession());
    if(!Boolean.TRUE.equals(state.get("registered")) || "verify-email".equals(state.get("state")) || "guest".equals(state.get("role")))
      throw new ResponseStatusException(UNAUTHORIZED,"Create an account and verify your email, or sign in, before redeeming your code.");
    if(List.of("deleted","revoked").contains(state.get("state").toString()))
      throw new ResponseStatusException(FORBIDDEN,"This account is blocked. Contact support.");
    codes.redeem(state.get("email").toString(),input.code()==null?null:input.code().trim());
    return access.state(req.getSession());
  }
  @GetMapping("/admin/appsumo") List<Map<String,Object>> list(HttpSession session,HttpServletResponse res){access.requireAdmin(session);res.setHeader("Cache-Control","no-store");return codes.batches();}
  @PostMapping("/admin/appsumo/import") Map<String,Object> importCodes(@RequestBody Import input,HttpSession session,HttpServletResponse res){access.requireAdmin(session);res.setHeader("Cache-Control","no-store");return codes.importCodes(input.csv());}
  @PostMapping("/admin/appsumo/activate") List<Map<String,Object>> activate(@RequestBody Activation input,HttpSession session){access.requireAdmin(session);codes.configure(input.id(),input.enabled());return codes.batches();}
  @PostMapping("/admin/appsumo/revoke") Map<String,Boolean> revoke(@RequestBody Revocation input,HttpSession session){access.requireAdmin(session);codes.revoke(access.email(input.email()),input.revoked());return Map.of("ok",true);}
}
