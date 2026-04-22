package com.lingxi.oa.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lingxi.oa.mapper.OaLeaveAdjustmentMapper;
import com.lingxi.oa.domain.OaLeaveAdjustment;
import com.lingxi.oa.service.IOaLeaveAdjustmentService;

/**
 * P2: 假期额度调整记录Service业务层处理
 */
@Service
public class OaLeaveAdjustmentServiceImpl implements IOaLeaveAdjustmentService 
{
    @Autowired
    private OaLeaveAdjustmentMapper leaveAdjustmentMapper;

    @Override
    public List<OaLeaveAdjustment> selectOaLeaveAdjustmentList(OaLeaveAdjustment oaLeaveAdjustment)
    {
        return leaveAdjustmentMapper.selectOaLeaveAdjustmentList(oaLeaveAdjustment);
    }
}
