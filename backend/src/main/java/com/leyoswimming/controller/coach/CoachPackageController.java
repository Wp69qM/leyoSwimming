package com.leyoswimming.controller.coach;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.CoachPackageDetailRequest;
import com.leyoswimming.dto.response.CoachPackageDetailResponse;
import com.leyoswimming.service.CoachStudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach/package")
@RequiredArgsConstructor
public class CoachPackageController {

  private final CoachStudentService coachStudentService;

  @PostMapping("/detail")
  public ApiResponse<CoachPackageDetailResponse> detail(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachPackageDetailRequest request) {
    return ApiResponse.ok(coachStudentService.packageDetail(coachId, request));
  }
}
