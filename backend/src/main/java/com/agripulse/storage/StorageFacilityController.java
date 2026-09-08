package com.agripulse.storage;

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

/** Storage facilities. Operators manage only their own records. */
@RestController
@RequestMapping("/api/storage")
@PreAuthorize("hasRole('STORAGE_OPERATOR')")
public class StorageFacilityController {

  private final StorageFacilityService storageService;

  public StorageFacilityController(StorageFacilityService storageService) {
    this.storageService = storageService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public StorageResponse create(
      @Valid @RequestBody CreateStorageRequest request, Principal principal) {
    return storageService.create(request, principal.getName());
  }

  @GetMapping("/mine")
  public List<StorageResponse> listMine(Principal principal) {
    return storageService.listMine(principal.getName());
  }

  @PatchMapping("/{id}/occupancy")
  public StorageResponse updateOccupancy(
      @PathVariable Long id,
      @Valid @RequestBody UpdateOccupancyRequest request,
      Principal principal) {
    return storageService.updateOccupancy(id, request, principal.getName());
  }
}
