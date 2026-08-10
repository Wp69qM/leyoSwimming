package com.leyoswimming.dto.response;

public record RefreshTokenResponse(
    String accessToken, String refreshToken, long expiresInSeconds) {}
