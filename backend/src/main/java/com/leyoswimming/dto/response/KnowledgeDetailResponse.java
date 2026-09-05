package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record KnowledgeDetailResponse(
    Long documentId,
    String title,
    String category,
    String contentType,
    String sourceType,
    String content,
    Integer status,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
