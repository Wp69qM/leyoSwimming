package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.AdminPackageTemplateAddRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateCustomConfigRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateDetailRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateListRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateToggleRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateUpdateRequest;
import com.leyoswimming.dto.response.AdminPackageTemplateCustomConfigResponse;
import com.leyoswimming.dto.response.AdminPackageTemplateDetailResponse;
import com.leyoswimming.dto.response.AdminPackageTemplateListResponse;
import com.leyoswimming.service.AdminPackageTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/package-template")
@RequiredArgsConstructor
public class AdminPackageTemplateController {

  private final AdminPackageTemplateService adminPackageTemplateService;

  @PostMapping("/list")
  public ApiResponse<AdminPackageTemplateListResponse> list(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminPackageTemplateListRequest request) {
    return ApiResponse.ok(adminPackageTemplateService.list(adminId, request));
  }

  @PostMapping("/detail")
  public ApiResponse<AdminPackageTemplateDetailResponse> detail(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminPackageTemplateDetailRequest request) {
    return ApiResponse.ok(adminPackageTemplateService.detail(adminId, request.packageTemplateId()));
  }

  @PostMapping("/add")
  public ApiResponse<AdminPackageTemplateDetailResponse> add(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminPackageTemplateAddRequest request) {
    return ApiResponse.ok(adminPackageTemplateService.add(adminId, request));
  }

  @PostMapping("/update")
  public ApiResponse<AdminPackageTemplateDetailResponse> update(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminPackageTemplateUpdateRequest request) {
    return ApiResponse.ok(adminPackageTemplateService.update(adminId, request));
  }

  @PostMapping("/toggle-status")
  public ApiResponse<AdminPackageTemplateDetailResponse> toggleStatus(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminPackageTemplateToggleRequest request) {
    return ApiResponse.ok(adminPackageTemplateService.toggleStatus(adminId, request));
  }

  @PostMapping("/custom-config/detail")
  public ApiResponse<AdminPackageTemplateCustomConfigResponse> customConfigDetail(
      @AuthenticationPrincipal Long adminId) {
    return ApiResponse.ok(adminPackageTemplateService.customConfigDetail(adminId));
  }

  @PostMapping("/custom-config")
  public ApiResponse<AdminPackageTemplateCustomConfigResponse> customConfig(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminPackageTemplateCustomConfigRequest request) {
    return ApiResponse.ok(adminPackageTemplateService.customConfig(adminId, request));
  }
}
