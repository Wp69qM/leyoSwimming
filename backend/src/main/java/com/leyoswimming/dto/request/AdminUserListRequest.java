package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminUserListRequest(
    Integer identity,
    Integer status,
    Boolean profileCompleted,
    String startDate,
    String endDate,
    String keyword,
    @Min(value = 1, message = "页码必须大于等于 1") Integer page,
    @Min(value = 1, message = "每页条数必须大于等于 1")
        @Max(value = 100, message = "每页条数不能超过 100")
        Integer pageSize) {

  public AdminUserListRequest {
    if (page == null || page < 1) {
      page = 1;
    }
    if (pageSize == null || pageSize < 1) {
      pageSize = 20;
    }
  }
}
