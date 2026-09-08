package com.agripulse.supply;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Farmer supply reports. All reads/writes are owner-scoped in the service;
 * role comes from the JWT, never from client input.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('FARMER')")
public class SupplyReportController {

  private final SupplyReportService supplyReportService;

  public SupplyReportController(SupplyReportService supplyReportService) {
    this.supplyReportService = supplyReportService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SupplyReportResponse create(
      @Valid @RequestBody CreateSupplyReportRequest request, Principal principal) {
    return supplyReportService.create(request, principal.getName());
  }

  @GetMapping("/mine")
  public List<SupplyReportResponse> listMine(Principal principal) {
    return supplyReportService.listMine(principal.getName());
  }

  @GetMapping("/{id}")
  public SupplyReportResponse getOne(@PathVariable Long id, Principal principal) {
    return supplyReportService.getOne(id, principal.getName());
  }

  @PatchMapping("/{id}")
  public SupplyReportResponse updateOwn(
      @PathVariable Long id,
      @Valid @RequestBody UpdateSupplyReportRequest request,
      Principal principal) {
    return supplyReportService.updateOwn(id, request, principal.getName());
  }
}
