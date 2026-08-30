package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CoachListItemResponse(
    Long id,
    String name,
    Integer status,
    String avatar,
    BigDecimal rating,
    Integer yearsOfTeaching,
    List<String> teachingStrokes,
    String realTimeStatus) {}
