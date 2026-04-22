package com.lingxi.oa.service;

import com.lingxi.oa.domain.OaApprovalRecord;

import java.util.List;

public interface IOaApprovalRecordService {

    void saveRecord(OaApprovalRecord record);

    List<OaApprovalRecord> listByBusinessKey(String businessKey);

    List<OaApprovalRecord> listByProcessInstanceId(String processInstanceId);

    /**
     * 根据业务类型查询审批记录
     *
     * @param businessType 业务类型
     * @param limit 限制条数
     * @return 审批记录列表
     */
    List<OaApprovalRecord> listByBusinessType(String businessType, int limit);
}
