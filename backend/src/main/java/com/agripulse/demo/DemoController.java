package com.agripulse.demo;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo tooling. Active on the {@code demo} profile only, intentionally open
 * (permitAll) so a fresh demo database — which contains no users yet — can
 * be bootstrapped. Never enable this profile against real data.
 */
@RestController
@RequestMapping("/api/demo")
@Profile("demo")
@PreAuthorize("permitAll")
public class DemoController {

  private final DemoDataService demoDataService;

  public DemoController(DemoDataService demoDataService) {
    this.demoDataService = demoDataService;
  }

  @PostMapping("/reset")
  public DemoDataService.DemoSummary reset() {
    return demoDataService.reset();
  }

  @PostMapping("/scenarios/{name}")
  public DemoDataService.DemoSummary scenario(@PathVariable String name) {
    return demoDataService.applyScenario(name);
  }

  @GetMapping("/status")
  public DemoStatus status() {
    return new DemoStatus(
        List.of("healthy", "high-supply", "suspicious", "no-storage", "transport-shortage",
            "strong-demand"),
        new DemoCredentials(
            "farmer001@demo.local",
            "buyer01@demo.local",
            "admin@demo.local",
            DemoDataService.PASSWORD));
  }

  public record DemoStatus(List<String> scenarios, DemoCredentials credentials) {}

  public record DemoCredentials(String farmer, String buyer, String admin, String password) {}
}
