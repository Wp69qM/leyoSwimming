package com.leyoswimming.dto.ai.gateway;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record AiRecommendationResponse(
    String type,
    Long id,
    @JsonAlias("coach_id") Long coachId,
    String name,
    @JsonAlias("coach_name") String coachName,
    @JsonAlias("avatar_url") String avatarUrl,
    Double rating,
    @JsonAlias("teaching_years") Integer teachingYears,
    @JsonAlias("reference_price") Integer referencePrice,
    Integer price,
    Integer hours,
    @JsonAlias("price_per_hour") Integer pricePerHour,
    @JsonAlias("total_price") Integer totalPrice,
    @JsonAlias("class_size") String classSize,
    @JsonAlias("validity_days") Integer validityDays,
    List<String> strokes,
    String reason) {
}
