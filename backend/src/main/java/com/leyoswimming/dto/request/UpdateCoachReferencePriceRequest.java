package com.leyoswimming.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateCoachReferencePriceRequest(
    @NotNull BigDecimal referencePrice,
    @NotBlank @Size(max = 64) String idempotencyKey) {}
