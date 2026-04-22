package com.lingxi.oa.service;

import java.util.List;
import com.lingxi.oa.domain.OaLeaveAdjustment;

/**
 * P2: 假期额度调整记录Service接口
 */
public interface IOaLeaveAdjustmentService 
{
    public List<OaLeaveAdjustment> selectOaLeaveAdjustmentList(OaLeaveAdjustment oaLeaveAdjustment);
}
