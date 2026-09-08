package com.agripulse.transport;

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

/** Transport capacity. Transporters manage only their own resources. */
@RestController
@RequestMapping("/api/transport")
@PreAuthorize("hasRole('TRANSPORTER')")
public class TransportResourceController {

  private final TransportResourceService transportService;

  public TransportResourceController(TransportResourceService transportService) {
    this.transportService = transportService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TransportResponse create(
      @Valid @RequestBody CreateTransportRequest request, Principal principal) {
    return transportService.create(request, principal.getName());
  }

  @GetMapping("/mine")
  public List<TransportResponse> listMine(Principal principal) {
    return transportService.listMine(principal.getName());
  }

  @PatchMapping("/{id}/status")
  public TransportResponse updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTransportStatusRequest request,
      Principal principal) {
    return transportService.updateStatus(id, request, principal.getName());
  }
}
