package com.lingxi.kernel;

/** 领域或应用服务主动拒绝业务操作时抛出的异常。 */
public class BusinessException extends RuntimeException {

  private final String code;

  public BusinessException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
