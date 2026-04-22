package com.lingxi.oa.domain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OaTemplateField {

    private String code;

    private String label;

    private String type;

    private Boolean required;

    private String displayStage;

    private Boolean approveReadonly;

    private List<Map<String, String>> options;
}
