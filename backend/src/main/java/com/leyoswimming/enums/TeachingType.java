package com.leyoswimming.enums;

import java.util.Objects;
import java.util.Set;

public enum TeachingType {
  ONE_ON_ONE("one_on_one", "一对一"),
  ONE_ON_TWO("one_on_two", "一对二"),
  ONE_ON_THREE("one_on_three", "一对三");

  private final String value;
  private final String label;

  TeachingType(String value, String label) {
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
    return Set.of(ONE_ON_ONE.value, ONE_ON_TWO.value, ONE_ON_THREE.value).contains(value);
  }

  public static TeachingType fromValue(String value) {
    for (TeachingType type : values()) {
      if (Objects.equals(type.value, value)) {
        return type;
      }
    }
    return null;
  }
}
