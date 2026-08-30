package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPackageTemplateListItemResponse(
    Long packageTemplateId,
    String name,
    String packageMode,
    String teachingType,
    List<Long> coachIds,
    List<String> coachNames,
    Integer totalHours,
    Integer durationMinutes,
    Integer validDays,
    BigDecimal originalPrice,
    BigDecimal price,
    Boolean refundEnabled,
    String status,
    LocalDateTime createdAt) {}
