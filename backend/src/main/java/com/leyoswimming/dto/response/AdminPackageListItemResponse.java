package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPackageListItemResponse(
    Long packageId,
    String packageNo,
    Long userId,
    String userName,
    Long coachId,
    String coachName,
    String packageMode,
    String teachingType,
    String status,
    String frozenReason,
    Integer totalHours,
    Integer consumedCount,
    Integer availableCount,
    Integer reservedCount,
    LocalDateTime expireAt,
    LocalDateTime createdAt,
    Boolean refundEnabled,
    Integer refundValidDays,
    BigDecimal refundRatio,
    BigDecimal refundAmount,
    Integer version) {}
