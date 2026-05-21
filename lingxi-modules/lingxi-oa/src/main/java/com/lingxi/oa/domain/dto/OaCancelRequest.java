package com.lingxi.oa.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * OA 取消申请请求。
 */
public class OaCancelRequest
{
    /**
     * 业务主键。
     */
    @NotBlank(message = "业务标识不能为空")
    private String businessKey;

    /**
     * 流程实例 ID。
     */
    @NotBlank(message = "流程实例ID不能为空")
    private String processInstanceId;

    public String getBusinessKey()
    {
        return businessKey;
    }

    public void setBusinessKey(String businessKey)
    {
        this.businessKey = businessKey;
    }

    public String getProcessInstanceId()
    {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId)
    {
        this.processInstanceId = processInstanceId;
    }
}
