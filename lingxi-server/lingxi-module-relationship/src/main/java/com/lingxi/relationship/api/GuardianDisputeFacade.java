package com.lingxi.relationship.api;

import com.lingxi.kernel.PageResult;
import java.time.LocalDateTime;

/** 监护关系争议的用户提交与后台处置门面。 */
public interface GuardianDisputeFacade {
  DisputeSummary submit(long userId,SubmitDisputeCommand command);
  PageResult<DisputeSummary> list(long adminId,String status,int page,int pageSize);
  DisputeSummary resolve(ResolveDisputeCommand command);
  record OperationContext(String reason,String ticketNo,String requestId,boolean recentAuthentication){}
  record SubmitDisputeCommand(long relationId,String reason){}
  record ResolveDisputeCommand(long adminId,long id,String status,String resolution,long expectedVersion,
      OperationContext context){}
  record DisputeSummary(long id,String disputeNo,long relationId,long teenUserId,long guardianUserId,
      String reason,String status,Long reviewerAdminId,String resolution,long version,
      LocalDateTime createdAt,LocalDateTime updatedAt){}
}
