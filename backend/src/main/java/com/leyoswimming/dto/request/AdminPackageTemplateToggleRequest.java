package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminPackageTemplateToggleRequest(
    @NotNull(message = "套餐模板 ID 不能为空") Long packageTemplateId) {}
