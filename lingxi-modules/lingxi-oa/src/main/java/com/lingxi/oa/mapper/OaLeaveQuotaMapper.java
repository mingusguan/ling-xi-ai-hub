package com.lingxi.oa.mapper;

import java.util.List;
import com.lingxi.oa.domain.OaLeaveQuota;
import org.apache.ibatis.annotations.Mapper;

/**
 * P0: 假期额度Mapper接口
 */
@Mapper
public interface OaLeaveQuotaMapper 
{
    /**
     * 查询假期额度
     */
    public OaLeaveQuota selectOaLeaveQuotaById(Long quotaId);

    /**
     * 查询用户某年假期额度
     */
    public OaLeaveQuota selectOaLeaveQuotaByUserAndYear(OaLeaveQuota oaLeaveQuota);

    /**
     * 查询假期额度列表
     */
    public List<OaLeaveQuota> selectOaLeaveQuotaList(OaLeaveQuota oaLeaveQuota);

    /**
     * 新增假期额度
     */
    public int insertOaLeaveQuota(OaLeaveQuota oaLeaveQuota);

    /**
     * 修改假期额度
     */
    public int updateOaLeaveQuota(OaLeaveQuota oaLeaveQuota);

    /**
     * 删除假期额度
     */
    public int deleteOaLeaveQuotaById(Long quotaId);

    /**
     * 批量删除假期额度
     */
    public int deleteOaLeaveQuotaByIds(Long[] quotaIds);

    /**
     * 批量插入或更新假期额度
     */
    public int batchUpsertOaLeaveQuota(List<OaLeaveQuota> quotaList);
}
