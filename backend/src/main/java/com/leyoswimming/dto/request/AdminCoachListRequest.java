package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminCoachListRequest(
    String keyword,
    String status,
    String realtimeStatus,
    @Min(value = 1, message = "页码不能小于 1") int page,
    @Min(value = 1, message = "每页数量不能小于 1")
        @Max(value = 100, message = "每页数量不能超过 100")
        int pageSize) {}
