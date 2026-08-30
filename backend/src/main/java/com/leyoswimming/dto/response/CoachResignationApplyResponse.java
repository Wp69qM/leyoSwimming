package com.leyoswimming.dto.response;

public record CoachResignationApplyResponse(
    Long ticketId, String ticketNo, Integer totalPackages, String message) {}
