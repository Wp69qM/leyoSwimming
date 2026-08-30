package com.leyoswimming.dto.ai.gateway;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AiChatResponse(
    @JsonAlias("session_id") String sessionId,
    @JsonAlias("message_id") String messageId,
    AiReplyResponse reply) {
}
