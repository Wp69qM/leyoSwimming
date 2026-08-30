package com.leyoswimming.dto.ai.internal;

import java.util.List;

public record InternalCoachItemResponse(
    String coachHash,
    Long coachId,
    String name,
    String avatarUrl,
    String gender,
    Integer age,
    Double rating,
    Integer referencePrice,
    Integer teachingYears,
    List<String> teachingStrokes,
    String description) {
}
