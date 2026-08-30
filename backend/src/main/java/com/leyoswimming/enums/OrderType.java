package com.leyoswimming.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderType {
  PURCHASE("purchase"),
  REFUND("refund");

  private final String value;

  public static OrderType fromValue(String value) {
    for (OrderType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    return null;
  }
}
