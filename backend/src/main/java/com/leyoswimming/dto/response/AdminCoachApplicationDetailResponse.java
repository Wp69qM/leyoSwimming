package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminCoachApplicationDetailResponse(
    Long coachId,
    Long applicationId,
    String status,
    Integer previousCoachStatus,
    String name,
    String phone,
    String gender,
    Integer age,
    String email,
    String wechatQrUrl,
    String idCardNo,
    Integer teachingYears,
    Integer totalStudents,
    Integer totalHours,
    String teachingStrokes,
    String bio,
    BigDecimal referencePrice,
    LocalDateTime submittedAt,
    LocalDateTime approvedAt,
    Long approvedBy,
    String rejectionReason,
    List<CoachApplicationCertificateResponse> certificates,
    List<CoachApplicationHistoryItem> history,
    List<CoachAuditLogItem> auditLogs) {

  public record CoachApplicationHistoryItem(
      Long applicationId,
      String status,
      LocalDateTime submittedAt,
      LocalDateTime approvedAt,
      Long approvedBy,
      String rejectionReason) {}

  public record CoachAuditLogItem(
      Long logId,
      Long adminId,
      String action,
      Integer fromStatus,
      Integer toStatus,
      String reason,
      LocalDateTime createdAt) {}
}
