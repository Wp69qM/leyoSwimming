package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderListResponse(
    Long orderId,
    String orderNo,
    String status,
    BigDecimal amount,
    BigDecimal paidAmount,
    LocalDateTime expireAt,
    String packageName,
    String packageMode,
    String coachName,
    String teachingType,
    Integer totalHours,
    LocalDateTime createdAt) {}
