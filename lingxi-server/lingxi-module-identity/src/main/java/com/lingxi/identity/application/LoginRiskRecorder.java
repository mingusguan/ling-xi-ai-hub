package com.lingxi.identity.application;

import com.lingxi.kernel.IdGenerator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 在独立事务中保留登录风险事实，避免认证失败回滚时丢失风险记录。 */
@Service
public class LoginRiskRecorder {
  private final JdbcTemplate jdbc;private final IdGenerator ids;
  public LoginRiskRecorder(JdbcTemplate jdbc,IdGenerator ids){this.jdbc=jdbc;this.ids=ids;}
  @Transactional(propagation=Propagation.REQUIRES_NEW)
  public void record(long userId,String deviceId,String level,String type,String summary){LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);jdbc.update("INSERT INTO id_login_risk_event(id,user_id,device_id,risk_level,risk_type,summary,status,version,deleted,created_at,updated_at) VALUES(?,?,?,?,?,?,'OPEN',0,0,?,?)",ids.nextId(),userId,deviceId,level,type,summary,now,now);}
}
