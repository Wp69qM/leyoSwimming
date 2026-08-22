package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PackageDetailCoachResponse(
    Long coachId,
    String name,
    String avatarUrl,
    BigDecimal rating,
    Integer teachingYears,
    Integer totalStudents,
    BigDecimal referencePrice,
    List<String> teachingStrokes,
    Integer status) {}
