package com.leyoswimming.dto.response;

import java.math.BigDecimal;

public record UserPackageRefundResponse(Long orderId, String orderNo, BigDecimal refundAmount) {}
