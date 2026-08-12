package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record AdminUserListItemResponse(
    Long userId,
    String avatarUrl,
    String name,
    String phone,
    Integer gender,
    Integer age,
    Integer identity,
    Boolean profileCompleted,
    Integer status,
    LocalDateTime createdAt) {}
