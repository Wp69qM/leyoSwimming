package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.AdminCoachAddRequest;
import com.leyoswimming.dto.request.AdminCoachCancelEntryRequest;
import com.leyoswimming.dto.request.AdminCoachDetailRequest;
import com.leyoswimming.dto.request.AdminCoachListRequest;
import com.leyoswimming.dto.request.AdminCoachUpdateRequest;
import com.leyoswimming.dto.response.AdminCoachDetailResponse;
import com.leyoswimming.dto.response.AdminCoachListResponse;
import com.leyoswimming.service.AdminCoachManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coach")
@RequiredArgsConstructor
public class AdminCoachController {

  private final AdminCoachManagementService adminCoachManagementService;

  @PostMapping("/list")
  public ApiResponse<AdminCoachListResponse> list(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminCoachListRequest request) {
    return ApiResponse.ok(adminCoachManagementService.list(adminId, request));
  }

  @PostMapping("/detail")
  public ApiResponse<AdminCoachDetailResponse> detail(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminCoachDetailRequest request) {
    return ApiResponse.ok(adminCoachManagementService.detail(adminId, request.coachId()));
  }

  @PostMapping("/add")
  public ApiResponse<AdminCoachDetailResponse> add(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminCoachAddRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(adminCoachManagementService.add(adminId, request, httpRequest));
  }

  @PostMapping("/update")
  public ApiResponse<AdminCoachDetailResponse> update(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminCoachUpdateRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(adminCoachManagementService.update(adminId, request, httpRequest));
  }

  @PostMapping("/cancelEntry")
  public ApiResponse<AdminCoachDetailResponse> cancelEntry(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminCoachCancelEntryRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        adminCoachManagementService.cancelEntry(adminId, request, httpRequest));
  }
}
