package com.leyoswimming.controller.coach;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.UpdateCoachProfileRequest;
import com.leyoswimming.dto.response.CoachProfileResponse;
import com.leyoswimming.service.CoachProfileService;
import com.leyoswimming.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/coach/profile")
@RequiredArgsConstructor
public class CoachProfileController {

  private final CoachProfileService coachProfileService;
  private final FileStorageService fileStorageService;

  @PostMapping("/detail")
  public ApiResponse<CoachProfileResponse> detail(@AuthenticationPrincipal Long coachId) {
    return ApiResponse.ok(coachProfileService.getProfile(coachId));
  }

  @PostMapping("/update")
  public ApiResponse<CoachProfileResponse> update(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody UpdateCoachProfileRequest request) {
    return ApiResponse.ok(coachProfileService.updateProfile(coachId, request));
  }

  @PostMapping("/upload-avatar")
  public ApiResponse<String> uploadAvatar(
      @AuthenticationPrincipal Long coachId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(fileStorageService.store(file));
  }
}
