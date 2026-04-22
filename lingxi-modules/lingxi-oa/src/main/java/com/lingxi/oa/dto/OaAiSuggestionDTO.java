package com.lingxi.oa.dto;

import java.io.Serializable;
import java.util.List;

/**
 * AI审批建议响应DTO
 */
public class OaAiSuggestionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 建议ID */
    private Long suggestId;

    /** AI建议内容 */
    private String aiSuggestion;

    /** 风险等级（low/normal/high） */
    private String riskLevel;

    /** 风险点列表 */
    private List<String> riskPoints;

    /** 相似案例列表 */
    private List<OaSimilarCaseDTO> similarCases;

    /** 推荐模板列表 */
    private List<String> templates;

    public Long getSuggestId() {
        return suggestId;
    }

    public void setSuggestId(Long suggestId) {
        this.suggestId = suggestId;
    }

    public String getAiSuggestion() {
        return aiSuggestion;
    }

    public void setAiSuggestion(String aiSuggestion) {
        this.aiSuggestion = aiSuggestion;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getRiskPoints() {
        return riskPoints;
    }

    public void setRiskPoints(List<String> riskPoints) {
        this.riskPoints = riskPoints;
    }

    public List<OaSimilarCaseDTO> getSimilarCases() {
        return similarCases;
    }

    public void setSimilarCases(List<OaSimilarCaseDTO> similarCases) {
        this.similarCases = similarCases;
    }

    public List<String> getTemplates() {
        return templates;
    }

    public void setTemplates(List<String> templates) {
        this.templates = templates;
    }
}
