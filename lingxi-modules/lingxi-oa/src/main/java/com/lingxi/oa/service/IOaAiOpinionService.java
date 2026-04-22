package com.lingxi.oa.service;


import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.oa.dto.OaBusinessFormDataDTO;
import com.lingxi.oa.dto.OaSimilarCaseDTO;

import java.util.List;

public interface IOaAiOpinionService {
    /**
     * 生成智能审批意见建议
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param taskId 任务ID
     * @param approverId 审批人ID
     * @return 审批建议结果
     */
    AjaxResult generateSuggestion(String businessType, Long businessId, String taskId, Long approverId);

    /**
     * 查询AI建议状态
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return AI建议结果
     */
    AjaxResult getSuggestionStatus(String businessType, Long businessId);

    /**
     * 获取相似案例
     * 根据业务类型查询历史已审批的相似案例
     *
     * @param businessType 业务类型
     * @param formData 表单数据
     * @return 相似案例列表
     */
    List<OaSimilarCaseDTO> getSimilarCases(String businessType, OaBusinessFormDataDTO formData);

    /**
     * 分析风险
     *
     * @param businessType 业务类型
     * @param formData 表单数据
     * @return 风险分析结果
     */
    AjaxResult analyzeRisk(String businessType, OaBusinessFormDataDTO formData);
}
