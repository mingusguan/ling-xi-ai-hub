package com.lingxi.identity.application;

import com.lingxi.identity.domain.DeviceSession;
import com.lingxi.identity.domain.IdentitySecurityRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 在独立事务中持久化刷新令牌重放处置，防止随后抛出的异常回滚撤销结果。 */
@Service
public class SessionReplayRevoker {
  private final IdentitySecurityRepository repository;

  public SessionReplayRevoker(IdentitySecurityRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void revoke(String familyId, LocalDateTime now) {
    DeviceSession session = repository.findSessionByFamilyId(familyId).orElse(null);
    if (session == null) {
      return;
    }
    long previousVersion = session.getVersion();
    session.revoke("REFRESH_TOKEN_REPLAY", now);
    repository.updateSession(session, previousVersion);
  }
}
