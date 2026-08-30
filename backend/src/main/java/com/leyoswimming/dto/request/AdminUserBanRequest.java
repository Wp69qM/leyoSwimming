package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserBanRequest(
    @NotNull(message = "用户 ID 不能为空") Long userId,
    @NotBlank(message = "原因不能为空")
        @Size(max = 512, message = "原因不能超过 512 个字符")
        String reason) {}
