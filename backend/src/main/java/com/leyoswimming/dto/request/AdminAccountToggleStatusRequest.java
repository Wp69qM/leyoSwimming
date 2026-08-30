package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAccountToggleStatusRequest(
    @NotNull(message = "管理员 ID 不能为空") Long adminId,
    @NotNull(message = "状态不能为空") @Min(value = 0, message = "状态只能是 0 或 1")
        @Max(value = 1, message = "状态只能是 0 或 1")
        Integer status,
    @NotBlank(message = "操作原因不能为空")
        @Size(min = 2, max = 200, message = "操作原因需在 2-200 个字符之间")
        String reason) {}
