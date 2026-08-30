package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPackageDetailResponse(
    Long packageId,
    String packageNo,
    Long userId,
    String userName,
    String userPhone,
    Long coachId,
    String coachName,
    String packageMode,
    String packageName,
    String teachingType,
    List<Integer> strokeIds,
    Integer durationMinutes,
    Integer validDays,
    String status,
    String frozenReason,
    String extendReason,
    Integer totalHours,
    Integer consumedCount,
    Integer reservedCount,
    Integer availableCount,
    BigDecimal pricePerHour,
    BigDecimal paidAmount,
    BigDecimal originalPrice,
    Boolean refundEnabled,
    BigDecimal refundRatio,
    Integer refundValidDays,
    BigDecimal refundAmount,
    LocalDateTime pendingHandoverAt,
    LocalDateTime expireAt,
    LocalDateTime exhaustedAt,
    LocalDateTime refundedAt,
    LocalDateTime createdAt,
    Integer version,
    List<ConsumptionRecord> consumptionRecords,
    List<OrderInfo> relatedOrders) {

  public record ConsumptionRecord(
      Long bookingId,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String status,
      Integer hours,
      LocalDateTime createdAt) {}

  public record OrderInfo(
      Long orderId,
      String orderNo,
      String type,
      String status,
      BigDecimal amount,
      LocalDateTime createdAt) {}
}
