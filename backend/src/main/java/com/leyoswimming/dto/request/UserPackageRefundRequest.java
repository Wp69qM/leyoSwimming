package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserPackageRefundRequest(
    @NotNull(message = "套餐 ID 不能为空") Long packageId,
    @NotBlank(message = "退款原因不能为空")
        @Size(max = 500, message = "退款原因长度不能超过 500 字符")
        String reason) {}
