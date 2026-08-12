package com.leyoswimming.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAccountDetailResponse(
    Long adminId,
    String username,
    String name,
    String role,
    Integer status,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer version,
    List<AdminAccountAuditLogResponse> auditLogs) {}
