package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CoachPackageListResponse(
    Long coachId,
    Integer coachStatus,
    BigDecimal referencePrice,
    List<PackageListItemResponse> standardPackages,
    Boolean customPackageEnabled,
    Integer customHoursMin,
    Integer customHoursMax) {}
