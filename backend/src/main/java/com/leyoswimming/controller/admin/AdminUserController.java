package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.AdminUserAddRequest;
import com.leyoswimming.dto.request.AdminUserBanRequest;
import com.leyoswimming.dto.request.AdminUserDetailRequest;
import com.leyoswimming.dto.request.AdminUserListRequest;
import com.leyoswimming.dto.request.AdminUserUpdateRequest;
import com.leyoswimming.dto.response.AdminUserDetailResponse;
import com.leyoswimming.dto.response.AdminUserListResponse;
import com.leyoswimming.service.AdminUserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

  private final AdminUserManagementService adminUserManagementService;

  @PostMapping("/list")
  public ApiResponse<AdminUserListResponse> list(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminUserListRequest request) {
    return ApiResponse.ok(adminUserManagementService.list(adminId, request));
  }

  @PostMapping("/detail")
  public ApiResponse<AdminUserDetailResponse> detail(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminUserDetailRequest request) {
    return ApiResponse.ok(adminUserManagementService.detail(adminId, request.userId()));
  }

  @PostMapping("/add")
  public ApiResponse<AdminUserDetailResponse> add(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminUserAddRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(adminUserManagementService.add(adminId, request, httpRequest));
  }

  @PostMapping("/update")
  public ApiResponse<AdminUserDetailResponse> update(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminUserUpdateRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(adminUserManagementService.update(adminId, request, httpRequest));
  }

  @PostMapping("/ban")
  public ApiResponse<AdminUserDetailResponse> ban(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminUserBanRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        adminUserManagementService.ban(adminId, request.userId(), request.reason(), httpRequest));
  }

  @PostMapping("/unban")
  public ApiResponse<AdminUserDetailResponse> unban(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminUserBanRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        adminUserManagementService.unban(adminId, request.userId(), request.reason(), httpRequest));
  }
}
