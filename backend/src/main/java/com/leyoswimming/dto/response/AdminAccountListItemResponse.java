package com.leyoswimming.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAccountListItemResponse(
    Long adminId,
    String username,
    String name,
    String phone,
    String role,
    Integer status,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt,
    List<String> allowedActions) {}
