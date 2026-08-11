package com.leyoswimming.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActorType {
  user("user"),
  coach("coach");

  private final String value;
}
