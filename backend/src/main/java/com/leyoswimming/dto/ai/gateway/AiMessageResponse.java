package com.leyoswimming.dto.ai.gateway;

import java.time.LocalDateTime;

public record AiMessageResponse(
    String role,
    String content,
    LocalDateTime createdAt) {
}
