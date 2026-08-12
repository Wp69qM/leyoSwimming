package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminUserDetailRequest(@NotNull(message = "用户 ID 不能为空") Long userId) {}
