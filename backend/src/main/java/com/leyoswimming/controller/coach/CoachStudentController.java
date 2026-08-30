package com.leyoswimming.controller.coach;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.CoachStudentDetailRequest;
import com.leyoswimming.dto.request.CoachStudentListRequest;
import com.leyoswimming.dto.request.CoachStudentPackageListRequest;
import com.leyoswimming.dto.request.CoachStudentUpdateRequest;
import com.leyoswimming.dto.response.CoachStudentDetailResponse;
import com.leyoswimming.dto.response.CoachStudentListResponse;
import com.leyoswimming.dto.response.CoachStudentPackageListResponse;
import com.leyoswimming.service.CoachStudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach/student")
@RequiredArgsConstructor
public class CoachStudentController {

  private final CoachStudentService coachStudentService;

  @PostMapping("/list")
  public ApiResponse<CoachStudentListResponse> list(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachStudentListRequest request) {
    return ApiResponse.ok(coachStudentService.list(coachId, request));
  }

  @PostMapping("/detail")
  public ApiResponse<CoachStudentDetailResponse> detail(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachStudentDetailRequest request) {
    return ApiResponse.ok(coachStudentService.detail(coachId, request));
  }

  @PostMapping("/update")
  public ApiResponse<Void> update(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachStudentUpdateRequest request) {
    coachStudentService.update(coachId, request);
    return ApiResponse.ok(null);
  }

  @PostMapping("/package-list")
  public ApiResponse<CoachStudentPackageListResponse> packageList(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachStudentPackageListRequest request) {
    return ApiResponse.ok(coachStudentService.packageList(coachId, request));
  }
}
