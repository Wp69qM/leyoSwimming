package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record CoachResignationSubmitRequest(@NotNull Long ticketId) {}
