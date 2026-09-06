package com.lingxi.identity.domain;

import com.lingxi.identity.api.AdminIdentityGovernanceFacade.*;
import com.lingxi.identity.api.AdminUserFacade;
import com.lingxi.kernel.PageResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 身份治理后台仓储。 */
public interface IdentityGovernanceRepository {
  AdminUserFacade.AdminUserSummary user(long userId);
  List<DeviceSummary> devices(long userId);
  List<AgeVerificationSummary> ageVerifications(long userId);
  PageResult<LoginRiskSummary> loginRisks(String status,String riskLevel,int page,int size);
  LoginRiskSummary resolveLoginRisk(long adminId,long id,String status,String resolution,long expected,LocalDateTime now);
  PageResult<AgeAppealSummary> ageAppeals(String status,int page,int size);
  AgeAppealSummary findAgeAppeal(long id);
  boolean hasPendingAgeAppeal(long userId);
  AgeAppealSummary insertAgeAppeal(long id,String appealNo,long userId,LocalDate claimedBirthDate,
      String evidenceRef,LocalDateTime now);
  AgeAppealSummary reviewAgeAppeal(long adminId,long verificationId,long id,boolean approved,
      String resolution,long expected,LocalDateTime now);
  PageResult<PrivacyRequestSummary> privacyRequests(String status,int page,int size);
}
