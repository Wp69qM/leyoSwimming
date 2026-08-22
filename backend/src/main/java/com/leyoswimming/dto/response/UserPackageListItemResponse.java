package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserPackageListItemResponse(
    Long packageId,
    String packageName,
    String packageMode,
    String coachName,
    String teachingType,
    Integer durationMinutes,
    Integer validDays,
    Integer totalHours,
    Integer consumedCount,
    Integer availableCount,
    BigDecimal paidAmount,
    BigDecimal originalPrice,
    String status,
    String frozenReason,
    Boolean refundEnabled,
    Integer refundValidDays,
    BigDecimal refundRatio,
    LocalDateTime expireAt,
    LocalDateTime createdAt,
    Boolean canRefund) {}
