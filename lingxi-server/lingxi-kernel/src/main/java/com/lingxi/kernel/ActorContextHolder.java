package com.lingxi.kernel;

/** 当前请求的服务端认证主体。仅由认证过滤器写入，并在请求结束时清理。 */
public final class ActorContextHolder {
  private static final ThreadLocal<ActorContext> CONTEXT = new ThreadLocal<>();

  private ActorContextHolder() {}

  public static void set(ActorContext context) {
    CONTEXT.set(context);
  }

  public static ActorContext requireCurrent() {
    ActorContext context = CONTEXT.get();
    if (context == null) {
      throw new BusinessException("AUTH_UNAUTHENTICATED", "请先登录");
    }
    return context;
  }

  /** 获取已认证的普通用户主体，避免管理端 Token 跨受众调用 C 端接口。 */
  public static ActorContext requireUser() {
    ActorContext context = requireCurrent();
    if (context.actorType() != ActorContext.ActorType.USER) {
      throw new BusinessException("AUTH_FORBIDDEN", "当前身份不能访问用户接口");
    }
    return context;
  }

  /** 获取已认证的管理员主体。 */
  public static ActorContext requireAdmin() {
    ActorContext context = requireCurrent();
    if (context.actorType() != ActorContext.ActorType.ADMIN) {
      throw new BusinessException("AUTH_ADMIN_REQUIRED", "需要管理员身份");
    }
    return context;
  }

  /** 校验近期认证事实；该事实只能由服务端认证适配器写入。 */
  public static void requireRecentAuthentication() {
    if (!requireCurrent().recentAuthentication()) {
      throw new BusinessException("AUTH_RECENT_AUTHENTICATION_REQUIRED", "该操作需要近期认证");
    }
  }

  public static void clear() {
    CONTEXT.remove();
  }
}
