package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminCoachApplicationDetailRequest(@NotNull(message = "申请 ID 不能为空") Long applicationId) {}
