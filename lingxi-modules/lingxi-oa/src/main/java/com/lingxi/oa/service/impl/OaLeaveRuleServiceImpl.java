package com.lingxi.oa.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lingxi.oa.mapper.OaLeaveRuleMapper;
import com.lingxi.oa.domain.OaLeaveRule;
import com.lingxi.oa.service.IOaLeaveRuleService;

/**
 * P1: 假期规则Service业务层处理
 */
@Service
public class OaLeaveRuleServiceImpl implements IOaLeaveRuleService 
{
    @Autowired
    private OaLeaveRuleMapper leaveRuleMapper;

    @Override
    public OaLeaveRule selectOaLeaveRuleById(Long ruleId)
    {
        return leaveRuleMapper.selectOaLeaveRuleById(ruleId);
    }

    @Override
    public List<OaLeaveRule> selectOaLeaveRuleList(OaLeaveRule oaLeaveRule)
    {
        return leaveRuleMapper.selectOaLeaveRuleList(oaLeaveRule);
    }

    @Override
    public int insertOaLeaveRule(OaLeaveRule oaLeaveRule)
    {
        return leaveRuleMapper.insertOaLeaveRule(oaLeaveRule);
    }

    @Override
    public int updateOaLeaveRule(OaLeaveRule oaLeaveRule)
    {
        return leaveRuleMapper.updateOaLeaveRule(oaLeaveRule);
    }

    @Override
    public int deleteOaLeaveRuleByIds(Long[] ruleIds)
    {
        return leaveRuleMapper.deleteOaLeaveRuleByIds(ruleIds);
    }
}
