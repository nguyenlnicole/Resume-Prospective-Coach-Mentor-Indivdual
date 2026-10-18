package com.pnbpaws.resume;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class RedemptionPageController {
  @GetMapping({"/redeem", "/redeem/"})
  String redeem(){return "forward:/index.html";}
}
