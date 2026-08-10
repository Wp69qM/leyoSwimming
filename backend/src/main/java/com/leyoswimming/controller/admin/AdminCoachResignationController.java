package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.AdminResignationApproveRequest;
import com.leyoswimming.dto.request.AdminResignationRejectRequest;
import com.leyoswimming.dto.request.AdminResignationTicketDetailRequest;
import com.leyoswimming.dto.request.AdminResignationTicketListRequest;
import com.leyoswimming.dto.response.AdminResignationTicketDetailResponse;
import com.leyoswimming.dto.response.AdminResignationTicketListResponse;
import com.leyoswimming.service.AdminCoachResignationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coach/resignation-ticket")
@RequiredArgsConstructor
public class AdminCoachResignationController {

  private final AdminCoachResignationService adminCoachResignationService;

  @PostMapping("/list")
  public ApiResponse<AdminResignationTicketListResponse> list(
      @Valid @RequestBody AdminResignationTicketListRequest request) {
    return ApiResponse.ok(
        adminCoachResignationService.list(
            request.status(),
            request.page(),
            request.pageSize(),
            request.keyword(),
            request.submitStartDate(),
            request.submitEndDate()));
  }

  @PostMapping("/detail")
  public ApiResponse<AdminResignationTicketDetailResponse> detail(
      @Valid @RequestBody AdminResignationTicketDetailRequest request) {
    return ApiResponse.ok(adminCoachResignationService.detail(request.ticketId()));
  }

  @PostMapping("/approve")
  public ApiResponse<Void> approve(
      @AuthenticationPrincipal Long adminId,
      @Valid @RequestBody AdminResignationApproveRequest request) {
    adminCoachResignationService.approve(adminId, request.ticketId(), request.comment());
    return ApiResponse.ok(null);
  }

  @PostMapping("/reject")
  public ApiResponse<Void> reject(
      @AuthenticationPrincipal Long adminId,
      @Valid @RequestBody AdminResignationRejectRequest request) {
    adminCoachResignationService.reject(adminId, request.ticketId(), request.reason());
    return ApiResponse.ok(null);
  }
}
