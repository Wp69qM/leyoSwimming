package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PackageDetailResponse(
    Long id,
    String name,
    String packageMode,
    String teachingType,
    Integer totalHours,
    Integer durationMinutes,
    Integer validDays,
    BigDecimal originalPrice,
    BigDecimal price,
    Boolean refundEnabled,
    BigDecimal refundRatio,
    Integer refundValidDays,
    String refundPolicySummary,
    List<String> tags,
    String description,
    List<String> images,
    List<PackageDetailCoachResponse> applicableCoaches) {}
