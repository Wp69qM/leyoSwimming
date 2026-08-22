package com.leyoswimming.dto.response;

public record OrderPayResponse(
    Long paymentId,
    String channelTradeNo,
    String status) {}
