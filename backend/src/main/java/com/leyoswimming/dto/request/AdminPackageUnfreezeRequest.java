package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminPackageUnfreezeRequest(
    @NotNull(message = "套餐 ID 不能为空")
    Long packageId,
    @NotNull(message = "版本号不能为空")
    Integer version) {}
