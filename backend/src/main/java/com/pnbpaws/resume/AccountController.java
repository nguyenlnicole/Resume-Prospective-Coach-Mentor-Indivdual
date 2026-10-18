package com.pnbpaws.resume;
import static org.springframework.http.HttpStatus.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
class AccountController {
 @org.springframework.beans.factory.annotation.Autowired RememberedSignIn remembered;
 private final RegisteredAccounts accounts; private final CoachAccess access; private final AdminService admin;
 AccountController(RegisteredAccounts accounts,CoachAccess access,AdminService admin){this.accounts=accounts;this.access=access;this.admin=admin;}
 record Signup(String email,String password,String edition){}
 record Login(String email,String password,boolean keepLoggedIn){}
 record Decision(String email,String decision,Long expiresAt){}
 record Removal(String email,String confirmation,boolean resetTrial){}
 record Reset(String token,String password){}
 @PostMapping("/accounts/forgot-password") Map<String,Boolean> forgot(@RequestBody Trial input,HttpServletRequest req){
  admin.limit("password-reset-ip:"+req.getRemoteAddr(),5);String email=access.email(input.email());admin.limit("password-reset-email:"+email,3);
  accounts.requestPasswordReset(email);return Map.of("ok",true);
 }
 @PostMapping("/accounts/reset-password") Map<String,Boolean> reset(@RequestBody Reset input,HttpServletRequest req){
  admin.limit("password-reset-submit:"+req.getRemoteAddr(),10);accounts.resetPassword(input.token(),input.password());return Map.of("ok",true);
 }
 @PostMapping("/accounts/signup") Map<String,Boolean> signup(@RequestBody Signup input,HttpServletRequest req){
  admin.limit("signup:"+req.getRemoteAddr(),5);String email=access.email(input.email());accounts.register(email,input.password(),input.edition());access.sendCode(email);return Map.of("ok",true);
 }
 record Trial(String email,String edition){}
 @PostMapping("/accounts/trial") Map<String,Boolean> trial(@RequestBody Trial input,HttpServletRequest req){
  admin.limit("trial-signup:"+req.getRemoteAddr(),5);String email=access.email(input.email());accounts.startTrial(email,input.edition());access.sendCode(email);return Map.of("ok",true);
 }
 @PostMapping("/accounts/create-password") Map<String,Boolean> createPassword(@RequestBody Login input,HttpSession session){
  var state=access.state(session);if(!Boolean.TRUE.equals(state.get("registered")) || "verify-email".equals(state.get("state")))throw new ResponseStatusException(UNAUTHORIZED,"Verify your email first.");
  accounts.requestAccount(state.get("email").toString(),input.password());return Map.of("ok",true);
 }
 @PostMapping("/accounts/login") Map<String,Object> login(@RequestBody Login input,HttpServletRequest req,HttpServletResponse response){
  admin.limit("account-login:"+req.getRemoteAddr(),15);String email=access.email(input.email());admin.limit("account-login-email:"+email,10);
  if(!accounts.matches(email,input.password()))throw new ResponseStatusException(UNAUTHORIZED,"Email or password is incorrect.");
  var previous=req.getSession(false);if(previous!=null)previous.invalidate();var session=req.getSession(true);session.setMaxInactiveInterval(15*60);
  session.setAttribute("coachEmail",email);session.setAttribute("coachToken",access.issueToken(email));remembered.choose(req,response,input.keepLoggedIn());return access.state(session);
 }
 @PostMapping("/accounts/subscribe") Map<String,Boolean> subscribe(HttpSession session){
  var state=access.state(session);if(!Boolean.TRUE.equals(state.get("registered")))throw new ResponseStatusException(UNAUTHORIZED,"Sign in first.");
  accounts.subscribe(state.get("email").toString());return Map.of("ok",true);
 }
 @PostMapping("/accounts/restore") Map<String,Object> restore(HttpSession session)throws Exception{access.requireTools(session);return accounts.restore(session);}
 @GetMapping("/admin/accounts") List<Map<String,Object>> list(HttpSession session){access.requireAdmin(session);return accounts.dashboard();}
 @PostMapping("/admin/accounts/delete") List<Map<String,Object>> remove(@RequestBody Removal input,HttpSession session){
  access.requireAdmin(session);String email=access.email(input.email());
  if(!email.equals(input.confirmation()))throw new ResponseStatusException(BAD_REQUEST,"Type the account email to confirm.");
  accounts.remove(email,input.resetTrial());return accounts.dashboard();
 }
 @PostMapping("/admin/accounts") List<Map<String,Object>> decide(@RequestBody Decision input,HttpSession session){access.requireAdmin(session);accounts.decide(access.email(input.email()),input.decision(),input.expiresAt());return accounts.dashboard();}
}
