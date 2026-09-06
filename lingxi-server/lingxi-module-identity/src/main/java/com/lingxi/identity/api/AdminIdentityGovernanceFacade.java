package com.lingxi.identity.api;

import com.lingxi.kernel.PageResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 用户详情、登录风险、年龄申诉和隐私请求的后台治理门面。 */
public interface AdminIdentityGovernanceFacade {
  UserDetail userDetail(long adminId,long userId);
  PageResult<LoginRiskSummary> loginRisks(long adminId,String status,String riskLevel,int page,int pageSize);
  LoginRiskSummary resolveLoginRisk(RiskResolutionCommand command);
  PageResult<AgeAppealSummary> ageAppeals(long adminId,String status,int page,int pageSize);
  AgeAppealSummary reviewAgeAppeal(AgeAppealReviewCommand command);
  PageResult<PrivacyRequestSummary> privacyRequests(long adminId,String status,int page,int pageSize);

  record OperationContext(String reason,String ticketNo,String requestId,boolean recentAuthentication){}
  record UserDetail(AdminUserFacade.AdminUserSummary user,List<DeviceSummary> devices,
      List<AgeVerificationSummary> ageVerifications){}
  record DeviceSummary(long id,String deviceId,LocalDateTime firstSeenAt,LocalDateTime lastSeenAt){}
  record AgeVerificationSummary(long id,String method,LocalDate verifiedBirthDate,String result,
      LocalDateTime verifiedAt){}
  record LoginRiskSummary(long id,long userId,String deviceId,String riskLevel,String riskType,
      String summary,String status,Long reviewerAdminId,String resolution,long version,
      LocalDateTime createdAt,LocalDateTime updatedAt){}
  record AgeAppealSummary(long id,String appealNo,long userId,LocalDate claimedBirthDate,
      String evidenceRef,String status,Long reviewerAdminId,String resolution,long version,
      LocalDateTime createdAt,LocalDateTime updatedAt){}
  record PrivacyRequestSummary(long id,long userId,String type,String status,int progress,
      LocalDateTime deadline,String resultReference,String lastError,long version,
      LocalDateTime createdAt,LocalDateTime updatedAt){}
  record RiskResolutionCommand(long operatorAdminId,long id,String status,String resolution,
      long expectedVersion,OperationContext context){}
  record AgeAppealReviewCommand(long operatorAdminId,long id,boolean approved,String resolution,
      long expectedVersion,OperationContext context){}
}
