package com.leyoswimming.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CoachCertificateType {
  ID_CARD_FRONT("ID_CARD_FRONT"),
  ID_CARD_BACK("ID_CARD_BACK"),
  COACH_CERT("COACH_CERT"),
  HEALTH_CERT("HEALTH_CERT"),
  PORTRAIT("PORTRAIT"),
  OTHER("OTHER");

  private final String value;

  public static boolean isValid(String type) {
    if (type == null) {
      return false;
    }
    return Arrays.stream(values()).anyMatch(e -> e.value.equals(type));
  }

  public static Set<String> valuesSet() {
    return Arrays.stream(values()).map(CoachCertificateType::getValue).collect(Collectors.toSet());
  }
}
