package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CoachReferencePriceResponse(
    BigDecimal referencePrice, LocalDateTime priceChangedAt, Integer remainingChangesToday) {}
