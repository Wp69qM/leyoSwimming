package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.CoachDetailRequest;
import com.leyoswimming.dto.request.CoachListRequest;
import com.leyoswimming.dto.response.CoachDetailResponse;
import com.leyoswimming.dto.response.CoachListResponse;
import com.leyoswimming.service.UserCoachService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
public class UserCoachController {

  private final UserCoachService userCoachService;

  @PostMapping("/list")
  public ApiResponse<CoachListResponse> list(@Valid @RequestBody CoachListRequest request) {
    return ApiResponse.ok(userCoachService.list(request));
  }

  @PostMapping("/detail")
  public ApiResponse<CoachDetailResponse> detail(
      @Valid @RequestBody CoachDetailRequest request) {
    return ApiResponse.ok(userCoachService.detail(request));
  }
}
