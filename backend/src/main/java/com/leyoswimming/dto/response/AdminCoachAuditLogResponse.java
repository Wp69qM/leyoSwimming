package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record AdminCoachAuditLogResponse(
    Long logId,
    Long adminId,
    String action,
    Integer fromStatus,
    Integer toStatus,
    String reason,
    LocalDateTime createdAt) {}
