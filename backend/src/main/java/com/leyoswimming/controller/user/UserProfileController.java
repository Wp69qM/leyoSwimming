package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.UpdateUserProfileRequest;
import com.leyoswimming.dto.response.UserProfileResponse;
import com.leyoswimming.service.FileStorageService;
import com.leyoswimming.service.UserProfileService;
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
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserProfileService userProfileService;
  private final FileStorageService fileStorageService;

  @PostMapping("/detail")
  public ApiResponse<UserProfileResponse> detail(@AuthenticationPrincipal Long userId) {
    return ApiResponse.ok(userProfileService.getProfile(userId));
  }

  @PostMapping("/update")
  public ApiResponse<UserProfileResponse> update(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody UpdateUserProfileRequest request) {
    return ApiResponse.ok(userProfileService.updateProfile(userId, request));
  }

  @PostMapping("/upload-avatar")
  public ApiResponse<String> uploadAvatar(
      @AuthenticationPrincipal Long userId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(fileStorageService.store(file));
  }
}
