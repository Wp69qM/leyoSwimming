package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderDetailResponse(
    Long orderId,
    String orderNo,
    String type,
    String status,
    Long userId,
    String userName,
    String userPhone,
    Long coachId,
    String coachName,
    String teachingType,
    Long packageId,
    Long purchaseOrderId,
    String purchaseOrderNo,
    BigDecimal originalAmount,
    BigDecimal discountAmount,
    BigDecimal paidAmount,
    BigDecimal calculatedRefundAmount,
    String paymentMethod,
    String channelTradeNo,
    String reason,
    String rejectedReason,
    String adjustReason,
    Long approvedBy,
    LocalDateTime approvedAt,
    LocalDateTime refundedAt,
    LocalDateTime createdAt,
    List<OrderStatusLog> statusTimeline,
    PackageSnapshot packageSnapshot) {

  public record OrderStatusLog(String status, LocalDateTime time, String description) {}

  public record PackageSnapshot(
      Long packageId,
      String packageNo,
      String status,
      Integer totalHours,
      Integer availableCount,
      LocalDateTime expireAt) {}
}
