package com.lingxi.identity.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/** 身份聚合持久化端口。 */
public interface IdentityRepository {
  Optional<User> findById(long userId);

  Optional<User> findByRegistrationKey(String registrationKey);

  void insertRegistration(User user, AgeVerification ageVerification);

  boolean update(User user, long previousVersion);

  /** 批量将已满 18 周岁的青少年账号推进到显式成人迁移态。 */
  int markDueAdultTransitions(LocalDate dueDate, LocalDateTime now);

  boolean isLogicallyDeleted(long userId);

  void logicallyDeleteClosedUser(long userId, long requestId);
}
