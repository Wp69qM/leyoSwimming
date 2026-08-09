package com.leyoswimming.common;

public record ApiResponse<T>(int code, String message, T data) {

  public static final int SUCCESS_CODE = 0;

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(SUCCESS_CODE, "success", data);
  }

  public static <T> ApiResponse<T> ok() {
    return new ApiResponse<>(SUCCESS_CODE, "success", null);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
