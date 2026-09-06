package com.lingxi.relationship.domain;

import java.time.*;
import java.util.*;

/** 伙伴关系、授权、互动和分享的聚合仓储。 */
public interface PartnerRepository {
  Optional<PartnerRelation> findRelation(long id);

  Optional<PartnerRelation> findRelationByRequestKey(String key);

  Optional<PartnerRelation> findOpenRelation(long a, long b);

  void insertRelation(PartnerRelation relation);

  boolean updateRelation(PartnerRelation relation, long previousVersion);

  Optional<PartnerGrant> findGrant(long id);

  Optional<PartnerGrant> findGrant(long relationId, long goalId);

  List<PartnerGrant> findActiveGrants(long goalId);

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

  Optional<ShareLink> findShareByRequestKey(String key);

  Optional<ShareLink> findShareByTokenHash(String hash);

  void insertShare(ShareLink share);

  boolean updateShare(ShareLink share, long previousVersion);

  boolean consumeShare(long shareId, Instant now, LocalDateTime updatedAt);
}
