package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminResignationApproveRequest(@NotNull Long ticketId, String comment) {}
