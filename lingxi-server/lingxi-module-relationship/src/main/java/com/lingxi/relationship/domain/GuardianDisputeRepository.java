package com.lingxi.relationship.domain;

import com.lingxi.kernel.PageResult;
import com.lingxi.relationship.api.GuardianDisputeFacade.DisputeSummary;
import java.time.LocalDateTime;

public interface GuardianDisputeRepository {
  RelationParties relationParties(long relationId);
  boolean hasOpenDispute(long relationId);
  DisputeSummary insert(long id,String no,long relationId,long teen,long guardian,String reason,LocalDateTime now);
  PageResult<DisputeSummary> list(String status,int page,int size);
  DisputeSummary resolve(long adminId,long id,String status,String resolution,long expected,LocalDateTime now);
  record RelationParties(long teenUserId,Long guardianUserId,String status){}
}
