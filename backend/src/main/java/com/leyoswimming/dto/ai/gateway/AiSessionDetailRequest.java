package com.leyoswimming.dto.ai.gateway;

import jakarta.validation.constraints.NotBlank;

public record AiSessionDetailRequest(@NotBlank(message = "sessionId 不能为空") String sessionId) {
}
