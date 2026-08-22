package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.UserPackageDetailRequest;
import com.leyoswimming.dto.request.UserPackageListRequest;
import com.leyoswimming.dto.request.UserPackageRefundRequest;
import com.leyoswimming.dto.response.UserActivePackageResponse;
import com.leyoswimming.dto.response.UserPackageDetailResponse;
import com.leyoswimming.dto.response.UserPackageListItemResponse;
import com.leyoswimming.dto.response.UserPackageQualificationResponse;
import com.leyoswimming.dto.response.UserPackageRefundResponse;
import com.leyoswimming.service.PackageService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/package")
@RequiredArgsConstructor
public class UserPackageController {

  private final PackageService packageService;

  @PostMapping("/refund")
  public ApiResponse<UserPackageRefundResponse> refund(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody UserPackageRefundRequest request) {
    return ApiResponse.ok(packageService.requestRefund(userId, request));
  }

  @PostMapping("/active")
  public ApiResponse<UserActivePackageResponse> active(
      @AuthenticationPrincipal Long userId) {
    return ApiResponse.ok(packageService.getActivePackage(userId));
  }

  @PostMapping("/qualification")
  public ApiResponse<UserPackageQualificationResponse> qualification(
      @AuthenticationPrincipal Long userId) {
    return ApiResponse.ok(packageService.getPackageQualification(userId));
  }

  @PostMapping("/list")
  public ApiResponse<List<UserPackageListItemResponse>> list(
      @AuthenticationPrincipal Long userId,
      @RequestBody UserPackageListRequest request) {
    return ApiResponse.ok(packageService.list(userId, request));
  }

  @PostMapping("/detail")
  public ApiResponse<UserPackageDetailResponse> detail(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody UserPackageDetailRequest request) {
    return ApiResponse.ok(packageService.detail(userId, request));
  }
}
