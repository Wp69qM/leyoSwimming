package com.leyoswimming.dto.response;

public record UserPackageQualificationResponse(
    boolean hasActivePackage,
    Long activePackageId,
    Long activeCoachId,
    boolean hasExperiencePackage) {}
