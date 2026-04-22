package com.lingxi.oa.service;

import java.util.List;
import com.lingxi.oa.domain.OaLeaveRule;

/**
 * P1: 假期规则Service接口
 */
public interface IOaLeaveRuleService 
{
    public OaLeaveRule selectOaLeaveRuleById(Long ruleId);

    public List<OaLeaveRule> selectOaLeaveRuleList(OaLeaveRule oaLeaveRule);

    public int insertOaLeaveRule(OaLeaveRule oaLeaveRule);

    public int updateOaLeaveRule(OaLeaveRule oaLeaveRule);

    public int deleteOaLeaveRuleByIds(Long[] ruleIds);
}
