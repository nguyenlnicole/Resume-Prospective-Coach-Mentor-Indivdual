package com.pnbpaws.resume;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExperienceOrderTest {
 @Test void currentRolesFirstWithTheirOwnBullets(){
  String text="Frontend - Intern | February 2026 - Present\n• Built pages.\n\nDelivery - Driver | September 2013 - December 2015\n• Delivered freight.\n\nPostal - Compliance | December 2015 - Present\n• Reviewed procedures.";
  String result=ExperienceOrder.sort(text);
  assertTrue(result.indexOf("Frontend")<result.indexOf("Postal"));assertTrue(result.indexOf("Postal")<result.indexOf("Delivery"));
  assertTrue(result.contains("Present\n• Reviewed procedures.\n\nDelivery"));
 }
 @Test void groupedPromotionsRetainCompanyAndBullets(){
  String result=ExperienceOrder.sort("Company\nJunior | Jan 2018 - Jan 2020\n• Junior duties.\nSenior | Jan 2020 - Present\n• Senior duties.");
  assertTrue(result.startsWith("Company\nSenior"));assertTrue(result.contains("Company\nJunior"));
 }
}
