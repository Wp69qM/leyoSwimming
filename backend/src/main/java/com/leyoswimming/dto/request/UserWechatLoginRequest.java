package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserWechatLoginRequest(
    @NotBlank String code,
    @NotBlank String phoneEncryptedData,
    @NotBlank String phoneIv,
    @NotNull Boolean termsAccepted,
    @NotNull Boolean privacyAccepted,
    String avatarUrl,
    String nickName) {}
