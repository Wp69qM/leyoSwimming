package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminPackageTemplateCustomConfigRequest(
    @NotNull(message = "最小课时不能为空") Integer minHours,
    @NotNull(message = "最大课时不能为空") Integer maxHours,
    @NotNull(message = "默认有效期不能为空") Integer defaultValidDays) {}
