package com.agripulse.fpo;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** FPO member visibility and validation. Privacy-safe member views only. */
@RestController
@RequestMapping("/api/fpo")
@PreAuthorize("hasRole('FPO')")
public class FpoController {

  private final FpoService fpoService;

  public FpoController(FpoService fpoService) {
    this.fpoService = fpoService;
  }

  @GetMapping("/members")
  public List<FpoMemberView> members(@RequestParam String region, Principal principal) {
    return fpoService.membersInRegion(region, principal.getName());
  }

  @PostMapping("/validations")
  @ResponseStatus(HttpStatus.CREATED)
  public void validate(
      @Valid @RequestBody ValidateFarmerRequest request, Principal principal) {
    fpoService.validateFarmer(request, principal.getName());
  }
}
