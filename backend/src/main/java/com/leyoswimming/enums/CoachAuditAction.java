package com.leyoswimming.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CoachAuditAction {
  DRAFT_SAVE("draft_save"),
  SUBMIT("submit"),
  APPROVE("approve"),
  REJECT("reject");

  private final String value;
}
