package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminAccountDetailRequest(@NotNull(message = "管理员 ID 不能为空") Long adminId) {}
