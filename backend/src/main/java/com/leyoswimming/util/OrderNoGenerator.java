package com.leyoswimming.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class OrderNoGenerator {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private OrderNoGenerator() {}

  public static String generateOrderNo(String prefix) {
    String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
    int random = ThreadLocalRandom.current().nextInt(1000, 10000);
    return prefix + timestamp + random;
  }
}
