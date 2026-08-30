package com.leyoswimming.dto.response;

public record AdminCoachCertificateResponse(
    Long certificateId, String certType, String imageUrl, Integer sortOrder) {}
