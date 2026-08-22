package com.leyoswimming.dto.ai.internal;

public record InternalPackageQueryRequest(
    String stroke,
    String packageMode,
    Integer minPrice,
    Integer maxPrice,
    Integer hours,
    Integer limit) {
}
