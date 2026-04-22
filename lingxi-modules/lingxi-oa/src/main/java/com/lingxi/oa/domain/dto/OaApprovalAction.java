package com.lingxi.oa.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class OaApprovalAction {

    private String taskId;

    private String businessKey;

    private String processInstanceId;

    private String comment;

    private String action;

    private Map<String, Object> variables;
}
