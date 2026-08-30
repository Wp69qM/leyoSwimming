package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public record AdminPackageListRequest(
    String status,
    String packageMode,
    String teachingType,
    String keyword,
    LocalDate expireAtStart,
    LocalDate expireAtEnd,
    @Min(1) int page,
    @Min(1) int pageSize) {

  public AdminPackageListRequest {
    if (page < 1) {
      page = 1;
    }
    if (pageSize < 1) {
      pageSize = 10;
    }
  }
}
