package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDetailResponse(
    Long orderId,
    String orderNo,
    String type,
    String status,
    BigDecimal amount,
    BigDecimal paidAmount,
    LocalDateTime paidAt,
    LocalDateTime expireAt,
    Long packageId,
    String packageName,
    String packageMode,
    BigDecimal originalAmount,
    String coachName,
    String teachingType,
    Integer totalHours,
    Integer durationMinutes,
    Integer validDays,
    LocalDateTime createdAt) {}
