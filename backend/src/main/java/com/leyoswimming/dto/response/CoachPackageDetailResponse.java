package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CoachPackageDetailResponse(
    Long packageId,
    String packageName,
    String packageMode,
    String status,
    String statusLabel,
    String teachingType,
    String strokeNames,
    Integer durationMinutes,
    String validStart,
    String validEnd,
    Integer totalHours,
    Integer consumedHours,
    Integer availableHours,
    Integer reservedHours,
    BigDecimal paidAmount,
    StudentMiniCard student,
    List<UsageRecord> usageRecords) {

  public record StudentMiniCard(
      Long userId,
      String name,
      String avatarUrl,
      Integer age,
      String gender) {}

  public record UsageRecord(
      Long bookingId,
      String startTime,
      String status,
      String statusLabel,
      Integer consumedHours,
      String cancelReason) {}
}
