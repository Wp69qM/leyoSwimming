package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CoachApplicationResponse(
    Long coachId,
    Long applicationId,
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
    List<String> teachingStrokes,
    String bio,
    BigDecimal referencePrice,
    Integer status,
    String applicationStatus,
    LocalDateTime submittedAt,
    String entryType,
    String promptMessage,
    List<CoachApplicationCertificateResponse> certificates) {}
