package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminOrderListItemResponse(
    Long orderId,
    String orderNo,
    String type,
    String status,
    Long userId,
    String userName,
    Long coachId,
    String coachName,
    Long packageId,
    BigDecimal originalAmount,
    BigDecimal discountAmount,
    BigDecimal paidAmount,
    String paymentMethod,
    String reason,
    LocalDateTime createdAt) {}
