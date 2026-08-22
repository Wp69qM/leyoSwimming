package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPackageTemplateDetailResponse(
    Long packageTemplateId,
    String name,
    String packageMode,
    String teachingType,
    List<Long> coachIds,
    List<CoachBriefResponse> coachList,
    List<Integer> strokeIds,
    Integer totalHours,
    Integer durationMinutes,
    Integer validDays,
    BigDecimal originalPrice,
    BigDecimal price,
    Boolean refundEnabled,
    BigDecimal refundRatio,
    Integer refundValidDays,
    List<String> tags,
    String description,
    List<String> images,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer version) {}
