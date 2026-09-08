package com.agripulse.demand;

import com.agripulse.audit.AuditService;
import com.agripulse.auth.AccountLockedException;
import com.agripulse.buyer.BuyerProfile;
import com.agripulse.buyer.BuyerProfileRepository;
import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Buyer demand signals. Buyers only ever see their own requirements. */
@Service
public class BuyerRequirementService {

  private final BuyerRequirementRepository requirements;
  private final BuyerProfileRepository buyerProfiles;
  private final CropRepository crops;
  private final UserRepository users;
  private final AuditService auditService;

  public BuyerRequirementService(
      BuyerRequirementRepository requirements,
      BuyerProfileRepository buyerProfiles,
      CropRepository crops,
      UserRepository users,
      AuditService auditService) {
    this.requirements = requirements;
    this.buyerProfiles = buyerProfiles;
    this.crops = crops;
    this.users = users;
    this.auditService = auditService;
  }

  @Transactional
  public BuyerRequirementResponse create(CreateBuyerRequirementRequest request, String email) {
    User buyer = buyerOf(email);
    BuyerProfile profile = profileOf(buyer, request.getRegion().trim());
    Crop crop =
        crops.findById(request.getCropId())
            .orElseThrow(() -> new IllegalArgumentException("Crop not found"));
    BuyerRequirement requirement = new BuyerRequirement();
    requirement.setBuyer(profile);
    requirement.setCrop(crop);
    requirement.setQuantityTonnes(request.getQuantityTonnes());
    requirement.setQuality(request.getQuality());
    requirement.setRequiredDate(request.getRequiredDate());
    requirement.setRegion(request.getRegion().trim());
    requirement.setStatus(RequirementStatus.OPEN);
    requirement.setDataSource(DataSource.REAL);
    requirements.save(requirement);
    auditService.record(
        buyer.getId(),
        "BUYER_REQUIREMENT_CREATED",
        "BuyerRequirement",
        String.valueOf(requirement.getId()),
        "region=" + requirement.getRegion());
    return toResponse(requirement);
  }

  @Transactional(readOnly = true)
  public List<BuyerRequirementResponse> listMine(String email) {
    User buyer = buyerOf(email);
    return buyerProfiles
        .findByUserId(buyer.getId())
        .map(p -> requirements.findByBuyerId(p.getId()).stream().map(this::toResponse).toList())
        .orElse(List.of());
  }

  private User buyerOf(String email) {
    User user =
        users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (user.getRole() != Role.BUYER) {
      throw new AccessDeniedException("Buyer role required");
    }
    if (user.getAccountStatus() != AccountStatus.ACTIVE) {
      throw new AccountLockedException("Account is not active");
    }
    return user;
  }

  private BuyerProfile profileOf(User user, String region) {
    return buyerProfiles
        .findByUserId(user.getId())
        .orElseGet(
            () -> {
              BuyerProfile profile = new BuyerProfile();
              profile.setUser(user);
              profile.setOrgName(user.getEmail().split("@")[0]);
              profile.setRegion(region);
              return buyerProfiles.save(profile);
            });
  }

  private BuyerRequirementResponse toResponse(BuyerRequirement requirement) {
    return new BuyerRequirementResponse(
        requirement.getId(),
        requirement.getCrop().getName(),
        requirement.getQuantityTonnes(),
        requirement.getQuality(),
        requirement.getRequiredDate(),
        requirement.getRegion(),
        requirement.getStatus(),
        requirement.getDataSource());
  }
}
