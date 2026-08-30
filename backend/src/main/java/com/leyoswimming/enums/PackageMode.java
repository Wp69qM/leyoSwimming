package com.leyoswimming.enums;

import java.util.Objects;
import java.util.Set;

public enum PackageMode {
  STANDARD("standard", "正价套餐"),
  EXPERIENCE("experience", "体验课"),
  CUSTOM("custom", "自定义套餐");

  private final String value;
  private final String label;

  PackageMode(String value, String label) {
    this.value = value;
    this.label = label;
  }

  public String getValue() {
    return value;
  }

  public String getLabel() {
    return label;
  }

  public static boolean isValid(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    return Set.of(STANDARD.value, EXPERIENCE.value, CUSTOM.value).contains(value);
  }

  public static PackageMode fromValue(String value) {
    for (PackageMode mode : values()) {
      if (Objects.equals(mode.value, value)) {
        return mode;
      }
    }
    return null;
  }
}
