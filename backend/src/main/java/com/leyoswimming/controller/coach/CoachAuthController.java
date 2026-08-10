package com.leyoswimming.controller.coach;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.CoachPhoneLoginRequest;
import com.leyoswimming.dto.request.CoachWechatLoginRequest;
import com.leyoswimming.dto.request.RefreshTokenRequest;
import com.leyoswimming.dto.response.CoachLoginResponse;
import com.leyoswimming.dto.response.RefreshTokenResponse;
import com.leyoswimming.service.CoachAuthService;
import com.leyoswimming.service.CoachSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach/auth")
@RequiredArgsConstructor
public class CoachAuthController {

  private final CoachAuthService coachAuthService;
  private final CoachSessionService coachSessionService;

  @PostMapping("/wechat-login")
  public ApiResponse<CoachLoginResponse> wechatLogin(
      @Valid @RequestBody CoachWechatLoginRequest request, HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        coachAuthService.wechatLogin(
            request, extractIp(httpRequest), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/phone-login")
  public ApiResponse<CoachLoginResponse> phoneLogin(
      @Valid @RequestBody CoachPhoneLoginRequest request, HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        coachAuthService.phoneLogin(
            request, extractIp(httpRequest), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/refresh")
  public ApiResponse<RefreshTokenResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.ok(coachSessionService.refreshAccessToken(request.refreshToken()));
  }

  private String extractIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
  }
}
