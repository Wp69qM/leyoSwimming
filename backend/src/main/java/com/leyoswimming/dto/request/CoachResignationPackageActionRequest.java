package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record CoachResignationPackageActionRequest(
    @NotNull Long ticketId,
    @NotNull Long packageId,
    @NotNull String action,
    Long targetCoachId) {}
