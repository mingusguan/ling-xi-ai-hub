package com.lingxi.relationship.domain;

import java.time.*;
import java.util.*;

/** 伙伴关系、授权、互动和分享的聚合仓储。 */
public interface PartnerRepository {
  Optional<PartnerRelation> findRelation(long id);

  Optional<PartnerRelation> findRelationByRequestKey(String key);

  Optional<PartnerRelation> findOpenRelation(long a, long b);

  /** 统计与指定用户相关的伙伴关系总数（作为邀请方或被邀请方）。 */
  long countRelationsByParticipant(long userId);

  /** 按创建时间倒序分页查询与指定用户相关的伙伴关系。 */
  List<PartnerRelation> findRelationsByParticipant(long userId, int page, int pageSize);

  void insertRelation(PartnerRelation relation);

  boolean updateRelation(PartnerRelation relation, long previousVersion);

  Optional<PartnerGrant> findGrant(long id);

  Optional<PartnerGrant> findGrant(long relationId, long goalId);

  List<PartnerGrant> findActiveGrants(long goalId);

  /** 查询指定用户作为授权方发出的全部目标授权（含已过期与已撤销，由客户端按状态展示）。 */
  List<PartnerGrant> findGrantsByOwner(long ownerUserId);

  void insertGrant(PartnerGrant grant);

  boolean updateGrant(PartnerGrant grant, long previousVersion);

  void insertInteraction(
      long id,
      long relationId,
      long grantId,
      long actor,
      String type,
      String resourceId,
      String contentJson,
      LocalDateTime now);

  void insertBlock(long id, long blocker, long blocked, String reason, LocalDateTime now);

  void insertReport(
      long id,
      long reporter,
      String targetType,
      String targetId,
      String reason,
      String evidence,
      LocalDateTime now);

  Optional<ShareLink> findShare(long id);

  /** 统计指定用户创建的分享链接总数。 */
  long countSharesByOwner(long ownerUserId);

  /** 按创建时间倒序分页查询指定用户创建的分享链接。 */
  List<ShareLink> findSharesByOwner(long ownerUserId, int page, int pageSize);

  Optional<ShareLink> findShareByRequestKey(String key);

  Optional<ShareLink> findShareByTokenHash(String hash);

  void insertShare(ShareLink share);

  boolean updateShare(ShareLink share, long previousVersion);

  boolean consumeShare(long shareId, Instant now, LocalDateTime updatedAt);
}
