package com.agripulse.farmer;

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

/** Farmer farms. Owner-scoped; role comes from the JWT. */
@RestController
@RequestMapping("/api/farmer/farms")
@PreAuthorize("hasRole('FARMER')")
public class FarmController {

  private final FarmService farmService;

  public FarmController(FarmService farmService) {
    this.farmService = farmService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FarmResponse create(@Valid @RequestBody CreateFarmRequest request, Principal principal) {
    return farmService.create(request, principal.getName());
  }

  @GetMapping("/mine")
  public List<FarmResponse> listMine(Principal principal) {
    return farmService.listMine(principal.getName());
  }
}
