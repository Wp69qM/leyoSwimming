package com.leyoswimming.dto.ai.gateway;

import java.time.LocalDateTime;

public record AiSessionListItemResponse(
    String sessionId,
    String title,
    LocalDateTime lastMessageAt,
    Long messageCount) {
}
