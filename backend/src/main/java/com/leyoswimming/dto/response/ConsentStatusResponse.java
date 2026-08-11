package com.leyoswimming.dto.response;

public record ConsentStatusResponse(
    String termsVersion,
    String privacyVersion,
    String requiredTermsVersion,
    String requiredPrivacyVersion,
    boolean needsReconsent) {}
