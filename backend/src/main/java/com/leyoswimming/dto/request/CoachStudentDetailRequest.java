package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record CoachStudentDetailRequest(@NotNull(message = "studentId 不能为空") Long studentId) {}
