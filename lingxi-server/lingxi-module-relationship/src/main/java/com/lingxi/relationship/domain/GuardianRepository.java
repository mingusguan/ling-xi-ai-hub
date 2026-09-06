package com.lingxi.relationship.domain;

import com.lingxi.relationship.api.GuardianPermissionType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 监护关系持久化端口。 */
public interface GuardianRepository {
  Optional<GuardianRelation> findById(long relationId);

  Optional<GuardianRelation> findByRequestKey(String requestKey);

  Optional<GuardianRelation> findByInvitationTokenHash(String tokenHash);

  void insert(GuardianRelation relation);

  boolean update(GuardianRelation relation, long previousVersion);

  long countActiveRelations(long teenUserId);

  void insertPermissions(
      long relationId, List<GuardianPermissionType> permissions, LocalDateTime now);

  List<GuardianPermissionType> findPermissions(long relationId);
}
