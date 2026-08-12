package com.leyoswimming.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum SwimStroke {
  BREASTSTROKE("breaststroke", "蛙泳"),
  FREESTYLE("freestyle", "自由泳"),
  BACKSTROKE("backstroke", "仰泳"),
  BUTTERFLY("butterfly", "蝶泳");

  private final String code;
  private final String label;

  private static final Map<String, String> CODE_TO_LABEL =
      Arrays.stream(values())
          .collect(Collectors.toMap(SwimStroke::getCode, SwimStroke::getLabel));

  private static final Map<String, String> LABEL_TO_CODE =
      Arrays.stream(values())
          .collect(Collectors.toMap(SwimStroke::getLabel, SwimStroke::getCode));

  SwimStroke(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static String toLabel(String code) {
    return CODE_TO_LABEL.getOrDefault(code, code);
  }

  public static String toCode(String label) {
    return LABEL_TO_CODE.getOrDefault(label, label);
  }

  public static boolean isValidCode(String code) {
    return CODE_TO_LABEL.containsKey(code);
  }

  public static boolean isValidLabel(String label) {
    return LABEL_TO_CODE.containsKey(label);
  }
}
