package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record KnowledgeListRequest(
    @Min(value = 1, message = "page 不能小于 1") int page,
    @Min(value = 1, message = "pageSize 不能小于 1")
        @Max(value = 50, message = "pageSize 不能超过 50")
        int pageSize,
    String category,
    Integer status,
    String keyword) {

  public KnowledgeListRequest {
    if (page < 1) {
      page = 1;
    }
    if (pageSize < 1) {
      pageSize = 10;
    }
  }
}
