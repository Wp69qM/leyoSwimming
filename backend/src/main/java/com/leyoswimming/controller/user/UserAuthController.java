package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.RefreshTokenRequest;
import com.leyoswimming.dto.request.UserPhoneLoginRequest;
import com.leyoswimming.dto.request.UserWechatLoginRequest;
import com.leyoswimming.dto.response.RefreshTokenResponse;
import com.leyoswimming.dto.response.UserLoginResponse;
import com.leyoswimming.service.UserAuthService;
import com.leyoswimming.service.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

  private final UserAuthService userAuthService;
  private final UserSessionService userSessionService;

  @PostMapping("/wechat-login")
  public ApiResponse<UserLoginResponse> wechatLogin(
      @Valid @RequestBody UserWechatLoginRequest request, HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        userAuthService.wechatLogin(
            request, extractIp(httpRequest), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/phone-login")
  public ApiResponse<UserLoginResponse> phoneLogin(
      @Valid @RequestBody UserPhoneLoginRequest request, HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        userAuthService.phoneLogin(
            request, extractIp(httpRequest), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/refresh")
  public ApiResponse<RefreshTokenResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.ok(userSessionService.refreshAccessToken(request.refreshToken()));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody RefreshTokenRequest request) {
    userSessionService.logout(userId, request.refreshToken());
    return ApiResponse.ok(null);
  }

  private String extractIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
  }
}
