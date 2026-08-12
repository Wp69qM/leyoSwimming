package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAccountDeleteRequest(
    @NotNull(message = "管理员 ID 不能为空") Long adminId,
    @NotBlank(message = "删除原因不能为空")
        @Size(min = 2, max = 200, message = "删除原因需在 2-200 个字符之间")
        String reason) {}
