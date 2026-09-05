package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record KnowledgeToggleRequest(
    @NotNull(message = "文档 ID 不能为空") Long documentId) {}
