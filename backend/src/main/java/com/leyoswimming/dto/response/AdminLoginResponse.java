package com.leyoswimming.dto.response;

public record AdminLoginResponse(String token, Long expiresIn, AdminInfo admin) {}
