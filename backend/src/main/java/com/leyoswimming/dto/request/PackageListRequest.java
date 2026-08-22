package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PackageListRequest(
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码不能小于 1")
        Integer page,
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 50, message = "每页条数不能超过 50")
        Integer pageSize) {}
