package com.leyoswimming.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
  PENDING_PAYMENT("pending_payment"),
  PAID("paid"),
  CANCELLED("cancelled"),
  REFUND_PENDING("refund_pending"),
  REFUND_PROCESSING("refund_processing"),
  REFUNDED("refunded"),
  REJECTED("rejected"),
  DISPUTE_PROCESSING("dispute_processing");

  private final String value;

  public static OrderStatus fromValue(String value) {
    for (OrderStatus status : values()) {
      if (status.value.equalsIgnoreCase(value)) {
        return status;
      }
    }
    return null;
  }
}
