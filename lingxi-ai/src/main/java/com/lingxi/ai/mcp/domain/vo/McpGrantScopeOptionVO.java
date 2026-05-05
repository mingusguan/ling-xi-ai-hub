package com.lingxi.ai.mcp.domain.vo;

/**
 * MCP 工具授权申请范围选项，供前端按当前用户权限展示可申请范围。
 */
public class McpGrantScopeOptionVO {

    /** 授权范围类型：USER、DEPT、TENANT。 */
    private String grantType;

    /** 授权范围显示名称。 */
    private String label;

    public McpGrantScopeOptionVO() {
    }

    public McpGrantScopeOptionVO(String grantType, String label) {
        this.grantType = grantType;
        this.label = label;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
