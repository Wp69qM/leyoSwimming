package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminResignationTicketDetailResponse(
    Long ticketId,
    String ticketNo,
    String status,
    String reason,
    Integer totalPackages,
    Integer handledPackages,
    Boolean scheduleCleared,
    Integer settlementStatus,
    LocalDateTime submittedAt,
    CoachInfo coach,
    List<PackageItem> packages,
    Checklist checklist) {

  public record CoachInfo(
      Long coachId,
      String name,
      String phone,
      Integer status,
      LocalDateTime submittedAt,
      LocalDateTime joinedAt) {}

  public record PackageItem(
      Long packageId,
      Long userId,
      String userName,
      Integer totalHours,
      Integer availableCount,
      Integer reservedCount,
      BigDecimal pricePerHour,
      String action,
      Long targetCoachId,
      BigDecimal refundAmount) {}

  public record Checklist(
      boolean allActionsRegistered,
      boolean scheduleCleared,
      boolean settlementCompleted) {}
}
