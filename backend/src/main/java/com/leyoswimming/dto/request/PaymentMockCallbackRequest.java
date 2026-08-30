package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentMockCallbackRequest(
    @NotNull(message = "支付渠道不能为空") Integer channel,
    @NotNull(message = "订单 ID 不能为空") Long orderId,
    @NotNull(message = "渠道流水号不能为空") String channelTradeNo,
    @NotNull(message = "金额不能为空") BigDecimal amount,
    @NotNull(message = "支付结果不能为空") Boolean success) {}
