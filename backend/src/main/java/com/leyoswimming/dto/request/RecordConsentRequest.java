package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RecordConsentRequest(
    @NotBlank String termsVersion, @NotBlank String privacyVersion) {}
