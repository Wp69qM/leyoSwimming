package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.AdminOrderDetailRequest;
import com.leyoswimming.dto.request.AdminOrderListRequest;
import com.leyoswimming.dto.request.AdminOrderRefundApproveRequest;
import com.leyoswimming.dto.request.AdminOrderRefundRejectRequest;
import com.leyoswimming.dto.response.AdminOrderDetailResponse;
import com.leyoswimming.dto.response.AdminOrderListResponse;
import com.leyoswimming.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

  private final OrderService orderService;

  @PostMapping("/list")
  public ApiResponse<AdminOrderListResponse> list(
      @AuthenticationPrincipal Long adminId, @RequestBody @Valid AdminOrderListRequest request) {
    return ApiResponse.ok(orderService.list(adminId, request));
  }

  @PostMapping("/detail")
  public ApiResponse<AdminOrderDetailResponse> detail(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminOrderDetailRequest request) {
    return ApiResponse.ok(orderService.detail(adminId, request.orderId()));
  }

  @PostMapping("/refundApprove")
  public ApiResponse<Void> refundApprove(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminOrderRefundApproveRequest request) {
    orderService.approveRefund(adminId, request);
    return ApiResponse.ok();
  }

  @PostMapping("/refundReject")
  public ApiResponse<Void> refundReject(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid AdminOrderRefundRejectRequest request) {
    orderService.rejectRefund(adminId, request);
    return ApiResponse.ok();
  }
}
