package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserPackageDetailRequest(@NotNull Long packageId) {}
