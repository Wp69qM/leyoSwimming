package com.leyoswimming.dto.response;

import java.util.List;

public record CoachStudentPackageListResponse(List<PackageItem> packages) {

  public record PackageItem(
      Long packageId,
      String packageName,
      String packageMode,
      String status,
      String statusLabel,
      String validStart,
      String validEnd,
      Integer remainingHours) {}
}
