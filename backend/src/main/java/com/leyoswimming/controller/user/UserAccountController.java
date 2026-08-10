package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.response.UserCancelCheckResponse;
import com.leyoswimming.dto.response.UserCancelResponse;
import com.leyoswimming.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/account")
@RequiredArgsConstructor
public class UserAccountController {

  private final UserAccountService userAccountService;

  @PostMapping("/cancel-check")
  public ApiResponse<UserCancelCheckResponse> cancelCheck(
      @AuthenticationPrincipal Long userId) {
    return ApiResponse.ok(userAccountService.cancelCheck(userId));
  }

  @PostMapping("/cancel")
  public ApiResponse<UserCancelResponse> cancel(@AuthenticationPrincipal Long userId) {
    return ApiResponse.ok(userAccountService.cancel(userId));
  }
}
