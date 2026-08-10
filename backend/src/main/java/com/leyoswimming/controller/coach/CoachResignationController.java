package com.leyoswimming.controller.coach;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.CoachResignationApplyRequest;
import com.leyoswimming.dto.request.CoachResignationPackageActionRequest;
import com.leyoswimming.dto.request.CoachResignationSubmitRequest;
import com.leyoswimming.dto.response.CoachResignationApplyResponse;
import com.leyoswimming.dto.response.CoachResignationDetailResponse;
import com.leyoswimming.dto.response.CoachResignationPackageActionResponse;
import com.leyoswimming.service.CoachResignationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach/resignation")
@RequiredArgsConstructor
public class CoachResignationController {

  private final CoachResignationService coachResignationService;

  @PostMapping("/apply")
  public ApiResponse<CoachResignationApplyResponse> apply(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachResignationApplyRequest request) {
    return ApiResponse.ok(
        coachResignationService.apply(
            coachId, request.reason(), request.idempotencyKey()));
  }

  @PostMapping("/detail")
  public ApiResponse<CoachResignationDetailResponse> detail(
      @AuthenticationPrincipal Long coachId) {
    return ApiResponse.ok(coachResignationService.detail(coachId));
  }

  @PostMapping("/package/action")
  public ApiResponse<CoachResignationPackageActionResponse> registerAction(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachResignationPackageActionRequest request) {
    return ApiResponse.ok(
        coachResignationService.registerAction(
            coachId,
            request.ticketId(),
            request.packageId(),
            request.action(),
            request.targetCoachId()));
  }

  @PostMapping("/submit")
  public ApiResponse<Void> submit(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachResignationSubmitRequest request) {
    coachResignationService.submit(coachId, request.ticketId());
    return ApiResponse.ok(null);
  }
}
