package com.lingxi.oa.mapper;

import java.util.List;
import com.lingxi.oa.domain.OaLeaveRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * P1: 假期规则Mapper接口
 */
@Mapper
public interface OaLeaveRuleMapper 
{
    public OaLeaveRule selectOaLeaveRuleById(Long ruleId);

    public OaLeaveRule selectOaLeaveRuleByType(String leaveType);

    public List<OaLeaveRule> selectOaLeaveRuleList(OaLeaveRule oaLeaveRule);

    public int insertOaLeaveRule(OaLeaveRule oaLeaveRule);

    public int updateOaLeaveRule(OaLeaveRule oaLeaveRule);

    public int deleteOaLeaveRuleById(Long ruleId);

    public int deleteOaLeaveRuleByIds(Long[] ruleIds);
}
