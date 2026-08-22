package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record CoachPackageListRequest(
    @NotNull(message = "教练 ID 不能为空") Long coachId) {}
