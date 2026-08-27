package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record OrderTrialRequest(
    @NotNull(message = "教练 ID 不能为空") Long coachId,
    @NotNull(message = "套餐 ID 不能为空") Long packageId) {}
