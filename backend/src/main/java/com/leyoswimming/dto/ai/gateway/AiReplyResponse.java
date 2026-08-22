package com.leyoswimming.dto.ai.gateway;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record AiReplyResponse(
    String text,
    List<AiRecommendationResponse> recommendations,
    @JsonAlias("suggested_questions") List<String> suggestedQuestions) {
}
