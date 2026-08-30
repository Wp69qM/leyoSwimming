package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record AdminAccountAuditLogResponse(
    Long logId,
    Long operatorId,
    String operatorName,
    String action,
    String reason,
    String ip,
    LocalDateTime createdAt) {}
