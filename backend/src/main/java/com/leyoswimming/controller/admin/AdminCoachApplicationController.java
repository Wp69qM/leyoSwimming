package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.AdminCoachApplicationApproveRequest;
import com.leyoswimming.dto.request.AdminCoachApplicationDetailRequest;
import com.leyoswimming.dto.request.AdminCoachApplicationListRequest;
import com.leyoswimming.dto.request.AdminCoachApplicationRejectRequest;
import com.leyoswimming.dto.response.AdminCoachApplicationDetailResponse;
import com.leyoswimming.dto.response.AdminCoachApplicationListResponse;
import com.leyoswimming.service.CoachAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coach/application")
@RequiredArgsConstructor
public class AdminCoachApplicationController {

  private final CoachAuditService coachAuditService;

  @PostMapping("/list")
  public ApiResponse<AdminCoachApplicationListResponse> list(
      @Valid @RequestBody AdminCoachApplicationListRequest request) {
    return ApiResponse.ok(coachAuditService.list(request));
  }

  @PostMapping("/detail")
  public ApiResponse<AdminCoachApplicationDetailResponse> detail(
      @Valid @RequestBody AdminCoachApplicationDetailRequest request) {
    return ApiResponse.ok(coachAuditService.detail(request.applicationId()));
  }

  @PostMapping("/approve")
  public ApiResponse<Void> approve(
      @AuthenticationPrincipal Long adminId,
      @Valid @RequestBody AdminCoachApplicationApproveRequest request) {
    coachAuditService.approve(adminId, request);
    return ApiResponse.ok(null);
  }

  @PostMapping("/reject")
  public ApiResponse<Void> reject(
      @AuthenticationPrincipal Long adminId,
      @Valid @RequestBody AdminCoachApplicationRejectRequest request) {
    coachAuditService.reject(adminId, request);
    return ApiResponse.ok(null);
  }
}
