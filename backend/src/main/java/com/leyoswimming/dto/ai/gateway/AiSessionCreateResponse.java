package com.leyoswimming.dto.ai.gateway;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record AiSessionCreateResponse(
    @JsonAlias("session_id") String sessionId,
    @JsonAlias("welcome_message") String welcomeMessage,
    @JsonAlias("suggested_questions") List<String> suggestedQuestions) {
}
