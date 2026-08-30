package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.OrderPayRequest;
import com.leyoswimming.dto.request.PaymentMockCallbackRequest;
import com.leyoswimming.dto.response.OrderPayResponse;
import com.leyoswimming.dto.response.PaymentMockCallbackResponse;
import com.leyoswimming.service.UserPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserPaymentController {

  private final UserPaymentService userPaymentService;

  @PostMapping("/order/pay")
  public ApiResponse<OrderPayResponse> pay(
      @AuthenticationPrincipal Long userId,
      @RequestBody @Valid OrderPayRequest request) {
    return ApiResponse.ok(userPaymentService.pay(userId, request));
  }

  @PostMapping("/payment/mock-callback")
  public ApiResponse<PaymentMockCallbackResponse> mockCallback(
      @RequestBody @Valid PaymentMockCallbackRequest request) {
    return ApiResponse.ok(userPaymentService.mockCallback(request));
  }
}
