package com.leyoswimming.common;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

  private final boolean success;
  private final T data;
  private final String error;

  private ApiResponse(boolean success, T data, String error) {
    this.success = success;
    this.data = data;
    this.error = error;
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null);
  }

  public static <T> ApiResponse<T> ok() {
    return new ApiResponse<>(true, null, null);
  }

  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(false, null, message);
  }
}
