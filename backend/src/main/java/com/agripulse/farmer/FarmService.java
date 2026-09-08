package com.agripulse.farmer;

import com.agripulse.audit.AuditService;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Farmers manage only their own farms. */
@Service
public class FarmService {

  private final FarmRepository farms;
  private final FarmerProfileRepository profiles;
  private final UserRepository users;
  private final AuditService auditService;

  public FarmService(
      FarmRepository farms,
      FarmerProfileRepository profiles,
      UserRepository users,
      AuditService auditService) {
    this.farms = farms;
    this.profiles = profiles;
    this.users = users;
    this.auditService = auditService;
  }

  @Transactional
  public FarmResponse create(CreateFarmRequest request, String email) {
    FarmerProfile profile = profileOf(email);
    Farm farm = new Farm();
    farm.setFarmer(profile);
    farm.setName(request.getName().trim());
    farm.setRegion(request.getRegion().trim());
    farm.setLatitude(request.getLatitude());
    farm.setLongitude(request.getLongitude());
    farm.setAreaAcres(request.getAreaAcres());
    farms.save(farm);
    auditService.record(
        profile.getUser().getId(),
        "FARM_CREATED",
        "Farm",
        String.valueOf(farm.getId()),
        "region=" + farm.getRegion());
    return toResponse(farm);
  }

  @Transactional(readOnly = true)
  public List<FarmResponse> listMine(String email) {
    FarmerProfile profile = profileOf(email);
    return farms.findByFarmerId(profile.getId()).stream().map(this::toResponse).toList();
  }

  private FarmerProfile profileOf(String email) {
    User user =
        users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (user.getRole() != Role.FARMER) {
      throw new AccessDeniedException("Farmer role required");
    }
    return profiles
        .findByUserId(user.getId())
        .orElseThrow(() -> new IllegalArgumentException("Farmer profile not created yet"));
  }

  private FarmResponse toResponse(Farm farm) {
    return new FarmResponse(farm.getId(), farm.getName(), farm.getRegion(), farm.getAreaAcres());
  }
}
