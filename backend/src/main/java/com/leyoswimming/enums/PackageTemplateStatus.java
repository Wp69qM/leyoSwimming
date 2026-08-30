package com.leyoswimming.enums;

import java.util.Objects;
import java.util.Set;

public enum PackageTemplateStatus {
  ACTIVE("active", "已上架"),
  INACTIVE("inactive", "未上架");

  private final String value;
  private final String label;

  PackageTemplateStatus(String value, String label) {
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
    return Set.of(ACTIVE.value, INACTIVE.value).contains(value);
  }

  public static PackageTemplateStatus fromValue(String value) {
    for (PackageTemplateStatus status : values()) {
      if (Objects.equals(status.value, value)) {
        return status;
      }
    }
    return null;
  }
}
