package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminResignationTicketDetailRequest(@NotNull Long ticketId) {}
