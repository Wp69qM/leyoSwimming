package com.leyoswimming.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CoachStatus {
  NOT_SUBMITTED(-1),
  PENDING(0),
  APPROVED(1),
  REJECTED(2),
  RESIGNED(3),
  RESIGNING(4);

  private final int value;
}
