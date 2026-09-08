package com.agripulse.transport;

import com.agripulse.audit.AuditService;
import com.agripulse.auth.AccountLockedException;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transporters manage only their own capacity. */
@Service
public class TransportResourceService {

  private final TransportResourceRepository resources;
  private final UserRepository users;
  private final AuditService auditService;

  public TransportResourceService(
      TransportResourceRepository resources, UserRepository users, AuditService auditService) {
    this.resources = resources;
    this.users = users;
    this.auditService = auditService;
  }

  @Transactional
  public TransportResponse create(CreateTransportRequest request, String email) {
    User owner = transporterOf(email);
    if (request.getCapacityTonnes() < 0) {
      throw new IllegalArgumentException("Transport capacity cannot be negative");
    }
    if (request.getAvailableFrom() != null
        && request.getAvailableTo() != null
        && request.getAvailableFrom().isAfter(request.getAvailableTo())) {
      throw new IllegalArgumentException("availableFrom must be on or before availableTo");
    }
    TransportResource resource = new TransportResource();
    resource.setOwner(owner);
    resource.setCapacityTonnes(request.getCapacityTonnes());
    resource.setAvailableFrom(request.getAvailableFrom());
    resource.setAvailableTo(request.getAvailableTo());
    resource.setOriginRegion(request.getOriginRegion().trim());
    resource.setDestRegion(request.getDestRegion());
    resource.setCostPerKm(request.getCostPerKm());
    resource.setTravelEstimateDays(request.getTravelEstimateDays());
    resource.setStatus(TransportStatus.AVAILABLE);
    resources.save(resource);
    auditService.record(
        owner.getId(),
        "TRANSPORT_CREATED",
        "TransportResource",
        String.valueOf(resource.getId()),
        "origin=" + resource.getOriginRegion());
    return toResponse(resource);
  }

  @Transactional(readOnly = true)
  public List<TransportResponse> listMine(String email) {
    User owner = transporterOf(email);
    return resources.findByOwnerId(owner.getId()).stream().map(this::toResponse).toList();
  }

  @Transactional
  public TransportResponse updateStatus(
      Long resourceId, UpdateTransportStatusRequest request, String email) {
    User owner = transporterOf(email);
    TransportResource resource =
        resources
            .findById(resourceId)
            .orElseThrow(() -> new IllegalArgumentException("Transport resource not found"));
    if (resource.getOwner() == null || !resource.getOwner().getId().equals(owner.getId())) {
      throw new AccessDeniedException("Transport resource does not belong to the transporter");
    }
    resource.setStatus(request.getStatus());
    auditService.record(
        owner.getId(),
        "TRANSPORT_UPDATED",
        "TransportResource",
        String.valueOf(resource.getId()),
        "status=" + request.getStatus().name());
    return toResponse(resource);
  }

  private User transporterOf(String email) {
    User user =
        users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (user.getRole() != Role.TRANSPORTER) {
      throw new AccessDeniedException("Transporter role required");
    }
    if (user.getAccountStatus() != AccountStatus.ACTIVE) {
      throw new AccountLockedException("Account is not active");
    }
    return user;
  }

  private TransportResponse toResponse(TransportResource resource) {
    return new TransportResponse(
        resource.getId(),
        resource.getCapacityTonnes(),
        resource.getOriginRegion(),
        resource.getDestRegion(),
        resource.getStatus());
  }
}
