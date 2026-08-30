package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CoachResignationDetailResponse(
    Long ticketId,
    String ticketNo,
    String status,
    String reason,
    Integer totalPackages,
    Integer handledPackages,
    LocalDateTime submittedAt,
    List<PackageItem> packages) {

  public record PackageItem(
      Long packageId,
      String packageNo,
      Long userId,
      String userName,
      String packageName,
      String packageMode,
      Integer totalHours,
      Integer availableCount,
      Integer reservedCount,
      BigDecimal pricePerHour,
      String action,
      Long targetCoachId,
      String targetCoachName,
      String targetCoachPhone) {}
}
