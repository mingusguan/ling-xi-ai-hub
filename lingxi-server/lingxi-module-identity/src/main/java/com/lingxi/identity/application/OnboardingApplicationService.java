package com.lingxi.identity.application;

import com.lingxi.identity.api.OnboardingProfile;
import com.lingxi.identity.api.OnboardingProfileResult;

/**
 * 新手引导画像的应用服务端口。
 *
 * <p>端口定义在应用层而不是 {@code api} 包：控制器（接口层）直接依赖应用服务，
 * 只有跨模块调用才通过 {@code api} 包暴露；画像目前没有跨模块消费方。
 *
 * <p>实现放在 {@link IdentityApplicationService}：画像与账号状态机写的是同一行，
 * 共用同一个乐观锁与同一套版本冲突处理，拆成两个类会各自维护一份容易走偏的落库逻辑。
 */
public interface OnboardingApplicationService {

  /** 读取当前引导状态与已填画像。 */
  OnboardingProfileResult getOnboardingProfile(long userId);

  /**
   * 覆盖写入画像，并按需结束引导。
   *
   * @param userId 当前登录用户
   * @param profile 用户填写的画像；null 视为全部留空
   * @param complete 是否同时结束引导（用户点「跳过」时同样传 true）
   * @param expectedVersion 客户端持有的账号版本
   */
  OnboardingProfileResult saveOnboardingProfile(
      long userId, OnboardingProfile profile, boolean complete, long expectedVersion);

  /** 重新进入引导：只清完成标记，保留已填画像。 */
  OnboardingProfileResult reopenOnboarding(long userId, long expectedVersion);
}
