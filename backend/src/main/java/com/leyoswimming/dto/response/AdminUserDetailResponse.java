package com.leyoswimming.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserDetailResponse(
    Long userId,
    String avatarUrl,
    String name,
    String phone,
    Integer gender,
    Integer age,
    Integer identity,
    Integer status,
    Boolean profileCompleted,
    String source,
    Boolean hasSwimBasis,
    List<String> swimStrokes,
    Integer swimYears,
    String personalDesc,
    String guardianName,
    String guardianPhone,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer version) {}
