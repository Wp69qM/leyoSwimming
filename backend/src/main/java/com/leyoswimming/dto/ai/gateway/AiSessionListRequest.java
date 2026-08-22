package com.leyoswimming.dto.ai.gateway;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AiSessionListRequest(
    @Min(value = 1, message = "页码不能小于 1") Integer page,
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 50, message = "每页条数不能超过 50") Integer size) {
}
