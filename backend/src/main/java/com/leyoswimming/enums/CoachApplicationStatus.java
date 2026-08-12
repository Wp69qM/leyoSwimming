package com.leyoswimming.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CoachApplicationStatus {
  DRAFT("draft"),
  PENDING("pending"),
  APPROVED("approved"),
  REJECTED("rejected");

  private final String value;

  public static boolean isTerminal(String status) {
    return APPROVED.value.equals(status) || REJECTED.value.equals(status);
  }
}
