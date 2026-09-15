package com.lingxi.kernel;

/**
 * 伙伴对话相关偏好的提供方（由身份模块实现）。
 *
 * <p>实现必须如实返回用户填过的内容：没填就是 null / 空集合，由消费方决定兜底话术。
 * 在提供方这一层替用户假造默认选择，会让「用户明确选了温和」和「用户没选过」
 * 在数据上再也无法区分，后续想按真实偏好做统计或调优就没有依据了。
 */
public interface CompanionPreferenceProvider {

  /**
   * 读取某用户的对话偏好。
   *
   * @param userId 用户标识
   * @return 偏好；用户不存在或从未填过引导时返回 {@link CompanionPreference#empty()}
   */
  CompanionPreference findPreference(long userId);
}
