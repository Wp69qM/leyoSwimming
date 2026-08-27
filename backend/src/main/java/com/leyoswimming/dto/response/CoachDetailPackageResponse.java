package com.leyoswimming.dto.response;

import java.math.BigDecimal;

public record CoachDetailPackageResponse(
    Long id,
    String name,
    String packageMode,
    BigDecimal price,
    Integer totalHours,
    Integer validDays,
    String imageUrl) {}
