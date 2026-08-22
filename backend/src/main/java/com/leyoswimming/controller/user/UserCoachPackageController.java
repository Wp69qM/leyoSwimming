package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.CoachPackageListRequest;
import com.leyoswimming.dto.response.CoachPackageListResponse;
import com.leyoswimming.service.UserPackageTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach/package")
@RequiredArgsConstructor
public class UserCoachPackageController {

  private final UserPackageTemplateService userPackageTemplateService;

  @PostMapping("/list")
  public ApiResponse<CoachPackageListResponse> list(
      @Valid @RequestBody CoachPackageListRequest request) {
    return ApiResponse.ok(userPackageTemplateService.coachPackages(request));
  }
}
