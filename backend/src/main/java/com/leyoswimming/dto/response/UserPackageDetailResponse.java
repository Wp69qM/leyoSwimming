package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UserPackageDetailResponse(
    Long packageId,
    String packageName,
    String packageMode,
    String coachName,
    String teachingType,
    List<Integer> strokeIds,
    Integer durationMinutes,
    Integer validDays,
    Integer totalHours,
    Integer consumedCount,
    Integer availableCount,
    BigDecimal paidAmount,
    BigDecimal originalPrice,
    Boolean refundEnabled,
    Integer refundValidDays,
    BigDecimal refundRatio,
    String refundRuleText,
    String status,
    String frozenReason,
    LocalDateTime expireAt,
    LocalDateTime createdAt,
    Boolean canRefund) {}
