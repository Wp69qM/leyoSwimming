package com.leyoswimming.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminOrderRefundApproveRequest(
    @NotNull(message = "订单 ID 不能为空") Long orderId,
    @NotNull(message = "退款金额不能为空")
        @DecimalMin(value = "0.00", inclusive = true, message = "退款金额不能小于 0")
        @DecimalMax(value = "99999999.99", message = "退款金额超出限制")
        BigDecimal refundAmount,
    @Size(max = 500, message = "调整原因长度不能超过 500 字符") String adjustReason) {}
