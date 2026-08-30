package com.leyoswimming.dto.ai.internal;

public record InternalCoachQueryRequest(
    String stroke,
    String gender,
    Integer minPrice,
    Integer maxPrice,
    Integer maxAge,
    String classSize,
    Integer limit) {
}
