package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record CoachPackageDetailRequest(
    @NotNull(message = "packageId 不能为空") Long packageId) {}
