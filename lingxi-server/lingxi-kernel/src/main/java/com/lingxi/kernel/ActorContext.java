package com.lingxi.kernel;

import java.util.Objects;

/**
 * 由认证层构造并传入业务模块的操作主体上下文。
 *
 * @param actorId 用户或管理员内部标识
 * @param actorType 主体类型
 * @param deviceId 设备标识
 * @param authorizationVersion 授权版本，用于拒绝年龄、监护或协议变化前的旧会话
 * @param recentAuthentication 是否在服务端认可的近期认证窗口内
 * @param requestId 请求追踪标识
 */
public record ActorContext(
    long actorId,
    ActorType actorType,
    String deviceId,
    long authorizationVersion,
    boolean recentAuthentication,
    String requestId) {

  public ActorContext {
    if (actorId <= 0) {
      throw new IllegalArgumentException("actorId must be positive");
    }
    Objects.requireNonNull(actorType, "actorType");
    Objects.requireNonNull(requestId, "requestId");
  }

  public enum ActorType {
    USER,
    ADMIN,
    SYSTEM
  }
}
