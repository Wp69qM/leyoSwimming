package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCoachApplicationRejectRequest(
    @NotNull(message = "申请 ID 不能为空") Long applicationId,
    @NotBlank(message = "驳回原因不能为空") @Size(max = 512, message = "驳回原因长度不能超过 512") String reason) {}
