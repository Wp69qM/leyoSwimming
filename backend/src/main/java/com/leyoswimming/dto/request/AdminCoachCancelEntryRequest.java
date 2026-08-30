package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCoachCancelEntryRequest(
    @NotNull(message = "教练 ID 不能为空") Long coachId,
    @NotBlank(message = "取消原因不能为空")
        @Size(max = 512, message = "取消原因不能超过 512 个字符")
        String reason) {}
