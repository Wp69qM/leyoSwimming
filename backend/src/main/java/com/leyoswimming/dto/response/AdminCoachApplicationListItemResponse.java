package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record AdminCoachApplicationListItemResponse(
    Long coachId,
    Long applicationId,
    String name,
    String gender,
    Integer age,
    Integer teachingYears,
    String teachingStrokes,
    LocalDateTime submittedAt,
    String status,
    Integer previousCoachStatus,
    String phone) {}
