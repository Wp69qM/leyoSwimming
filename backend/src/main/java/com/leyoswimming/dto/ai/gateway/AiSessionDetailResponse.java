package com.leyoswimming.dto.ai.gateway;

import java.util.List;

public record AiSessionDetailResponse(String sessionId, List<AiMessageResponse> messages) {
}
