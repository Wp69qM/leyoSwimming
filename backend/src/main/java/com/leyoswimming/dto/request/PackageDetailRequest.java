package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record PackageDetailRequest(
    @NotNull(message = "套餐 ID 不能为空") Long packageId, Long coachId) {}
