package com.leyoswimming.dto.ai.internal;

import java.util.List;

public record InternalUserPackageItemResponse(
    String packageHash,
    Long packageId,
    String name,
    String status,
    Integer hours,
    Integer remainingHours,
    List<String> strokes) {
}
