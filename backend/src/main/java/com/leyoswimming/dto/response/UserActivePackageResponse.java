package com.leyoswimming.dto.response;

public record UserActivePackageResponse(
    Long id, Long coachId, String coachName, String status) {}
