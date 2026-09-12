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

  /** 统计与指定用户相关的监护关系数量（作为青少年或作为监护人）。 */
  long countRelationsByParticipant(long userId);

  /** 按创建时间倒序分页查询与指定用户相关的监护关系。 */
  List<GuardianRelation> findRelationsByParticipant(long userId, int page, int pageSize);

  void insertPermissions(
      long relationId, List<GuardianPermissionType> permissions, LocalDateTime now);

  List<GuardianPermissionType> findPermissions(long relationId);
}
