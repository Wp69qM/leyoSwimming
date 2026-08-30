package com.leyoswimming.dto.response;

public record PaymentMockCallbackResponse(String code) {
  public static PaymentMockCallbackResponse success() {
    return new PaymentMockCallbackResponse("SUCCESS");
  }
}
