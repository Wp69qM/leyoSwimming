package com.leyoswimming.dto.response;

public record CoachLoginResponse(
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    boolean isNewCoach,
    int coachStatus,
    Long coachId,
    boolean profileCompleted) {}
