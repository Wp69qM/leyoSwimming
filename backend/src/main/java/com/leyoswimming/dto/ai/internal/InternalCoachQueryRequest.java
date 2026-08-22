package com.leyoswimming.dto.ai.internal;

public record InternalCoachQueryRequest(
    String stroke,
    String gender,
    Integer minPrice,
    Integer maxPrice,
    String classSize,
    Integer limit) {
}
