package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Min;

public record AdminOrderListRequest(
    String type,
    String status,
    String paymentMethod,
    String startDate,
    String endDate,
    String keyword,
    @Min(1) int page,
    @Min(1) int pageSize) {

  public AdminOrderListRequest {
    if (page < 1) {
      page = 1;
    }
    if (pageSize < 1) {
      pageSize = 10;
    }
  }
}
