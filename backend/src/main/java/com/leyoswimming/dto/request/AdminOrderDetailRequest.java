package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminOrderDetailRequest(@NotNull(message = "订单 ID 不能为空") Long orderId) {}
