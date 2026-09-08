package com.agripulse.user;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin operations. Every action is audit-logged in the service layer. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/overview")
  public AdminOverviewResponse overview() {
    return adminService.overview();
  }

  @PutMapping("/users/{id}/role")
  public void changeRole(
      @PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request, Principal principal) {
    adminService.changeRole(id, request.getRole(), principal.getName());
  }

  @PutMapping("/users/{id}/status")
  public void changeStatus(
      @PathVariable Long id,
      @Valid @RequestBody ChangeStatusRequest request,
      Principal principal) {
    adminService.changeStatus(id, request.getStatus(), principal.getName());
  }
}
