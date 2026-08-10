package com.leyoswimming.common;

import com.leyoswimming.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ApiResponse<Void> handleBusiness(BusinessException ex) {
    log.warn("Business exception: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
    return ApiResponse.error(ex.getErrorCode().getCode(), ex.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
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
    return ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message);
  }

  @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleBadRequest(Exception ex) {
    log.warn("Bad request: {}", ex.getMessage());
    return ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), "请求参数错误");
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> handleUnknown(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error: method={}, uri={}", request.getMethod(), request.getRequestURI(), ex);
    return ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
  }
}
