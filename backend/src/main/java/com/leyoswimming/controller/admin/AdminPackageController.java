package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.AdminPackageDetailRequest;
import com.leyoswimming.dto.request.AdminPackageExtendRequest;
import com.leyoswimming.dto.request.AdminPackageFreezeRequest;
import com.leyoswimming.dto.request.AdminPackageListRequest;
import com.leyoswimming.dto.request.AdminPackageRefundRequest;
import com.leyoswimming.dto.request.AdminPackageUnfreezeRequest;
import com.leyoswimming.dto.response.AdminPackageDetailResponse;
import com.leyoswimming.dto.response.AdminPackageListResponse;
import com.leyoswimming.dto.response.AdminPackageOperationResponse;
import com.leyoswimming.service.AdminPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/package")
@RequiredArgsConstructor
public class AdminPackageController {

  private final AdminPackageService adminPackageService;

  @PostMapping("/list")
  public ApiResponse<AdminPackageListResponse> list(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminPackageListRequest request) {
    return ApiResponse.ok(adminPackageService.list(adminId, request));
  }

  @PostMapping("/detail")
  public ApiResponse<AdminPackageDetailResponse> detail(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminPackageDetailRequest request) {
    return ApiResponse.ok(adminPackageService.detail(adminId, request.packageId()));
  }

  @PostMapping("/freeze")
  public ApiResponse<AdminPackageOperationResponse> freeze(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminPackageFreezeRequest request) {
    return ApiResponse.ok(adminPackageService.freeze(adminId, request));
  }

  @PostMapping("/unfreeze")
  public ApiResponse<AdminPackageOperationResponse> unfreeze(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminPackageUnfreezeRequest request) {
    return ApiResponse.ok(adminPackageService.unfreeze(adminId, request));
  }

  @PostMapping("/extend")
  public ApiResponse<AdminPackageOperationResponse> extend(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminPackageExtendRequest request) {
    return ApiResponse.ok(adminPackageService.extend(adminId, request));
  }

  @PostMapping("/refund")
  public ApiResponse<AdminPackageOperationResponse> refund(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminPackageRefundRequest request) {
    return ApiResponse.ok(adminPackageService.requestRefund(adminId, request));
  }
}
