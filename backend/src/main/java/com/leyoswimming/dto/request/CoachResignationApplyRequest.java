package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Size;

public record CoachResignationApplyRequest(
    @Size(max = 500) String reason, String idempotencyKey) {}
