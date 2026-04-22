package com.lingxi.oa.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OaTemplateMeta {

    private Long templateId;

    private String businessType;

    private String templateName;

    private String processDefinitionKey;

    private String processDefinitionName;

    private String processVersion;

    private String formRoute;

    private List<OaTemplateField> formFields = new ArrayList<>();
}
