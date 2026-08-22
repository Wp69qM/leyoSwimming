package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CoachStudentListRequest(
    @NotBlank(message = "tab 不能为空") String tab,
    String keyword) {}
