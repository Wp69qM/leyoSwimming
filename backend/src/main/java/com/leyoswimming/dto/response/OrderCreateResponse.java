package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreateResponse(
    Long orderId,
    String orderNo,
    BigDecimal amount,
    LocalDateTime expireAt) {}
