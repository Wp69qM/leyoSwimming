package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderFormalRequest(
    @NotNull(message = "教练 ID 不能为空") Long coachId,
    @NotNull(message = "套餐模板 ID 不能为空") Long packageId,
    Integer hours,
    Integer validDays,
    List<Integer> strokeIds,
    AgreementVersions agreementVersions) {

  public record AgreementVersions(
      String userNotice, String health, String disclaimer) {}
}
