package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record KnowledgeListItemResponse(
    Long documentId,
    String title,
    String category,
    String contentType,
    String sourceType,
    Integer status,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
