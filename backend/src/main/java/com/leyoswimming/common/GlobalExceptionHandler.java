package com.leyoswimming.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Bad request: {}", ex.getMessage());
    return ApiResponse.error(ex.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleValidation(Exception ex) {
    String message = "请求参数校验失败";
    if (ex instanceof MethodArgumentNotValidException e) {
      message =
          e.getBindingResult().getFieldErrors().stream()
              .findFirst()
              .map(error -> error.getField() + ": " + error.getDefaultMessage())
              .orElse(message);
    } else if (ex instanceof ConstraintViolationException e) {
      message = e.getConstraintViolations().stream().findFirst().map(Object::toString).orElse(message);
    }
    log.warn("Validation failed: {}", message);
    return ApiResponse.error(message);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> handleUnknown(Exception ex) {
    log.error("Unexpected error", ex);
    return ApiResponse.error("系统繁忙，请稍后重试");
  }
}
