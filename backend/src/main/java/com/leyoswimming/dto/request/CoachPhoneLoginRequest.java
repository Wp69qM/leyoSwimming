package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CoachPhoneLoginRequest(
    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
    @NotBlank @Size(min = 6, max = 6) String code,
    @NotNull Boolean termsAccepted,
    @NotNull Boolean privacyAccepted,
    @NotBlank String termsVersion,
    @NotBlank String privacyVersion) {}
