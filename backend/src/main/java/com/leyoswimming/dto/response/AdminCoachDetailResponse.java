package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminCoachDetailResponse(
    Long coachId,
    String avatarUrl,
    String phone,
    String name,
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
    Integer status,
    LocalDateTime approvedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer version,
    List<AdminCoachCertificateResponse> certificates,
    List<AdminCoachApplicationHistoryResponse> applicationHistory,
    List<AdminCoachAuditLogResponse> auditLogs) {}
