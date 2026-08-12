package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record AdminCoachListItemResponse(
    Long coachId,
    String name,
    String gender,
    Integer age,
    Integer teachingYears,
    String teachingStrokes,
    LocalDateTime approvedAt,
    String tenure,
    Integer status,
    Long currentStudentCount,
    String realtimeStatus) {}
