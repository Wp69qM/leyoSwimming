package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.AdminLoginRequest;
import com.leyoswimming.dto.response.AdminLoginResponse;
import com.leyoswimming.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

  private final AdminAuthService adminAuthService;

  @PostMapping("/login")
  public ApiResponse<AdminLoginResponse> login(
      @RequestBody @Valid AdminLoginRequest request, HttpServletRequest httpRequest) {
    return ApiResponse.ok(adminAuthService.login(request, httpRequest));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
    String token = extractBearerToken(authorization);
    adminAuthService.logout(token);
    return ApiResponse.ok();
  }

  private String extractBearerToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    return authorization.substring(7);
  }
}
