package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record CoachApplicationSubmitResponse(
    Long coachId, Long applicationId, Integer status, LocalDateTime submittedAt) {}
