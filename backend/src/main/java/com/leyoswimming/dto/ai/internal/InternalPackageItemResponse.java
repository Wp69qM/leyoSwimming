package com.leyoswimming.dto.ai.internal;

import java.util.List;

public record InternalPackageItemResponse(
    String packageHash,
    Long packageId,
    String packageMode,
    String name,
    Integer hours,
    Integer price,
    Integer pricePerHour,
    Integer validityDays,
    String classSize,
    List<String> strokes,
    Long coachId,
    String coachName,
    Integer referencePrice) {
}
