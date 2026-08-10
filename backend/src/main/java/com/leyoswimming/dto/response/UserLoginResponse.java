package com.leyoswimming.dto.response;

public record UserLoginResponse(
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    boolean isNewUser,
    boolean profileCompleted,
    Long userId) {}
