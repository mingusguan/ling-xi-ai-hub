package com.lingxi.oa.mapper;

import java.util.List;
import com.lingxi.oa.domain.OaLeaveAdjustment;
import org.apache.ibatis.annotations.Mapper;

/**
 * P2: 假期额度调整记录Mapper接口
 */
@Mapper
public interface OaLeaveAdjustmentMapper 
{
    public OaLeaveAdjustment selectOaLeaveAdjustmentById(Long adjustmentId);

    public List<OaLeaveAdjustment> selectOaLeaveAdjustmentList(OaLeaveAdjustment oaLeaveAdjustment);

    public int insertOaLeaveAdjustment(OaLeaveAdjustment oaLeaveAdjustment);

    public int deleteOaLeaveAdjustmentByIds(Long[] adjustmentIds);
}
