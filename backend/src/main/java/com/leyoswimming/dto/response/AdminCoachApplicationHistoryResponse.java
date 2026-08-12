package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record AdminCoachApplicationHistoryResponse(
    Long applicationId,
    String status,
    String previousCoachStatus,
    String name,
    String phone,
    String gender,
    Integer age,
    String email,
    String idCardNo,
    Integer teachingYears,
    Integer totalStudents,
    Integer totalHours,
    String teachingStrokes,
    String bio,
    java.math.BigDecimal referencePrice,
    LocalDateTime submittedAt,
    LocalDateTime approvedAt,
    Long approvedBy,
    String rejectionReason,
    LocalDateTime createdAt) {}
