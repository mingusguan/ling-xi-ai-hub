package com.lingxi.operations.infrastructure.persistence;
import com.lingxi.operations.domain.*;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcSafetyCaseRepository implements SafetyCaseRepository{
  private final JdbcTemplate jdbc;public JdbcSafetyCaseRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
  public Optional<SafetyCase> find(long id){return jdbc.query("SELECT id,case_no,risk_category,risk_level,status,reviewer_admin_id,resolution,version,updated_at FROM ops_safety_case WHERE id=? AND deleted=0",(rs,row)->{long reviewer=rs.getLong("reviewer_admin_id");boolean noReviewer=rs.wasNull();return SafetyCase.rehydrate(rs.getLong("id"),rs.getString("case_no"),rs.getString("risk_category"),rs.getString("risk_level"),rs.getString("status"),noReviewer?null:reviewer,rs.getString("resolution"),rs.getLong("version"),rs.getObject("updated_at",LocalDateTime.class));},id).stream().findFirst();}
  public boolean update(SafetyCase c,long old){return jdbc.update("UPDATE ops_safety_case SET status=?,reviewer_admin_id=?,resolution=?,version=?,updated_at=? WHERE id=? AND version=? AND deleted=0",c.getStatus().name(),c.getReviewerAdminId(),c.getResolution(),c.getVersion(),c.getUpdatedAt(),c.getId(),old)==1;}
}
