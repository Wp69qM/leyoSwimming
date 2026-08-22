package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.OrderDetailRequest;
import com.leyoswimming.dto.request.OrderFormalRequest;
import com.leyoswimming.dto.request.OrderListRequest;
import com.leyoswimming.dto.request.OrderTrialRequest;
import com.leyoswimming.dto.response.OrderCreateResponse;
import com.leyoswimming.dto.response.OrderDetailResponse;
import com.leyoswimming.dto.response.OrderListResponse;
import com.leyoswimming.service.UserOrderService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class UserOrderController {

  private final UserOrderService userOrderService;

  @PostMapping("/trial")
  public ApiResponse<OrderCreateResponse> trial(
      @AuthenticationPrincipal Long userId,
      @RequestBody @Valid OrderTrialRequest request) {
    return ApiResponse.ok(userOrderService.createTrialOrder(userId, request));
  }

  @PostMapping("/formal")
  public ApiResponse<OrderCreateResponse> formal(
      @AuthenticationPrincipal Long userId,
      @RequestBody @Valid OrderFormalRequest request) {
    return ApiResponse.ok(userOrderService.createFormalOrder(userId, request));
  }

  @PostMapping("/detail")
  public ApiResponse<OrderDetailResponse> detail(
      @AuthenticationPrincipal Long userId,
      @RequestBody @Valid OrderDetailRequest request) {
    return ApiResponse.ok(userOrderService.detail(userId, request.orderId()));
  }

  @PostMapping("/list")
  public ApiResponse<Page<OrderListResponse>> list(
      @AuthenticationPrincipal Long userId,
      @RequestBody @Valid OrderListRequest request) {
    return ApiResponse.ok(userOrderService.list(userId, request));
  }
}
