package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminPackageRefundRequest(
    @NotNull(message = "套餐 ID 不能为空")
    Long packageId,
    @NotBlank(message = "退款原因不能为空")
    String reason,
    BigDecimal refundAmount,
    String adjustReason,
    @NotNull(message = "版本号不能为空")
    Integer version) {}
