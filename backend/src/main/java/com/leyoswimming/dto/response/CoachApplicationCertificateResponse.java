package com.leyoswimming.dto.response;

public record CoachApplicationCertificateResponse(
    Long certId, String certType, String imageUrl, Integer sortOrder) {}
