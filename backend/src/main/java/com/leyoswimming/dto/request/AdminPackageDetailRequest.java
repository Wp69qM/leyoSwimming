package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminPackageDetailRequest(
    @NotNull(message = "套餐 ID 不能为空")
    Long packageId) {}
