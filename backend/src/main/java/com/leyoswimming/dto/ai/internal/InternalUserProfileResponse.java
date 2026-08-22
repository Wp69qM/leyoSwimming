package com.leyoswimming.dto.ai.internal;

public record InternalUserProfileResponse(
    Long userId,
    String userHash,
    Integer age,
    String targetStroke,
    String swimmingLevel,
    Integer budget,
    Boolean isMinor) {
}
