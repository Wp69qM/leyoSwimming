package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.UserPackageRefundRequest;
import com.leyoswimming.dto.response.UserPackageRefundResponse;
import com.leyoswimming.service.PackageService;
import jakarta.validation.Valid;
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
}
