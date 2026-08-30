package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PackageListItemResponse(
    Long id,
    String name,
    String packageMode,
    String teachingType,
    Integer totalHours,
    Integer durationMinutes,
    Integer validDays,
    BigDecimal originalPrice,
    BigDecimal price,
    List<String> tags,
    String imageUrl,
    String refundPolicySummary) {}
