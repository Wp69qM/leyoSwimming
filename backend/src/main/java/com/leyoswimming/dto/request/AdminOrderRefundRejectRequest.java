package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminOrderRefundRejectRequest(
    @NotNull(message = "订单 ID 不能为空") Long orderId,
    @NotBlank(message = "驳回原因不能为空")
        @Size(max = 500, message = "驳回原因长度不能超过 500 字符")
        String rejectedReason) {}
