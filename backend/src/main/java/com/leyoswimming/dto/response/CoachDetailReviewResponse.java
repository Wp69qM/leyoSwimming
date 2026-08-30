package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record CoachDetailReviewResponse(
    Long id, String userName, Integer rating, String content, LocalDateTime createdAt) {}
