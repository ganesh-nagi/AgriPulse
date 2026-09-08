package com.agripulse.demand;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Buyer requirements. Owner-scoped; role comes from the JWT. */
@RestController
@RequestMapping("/api/buyer/requirements")
@PreAuthorize("hasRole('BUYER')")
public class BuyerRequirementController {

  private final BuyerRequirementService requirementService;

  public BuyerRequirementController(BuyerRequirementService requirementService) {
    this.requirementService = requirementService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BuyerRequirementResponse create(
      @Valid @RequestBody CreateBuyerRequirementRequest request, Principal principal) {
    return requirementService.create(request, principal.getName());
  }

  @GetMapping("/mine")
  public List<BuyerRequirementResponse> listMine(Principal principal) {
    return requirementService.listMine(principal.getName());
  }
}
