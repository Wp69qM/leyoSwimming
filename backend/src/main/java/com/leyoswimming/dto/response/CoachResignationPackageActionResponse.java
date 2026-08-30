package com.leyoswimming.dto.response;

import java.math.BigDecimal;

public record CoachResignationPackageActionResponse(
    Long actionId, Long packageId, String action, BigDecimal refundAmount) {}
