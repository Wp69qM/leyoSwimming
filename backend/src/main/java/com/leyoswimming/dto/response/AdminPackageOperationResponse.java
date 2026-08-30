package com.leyoswimming.dto.response;

public record AdminPackageOperationResponse(String message, String orderNo) {

  public static AdminPackageOperationResponse of(String message) {
    return new AdminPackageOperationResponse(message, null);
  }

  public static AdminPackageOperationResponse of(String message, String orderNo) {
    return new AdminPackageOperationResponse(message, orderNo);
  }
}
