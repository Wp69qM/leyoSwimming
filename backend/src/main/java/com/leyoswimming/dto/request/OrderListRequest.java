package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderListRequest(
    String tab,
    @NotNull(message = "页码不能为空") @Min(value = 1, message = "页码必须大于等于 1") Integer page,
    @NotNull(message = "每页数量不能为空") @Min(value = 1, message = "每页数量必须大于等于 1") Integer size) {}
