package com.leyoswimming.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record AdminResignationTicketListRequest(
    String status,
    @Min(1) Integer page,
    @Min(1) Integer pageSize,
    String keyword,
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "开始日期格式必须为 YYYY-MM-DD")
    String submitStartDate,
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "结束日期格式必须为 YYYY-MM-DD")
    String submitEndDate) {

  @AssertTrue(message = "开始日期不能晚于结束日期")
  public boolean isDateRangeValid() {
    if (submitStartDate == null || submitStartDate.isBlank()
        || submitEndDate == null || submitEndDate.isBlank()) {
      return true;
    }
    return submitStartDate.compareTo(submitEndDate) <= 0;
  }
}
