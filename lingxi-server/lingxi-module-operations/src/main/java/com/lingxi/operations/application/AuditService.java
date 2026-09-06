package com.lingxi.operations.application;

import com.lingxi.kernel.IdGenerator;
import com.lingxi.operations.api.AuditContext;
import com.lingxi.operations.domain.OperationsRepository;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AuditService {
  private final OperationsRepository repo;
  private final IdGenerator ids;

  public AuditService(OperationsRepository r, IdGenerator i) {
    repo = r;
    ids = i;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void append(
      long admin,
      String action,
      String type,
      String id,
      AuditContext c,
      String before,
      String after,
      String result) {
    repo.appendAudit(
        ids.nextId(),
        admin,
        action,
        type,
        id,
        c.reason(),
        c.ticketNo(),
        before,
        after,
        result,
        c.requestId(),
        LocalDateTime.now(ZoneOffset.UTC));
  }

  /**
   * 记录已经失败并回滚的管理操作。
   *
   * <p>失败审计必须独立提交；成功审计只能使用 {@link #append} 与业务事实同事务提交。
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void appendFailure(
      long admin,String action,String type,String id,AuditContext context,String before) {
    repo.appendAudit(ids.nextId(),admin,action,type,id,context.reason(),context.ticketNo(),before,
        null,"FAILED",context.requestId(),LocalDateTime.now(ZoneOffset.UTC));
  }
}
