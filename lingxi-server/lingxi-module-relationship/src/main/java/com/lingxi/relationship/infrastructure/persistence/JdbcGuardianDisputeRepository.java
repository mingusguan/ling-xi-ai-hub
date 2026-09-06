package com.lingxi.relationship.infrastructure.persistence;

import com.lingxi.kernel.*;
import com.lingxi.relationship.api.GuardianDisputeFacade.DisputeSummary;
import com.lingxi.relationship.domain.GuardianDisputeRepository;
import java.sql.*;import java.time.LocalDateTime;import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Repository;

@Repository
public class JdbcGuardianDisputeRepository implements GuardianDisputeRepository {
  private final JdbcTemplate jdbc;public JdbcGuardianDisputeRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
  @Override public RelationParties relationParties(long id){return jdbc.query("SELECT teen_user_id,guardian_user_id,status FROM rel_guardian_relation WHERE id=? AND deleted=0",(r,n)->{long guardian=r.getLong("guardian_user_id");boolean empty=r.wasNull();return new RelationParties(r.getLong("teen_user_id"),empty?null:guardian,r.getString("status"));},id).stream().findFirst().orElse(null);}
  @Override public boolean hasOpenDispute(long relationId){Integer v=jdbc.queryForObject("SELECT COUNT(*) FROM rel_guardian_dispute WHERE relation_id=? AND status IN ('OPEN','REVIEWING') AND deleted=0",Integer.class,relationId);return v!=null&&v>0;}
  @Override public DisputeSummary insert(long id,String no,long relation,long teen,long guardian,String reason,LocalDateTime now){jdbc.update("INSERT INTO rel_guardian_dispute(id,dispute_no,relation_id,teen_user_id,guardian_user_id,reason,status,version,deleted,created_at,updated_at) VALUES(?,?,?,?,?,?,'OPEN',0,0,?,?)",id,no,relation,teen,guardian,reason,now,now);return find(id);}
  @Override public PageResult<DisputeSummary> list(String status,int page,int size){List<Object>a=new ArrayList<>();String w=" WHERE deleted=0";if(status!=null&&!status.isBlank()){w+=" AND status=?";a.add(status);}Long v=jdbc.queryForObject("SELECT COUNT(*) FROM rel_guardian_dispute"+w,Long.class,a.toArray());a.add(size);a.add((page-1)*size);List<DisputeSummary>items=jdbc.query("SELECT * FROM rel_guardian_dispute"+w+" ORDER BY created_at DESC LIMIT ? OFFSET ?",(r,n)->map(r),a.toArray());return new PageResult<>(items,v==null?0:v,page,size);}
  @Override public DisputeSummary resolve(long admin,long id,String status,String resolution,long expected,LocalDateTime now){int n=jdbc.update("UPDATE rel_guardian_dispute SET status=?,reviewer_admin_id=?,resolution=?,version=version+1,updated_at=? WHERE id=? AND version=? AND deleted=0",status,admin,resolution,now,id,expected);if(n!=1)throw new BusinessException("REL_GUARDIAN_DISPUTE_CONFLICT","监护争议已变化，请刷新后重试");return find(id);}
  private DisputeSummary find(long id){return jdbc.query("SELECT * FROM rel_guardian_dispute WHERE id=? AND deleted=0",(r,n)->map(r),id).stream().findFirst().orElseThrow();}
  private DisputeSummary map(ResultSet r)throws SQLException{long reviewer=r.getLong("reviewer_admin_id");boolean empty=r.wasNull();return new DisputeSummary(r.getLong("id"),r.getString("dispute_no"),r.getLong("relation_id"),r.getLong("teen_user_id"),r.getLong("guardian_user_id"),r.getString("reason"),r.getString("status"),empty?null:reviewer,r.getString("resolution"),r.getLong("version"),r.getObject("created_at",LocalDateTime.class),r.getObject("updated_at",LocalDateTime.class));}
}
