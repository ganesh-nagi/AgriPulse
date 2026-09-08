package com.agripulse.storage;

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

/** Storage operators manage only their own facilities. */
@Service
public class StorageFacilityService {

  private final StorageFacilityRepository facilities;
  private final UserRepository users;
  private final AuditService auditService;

  public StorageFacilityService(
      StorageFacilityRepository facilities, UserRepository users, AuditService auditService) {
    this.facilities = facilities;
    this.users = users;
    this.auditService = auditService;
  }

  @Transactional
  public StorageResponse create(CreateStorageRequest request, String email) {
    User operator = operatorOf(email);
    StorageFacility facility = new StorageFacility();
    facility.setName(request.getName().trim());
    facility.setRegion(request.getRegion().trim());
    facility.setCapacityTonnes(request.getCapacityTonnes());
    facility.setOccupiedTonnes(0.0);
    facility.setCropCompatibility(request.getCropCompatibility());
    facility.setCostPerDay(request.getCostPerDay());
    facility.setAvailableFrom(request.getAvailableFrom());
    facility.setAvailableTo(request.getAvailableTo());
    facility.setOperator(operator);
    facilities.save(facility);
    auditService.record(
        operator.getId(),
        "STORAGE_CREATED",
        "StorageFacility",
        String.valueOf(facility.getId()),
        "region=" + facility.getRegion());
    return toResponse(facility);
  }

  @Transactional(readOnly = true)
  public List<StorageResponse> listMine(String email) {
    User operator = operatorOf(email);
    return facilities.findByOperatorId(operator.getId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public StorageResponse updateOccupancy(
      Long facilityId, UpdateOccupancyRequest request, String email) {
    User operator = operatorOf(email);
    StorageFacility facility = ownedFacility(facilityId, operator);
    if (request.getOccupiedTonnes() > facility.getCapacityTonnes()) {
      throw new IllegalArgumentException("Occupied capacity cannot exceed total capacity");
    }
    facility.setOccupiedTonnes(request.getOccupiedTonnes());
    auditService.record(
        operator.getId(),
        "STORAGE_UPDATED",
        "StorageFacility",
        String.valueOf(facility.getId()),
        "occupied=" + request.getOccupiedTonnes());
    return toResponse(facility);
  }

  private StorageFacility ownedFacility(Long facilityId, User operator) {
    StorageFacility facility =
        facilities
            .findById(facilityId)
            .orElseThrow(() -> new IllegalArgumentException("Storage facility not found"));
    if (facility.getOperator() == null
        || !facility.getOperator().getId().equals(operator.getId())) {
      throw new AccessDeniedException("Storage facility does not belong to the operator");
    }
    return facility;
  }

  private User operatorOf(String email) {
    User user =
        users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (user.getRole() != Role.STORAGE_OPERATOR) {
      throw new AccessDeniedException("Storage operator role required");
    }
    if (user.getAccountStatus() != AccountStatus.ACTIVE) {
      throw new AccountLockedException("Account is not active");
    }
    return user;
  }

  private StorageResponse toResponse(StorageFacility facility) {
    return new StorageResponse(
        facility.getId(),
        facility.getName(),
        facility.getRegion(),
        facility.getCapacityTonnes(),
        facility.getOccupiedTonnes(),
        facility.getAvailableCapacityTonnes());
  }
}
