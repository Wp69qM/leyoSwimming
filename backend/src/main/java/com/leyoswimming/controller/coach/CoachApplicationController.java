package com.leyoswimming.controller.coach;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.CoachApplicationSaveDraftRequest;
import com.leyoswimming.dto.request.CoachApplicationSubmitRequest;
import com.leyoswimming.dto.response.CoachApplicationResponse;
import com.leyoswimming.dto.response.CoachApplicationSubmitResponse;
import com.leyoswimming.service.CoachOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach/application")
@RequiredArgsConstructor
public class CoachApplicationController {

  private final CoachOnboardingService coachOnboardingService;

  @PostMapping("/detail")
  public ApiResponse<CoachApplicationResponse> detail(@AuthenticationPrincipal Long coachId) {
    return ApiResponse.ok(coachOnboardingService.getDetail(coachId));
  }

  @PostMapping("/save-draft")
  public ApiResponse<CoachApplicationSubmitResponse> saveDraft(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachApplicationSaveDraftRequest request) {
    return ApiResponse.ok(coachOnboardingService.saveDraft(coachId, request));
  }

  @PostMapping("/submit")
  public ApiResponse<CoachApplicationSubmitResponse> submit(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachApplicationSubmitRequest request) {
    return ApiResponse.ok(coachOnboardingService.submit(coachId, request));
  }
}
