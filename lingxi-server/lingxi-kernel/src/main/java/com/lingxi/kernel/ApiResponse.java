package com.lingxi.kernel;

/**
 * 用户端与管理端统一响应结构。
 *
 * @param code 业务响应码，成功固定为 OK
 * @param message 面向调用方的响应说明
 * @param data 业务数据
 * @param requestId 请求追踪标识
 * @param <T> 数据类型
 */
public record ApiResponse<T>(String code, String message, T data, String requestId) {

  public static <T> ApiResponse<T> success(T data, String requestId) {
    return new ApiResponse<>("OK", "success", data, requestId);
  }

  public static ApiResponse<Void> failure(String code, String message, String requestId) {
    return new ApiResponse<>(code, message, null, requestId);
  }
}
