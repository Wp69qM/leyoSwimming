package com.leyoswimming.dto.response;

public record AdminPackageTemplateCustomConfigResponse(
    Long configId, Integer minHours, Integer maxHours, Integer defaultValidDays) {}
