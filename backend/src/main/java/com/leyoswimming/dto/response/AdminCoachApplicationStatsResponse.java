package com.leyoswimming.dto.response;

public record AdminCoachApplicationStatsResponse(
    long pendingCount,
    long todayNewCount,
    long overdue24hCount,
    long todayApprovedCount,
    long todayRejectedCount) {}
