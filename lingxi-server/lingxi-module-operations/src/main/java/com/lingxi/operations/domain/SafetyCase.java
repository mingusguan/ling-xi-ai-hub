package com.lingxi.operations.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;

/** 安全事件聚合；正文不进入聚合，只保留最小处置摘要。 */
public class SafetyCase {
  public enum Status { OPEN, REVIEWING, ESCALATED, RESOLVED, CLOSED }
  private final long id; private final String caseNo,riskCategory,riskLevel;
  private Status status; private Long reviewerAdminId; private String resolution;
  private long version; private LocalDateTime updatedAt;
  private SafetyCase(long id,String no,String category,String level,Status status,Long reviewer,
      String resolution,long version,LocalDateTime updatedAt){this.id=id;caseNo=no;riskCategory=category;riskLevel=level;this.status=status;reviewerAdminId=reviewer;this.resolution=resolution;this.version=version;this.updatedAt=updatedAt;}
  public static SafetyCase rehydrate(long id,String no,String category,String level,String status,
      Long reviewer,String resolution,long version,LocalDateTime updatedAt){return new SafetyCase(id,no,category,level,Status.valueOf(status),reviewer,resolution,version,updatedAt);}
  public void transition(long admin,String target,String resolution,long expected,LocalDateTime now){
    if(version!=expected)throw new BusinessException("OPS_SAFETY_CASE_CONFLICT","安全事件已变化");
    Status next;try{next=Status.valueOf(target);}catch(Exception e){throw new BusinessException("OPS_SAFETY_STATUS_INVALID","安全事件状态不合法");}
    if(status==Status.CLOSED)throw new BusinessException("OPS_SAFETY_STATE_INVALID","已关闭事件不可修改");
    if((next==Status.RESOLVED||next==Status.CLOSED)&&(resolution==null||resolution.isBlank()))throw new BusinessException("OPS_SAFETY_RESOLUTION_REQUIRED","完成处置必须填写结论");
    status=next;reviewerAdminId=admin;this.resolution=resolution;version++;updatedAt=now;
  }
  public long getId(){return id;}public String getCaseNo(){return caseNo;}public String getRiskCategory(){return riskCategory;}public String getRiskLevel(){return riskLevel;}public Status getStatus(){return status;}public Long getReviewerAdminId(){return reviewerAdminId;}public String getResolution(){return resolution;}public long getVersion(){return version;}public LocalDateTime getUpdatedAt(){return updatedAt;}
}
