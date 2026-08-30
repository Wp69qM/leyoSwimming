package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.AdminAccountAddRequest;
import com.leyoswimming.dto.request.AdminAccountDeleteRequest;
import com.leyoswimming.dto.request.AdminAccountDetailRequest;
import com.leyoswimming.dto.request.AdminAccountListRequest;
import com.leyoswimming.dto.request.AdminAccountResetPasswordRequest;
import com.leyoswimming.dto.request.AdminAccountToggleStatusRequest;
import com.leyoswimming.dto.request.AdminAccountUpdateRequest;
import com.leyoswimming.dto.response.AdminAccountDetailResponse;
import com.leyoswimming.dto.response.AdminAccountListResponse;
import com.leyoswimming.dto.response.AdminAccountResetPasswordResponse;
import com.leyoswimming.service.AdminAccountManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/admin")
@RequiredArgsConstructor
public class AdminAccountController {

  private final AdminAccountManagementService adminAccountManagementService;

  @PostMapping("/list")
  public ApiResponse<AdminAccountListResponse> list(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminAccountListRequest request) {
    return ApiResponse.ok(adminAccountManagementService.list(adminId, request));
  }

  @PostMapping("/detail")
  public ApiResponse<AdminAccountDetailResponse> detail(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminAccountDetailRequest request) {
    return ApiResponse.ok(adminAccountManagementService.detail(adminId, request.adminId()));
  }

  @PostMapping("/add")
  public ApiResponse<AdminAccountDetailResponse> add(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminAccountAddRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(adminAccountManagementService.add(adminId, request, httpRequest));
  }

  @PostMapping("/update")
  public ApiResponse<AdminAccountDetailResponse> update(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminAccountUpdateRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(adminAccountManagementService.update(adminId, request, httpRequest));
  }

  @PostMapping("/toggle-status")
  public ApiResponse<AdminAccountDetailResponse> toggleStatus(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminAccountToggleStatusRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        adminAccountManagementService.toggleStatus(adminId, request, httpRequest));
  }

  @PostMapping("/delete")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminAccountDeleteRequest request,
      HttpServletRequest httpRequest) {
    adminAccountManagementService.delete(adminId, request, httpRequest);
    return ApiResponse.ok();
  }

  @PostMapping("/reset-password")
  public ApiResponse<AdminAccountResetPasswordResponse> resetPassword(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminAccountResetPasswordRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        adminAccountManagementService.resetPassword(adminId, request, httpRequest));
  }
}
