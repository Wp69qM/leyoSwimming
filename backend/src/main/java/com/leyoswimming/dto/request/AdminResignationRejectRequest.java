package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminResignationRejectRequest(
    @NotNull Long ticketId, @Size(max = 500) String reason) {}
