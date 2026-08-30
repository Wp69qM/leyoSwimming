package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record OrderPayRequest(
    @NotNull(message = "订单 ID 不能为空") Long orderId,
    @NotNull(message = "支付渠道不能为空") Integer channel) {}
