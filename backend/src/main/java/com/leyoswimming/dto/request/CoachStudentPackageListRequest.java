package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record CoachStudentPackageListRequest(
    @NotNull(message = "studentId 不能为空") Long studentId) {}
