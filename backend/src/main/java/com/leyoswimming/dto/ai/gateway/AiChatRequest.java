package com.leyoswimming.dto.ai.gateway;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
    @NotBlank(message = "sessionId 不能为空") String sessionId,
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容不能超过 500 字") String message) {
}
