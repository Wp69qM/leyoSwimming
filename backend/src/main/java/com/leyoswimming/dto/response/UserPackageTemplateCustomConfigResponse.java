package com.leyoswimming.dto.response;

import java.util.List;

public record UserPackageTemplateCustomConfigResponse(
    Integer minHours,
    Integer maxHours,
    Integer defaultValidDays,
    List<Integer> allowedValidDays) {}
