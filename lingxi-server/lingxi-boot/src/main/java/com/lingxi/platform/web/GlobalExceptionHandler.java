package com.lingxi.platform.web;

import com.lingxi.kernel.ApiResponse;
import com.lingxi.kernel.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 新 C 端 API 的统一异常映射。 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(
      BusinessException exception, HttpServletRequest request) {
    return ResponseEntity.status(status(exception.getCode()))
        .body(ApiResponse.failure(exception.getCode(), exception.getMessage(), requestId(request)));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("请求参数不合法");
    return ResponseEntity.badRequest()
        .body(ApiResponse.failure("COMMON_VALIDATION_FAILED", message, requestId(request)));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    ServletRequestBindingException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(
      Exception exception, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.failure("COMMON_INVALID_REQUEST", "请求格式或参数类型不正确", requestId(request)));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    log.error("未处理的服务端异常, requestId={}", requestId(request), exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.failure("COMMON_INTERNAL_ERROR", "服务暂时不可用", requestId(request)));
  }

  private HttpStatus status(String code) {
    if (code == null) {
      return HttpStatus.BAD_REQUEST;
    }
    if (code.endsWith("FORBIDDEN")
        || code.endsWith("ACCESS_DENIED")
        || code.equals("AUTH_ADMIN_REQUIRED")) {
      return HttpStatus.FORBIDDEN;
    }
    if (code.startsWith("AUTH_")) {
      return HttpStatus.UNAUTHORIZED;
    }
    if (code.endsWith("NOT_FOUND")) {
      return HttpStatus.NOT_FOUND;
    }
    if (code.endsWith("CONFLICT")
        || code.contains("IDEMPOTENCY_CONFLICT")
        || code.endsWith("CONCURRENT_UPDATE")) {
      return HttpStatus.CONFLICT;
    }
    return HttpStatus.BAD_REQUEST;
  }

  private String requestId(HttpServletRequest request) {
    Object requestId = request.getAttribute(RequestTraceFilter.REQUEST_ID_ATTRIBUTE);
    return requestId == null ? "unknown" : requestId.toString();
  }
}
