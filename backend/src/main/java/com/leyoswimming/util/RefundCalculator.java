package com.leyoswimming.util;

import com.leyoswimming.entity.CoursePackage;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RefundCalculator {

  private RefundCalculator() {}

  /**
   * 计算未消耗课时退款金额：单价 × (available + reserved)，已消耗不退。
   */
  public static BigDecimal calculateResignationRefund(CoursePackage pkg) {
    int remainingHours =
        pkg.getAvailableCount() == null ? 0 : pkg.getAvailableCount();
    int reserved = pkg.getReservedCount() == null ? 0 : pkg.getReservedCount();
    BigDecimal price = pkg.getPricePerHour() == null ? BigDecimal.ZERO : pkg.getPricePerHour();
    return price.multiply(BigDecimal.valueOf(remainingHours + reserved))
        .setScale(2, RoundingMode.HALF_UP);
  }
}
