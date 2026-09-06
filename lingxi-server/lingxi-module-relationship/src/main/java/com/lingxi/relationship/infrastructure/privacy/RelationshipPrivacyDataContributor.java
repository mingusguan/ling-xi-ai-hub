package com.lingxi.relationship.infrastructure.privacy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.*;
import com.lingxi.relationship.infrastructure.persistence.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 统一处理监护、伙伴、分享、拉黑与举报关系中的用户隐私数据。 */
@Component
public class RelationshipPrivacyDataContributor implements PrivacyDataContributor {
  private final GuardianRelationMapper guardianMapper;
  private final GuardianPermissionMapper permissionMapper;
  private final PartnerRelationMapper partnerMapper;
  private final PartnerGrantMapper grantMapper;
  private final PartnerInteractionMapper interactionMapper;
  private final ShareLinkMapper shareMapper;
  private final BlockRelationMapper blockMapper;
  private final ReportMapper reportMapper;
  private final ObjectMapper objectMapper;

  public RelationshipPrivacyDataContributor(
      GuardianRelationMapper guardianMapper, GuardianPermissionMapper permissionMapper,
      PartnerRelationMapper partnerMapper, PartnerGrantMapper grantMapper,
      PartnerInteractionMapper interactionMapper, ShareLinkMapper shareMapper,
      BlockRelationMapper blockMapper, ReportMapper reportMapper, ObjectMapper objectMapper) {
    this.guardianMapper = guardianMapper;
    this.permissionMapper = permissionMapper;
    this.partnerMapper = partnerMapper;
    this.grantMapper = grantMapper;
    this.interactionMapper = interactionMapper;
    this.shareMapper = shareMapper;
    this.blockMapper = blockMapper;
    this.reportMapper = reportMapper;
    this.objectMapper = objectMapper;
  }

  @Override public String moduleName() { return "relationship"; }

  @Override
  @Transactional
  public PrivacyContribution process(PrivacyProcessingContext context) {
    long userId = context.userId();
    PrivacyRequestType type = context.type();
    if (type == PrivacyRequestType.EXPORT) {
      return PrivacyContribution.exported(export(userId));
    }
    if (type != PrivacyRequestType.DELETE_DATA && type != PrivacyRequestType.CLOSE_ACCOUNT) {
      return PrivacyContribution.unchanged();
    }
    List<Long> guardianIds = guardianMapper.selectList(guardianQuery(userId)).stream()
        .map(GuardianRelationEntity::getId).toList();
    List<Long> partnerIds = partnerMapper.selectList(partnerQuery(userId)).stream()
        .map(PartnerRelationEntity::getId).toList();
    int affectedRows = 0;
    if (!guardianIds.isEmpty()) {
      affectedRows += permissionMapper.delete(Wrappers.<GuardianPermissionEntity>lambdaQuery()
          .in(GuardianPermissionEntity::getRelationId, guardianIds));
      affectedRows += guardianMapper.deleteByIds(guardianIds);
    }
    if (!partnerIds.isEmpty()) {
      List<Long> grantIds = grantMapper.selectList(Wrappers.<PartnerGrantEntity>lambdaQuery()
              .select(PartnerGrantEntity::getId).in(PartnerGrantEntity::getRelationId, partnerIds))
          .stream().map(PartnerGrantEntity::getId).toList();
      affectedRows += interactionMapper.delete(Wrappers.<PartnerInteractionEntity>lambdaQuery()
          .in(PartnerInteractionEntity::getRelationId, partnerIds));
      if (!grantIds.isEmpty()) {
        affectedRows += grantMapper.deleteByIds(grantIds);
      }
      affectedRows += partnerMapper.deleteByIds(partnerIds);
    }
    affectedRows += shareMapper.delete(Wrappers.<ShareLinkEntity>lambdaQuery()
        .eq(ShareLinkEntity::getOwnerUserId, userId));
    affectedRows += blockMapper.delete(Wrappers.<BlockRelationEntity>lambdaQuery()
        .and(q -> q.eq(BlockRelationEntity::getBlockerUserId, userId)
            .or().eq(BlockRelationEntity::getBlockedUserId, userId)));
    affectedRows += reportMapper.delete(Wrappers.<ReportEntity>lambdaQuery()
        .eq(ReportEntity::getReporterUserId, userId));
    return PrivacyContribution.deleted(affectedRows);
  }

  private String export(long userId) {
    Map<String, Object> data = new LinkedHashMap<>();
    List<GuardianRelationEntity> guardianRelations = guardianMapper.selectList(guardianQuery(userId));
    List<Long> guardianIds = guardianRelations.stream().map(GuardianRelationEntity::getId).toList();
    List<PartnerRelationEntity> partnerRelations = partnerMapper.selectList(partnerQuery(userId));
    List<Long> partnerIds = partnerRelations.stream().map(PartnerRelationEntity::getId).toList();
    data.put("guardianRelations", guardianRelations);
    data.put("guardianPermissions", guardianIds.isEmpty() ? List.of() : permissionMapper.selectList(
        Wrappers.<GuardianPermissionEntity>lambdaQuery()
            .in(GuardianPermissionEntity::getRelationId, guardianIds)));
    data.put("partnerRelations", partnerRelations);
    data.put("partnerGrants", partnerIds.isEmpty() ? List.of() : grantMapper.selectList(
        Wrappers.<PartnerGrantEntity>lambdaQuery()
            .in(PartnerGrantEntity::getRelationId, partnerIds)));
    data.put("partnerInteractions", partnerIds.isEmpty() ? List.of() : interactionMapper.selectList(
        Wrappers.<PartnerInteractionEntity>lambdaQuery()
            .in(PartnerInteractionEntity::getRelationId, partnerIds)));
    data.put("shares", shareMapper.selectList(Wrappers.<ShareLinkEntity>lambdaQuery()
        .eq(ShareLinkEntity::getOwnerUserId, userId)));
    data.put("blocks", blockMapper.selectList(Wrappers.<BlockRelationEntity>lambdaQuery()
        .and(q -> q.eq(BlockRelationEntity::getBlockerUserId, userId)
            .or().eq(BlockRelationEntity::getBlockedUserId, userId))));
    data.put("reports", reportMapper.selectList(Wrappers.<ReportEntity>lambdaQuery()
        .eq(ReportEntity::getReporterUserId, userId)));
    try { return objectMapper.writeValueAsString(data); }
    catch (JsonProcessingException e) { throw new IllegalStateException("关系数据导出失败", e); }
  }

  private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GuardianRelationEntity>
      guardianQuery(long userId) {
    return Wrappers.<GuardianRelationEntity>lambdaQuery()
        .and(q -> q.eq(GuardianRelationEntity::getTeenUserId, userId)
            .or().eq(GuardianRelationEntity::getGuardianUserId, userId));
  }

  private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PartnerRelationEntity>
      partnerQuery(long userId) {
    return Wrappers.<PartnerRelationEntity>lambdaQuery()
        .and(q -> q.eq(PartnerRelationEntity::getInviterUserId, userId)
            .or().eq(PartnerRelationEntity::getInviteeUserId, userId));
  }
}
