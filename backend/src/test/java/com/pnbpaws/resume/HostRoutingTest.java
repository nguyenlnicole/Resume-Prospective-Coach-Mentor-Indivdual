package com.pnbpaws.resume;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:host-routing;DB_CLOSE_DELAY=-1",
    "app.admin-email=owner@example.com", "app.admin-password-hash=", "app.openai-key=",
    "app.openai-model=", "app.mail-enabled=false"
})
@AutoConfigureMockMvc
class HostRoutingTest {
  @Autowired MockMvc mvc;

  @Test void adminAddressOpensAdminSignIn() throws Exception {
    mvc.perform(get("/").with(request -> {
      request.setServerName("adminrp.pnbpaws.com");
      return request;
    })).andExpect(status().isFound()).andExpect(header().string("Location", "/admin/index.html"));
  }

  @Test void coachAddressDoesNotExposeAdminPagesOrApi() throws Exception {
    for (String host : new String[] {"crp.pnbpaws.com", "betarp.pnbpaws.com", "resumep.pnbpaws.com"}) {
      mvc.perform(get("/admin/index.html").with(request -> {
        request.setServerName(host);
        return request;
      })).andExpect(status().isNotFound());
      mvc.perform(get("/api/admin/me").with(request -> {
        request.setServerName(host);
        return request;
      })).andExpect(status().isNotFound());
    }
  }

  @Test void statusStillWorksOnBothAddresses() throws Exception {
    for (String host : new String[] {"crp.pnbpaws.com", "adminrp.pnbpaws.com"}) {
      mvc.perform(get("/api/status").with(request -> {
        request.setServerName(host);
        return request;
      })).andExpect(status().isOk());
    }
  }
}
