package com.lingxi.ai.mcp.constant;

/**
 * MCP 工具市场状态码、授权范围和默认值常量。
 */
public final class McpMarketConstants {

    /** 工具草稿状态。 */
    public static final String TOOL_STATUS_DRAFT = "DRAFT";

    /** 工具已发布状态。 */
    public static final String TOOL_STATUS_PUBLISHED = "PUBLISHED";

    /** 工具已停用状态。 */
    public static final String TOOL_STATUS_DISABLED = "DISABLED";

    /** 工具已废弃状态。 */
    public static final String TOOL_STATUS_DEPRECATED = "DEPRECATED";

    /** 申请待审批状态。 */
    public static final String APPLY_STATUS_PENDING = "PENDING";

    /** 申请已通过状态。 */
    public static final String APPLY_STATUS_APPROVED = "APPROVED";

    /** 申请已拒绝状态。 */
    public static final String APPLY_STATUS_REJECTED = "REJECTED";

    /** 成功调用状态。 */
    public static final String CALL_STATUS_SUCCESS = "SUCCESS";

    /** 失败调用状态。 */
    public static final String CALL_STATUS_FAILURE = "FAILURE";

    /** 默认 MCP 协议标识。 */
    public static final String DEFAULT_PROTOCOL = "MCP_SSE";

    /** 小灵儿助手在工具编排绑定表中的业务场景编码。 */
    public static final String AGENT_CODE_XIAOLINGER = "xiaolinger";

    /** 用户级工具授权。 */
    public static final String GRANT_TYPE_USER = "USER";

    /** 部门级工具授权。 */
    public static final String GRANT_TYPE_DEPT = "DEPT";

    /** 租户级工具授权。 */
    public static final String GRANT_TYPE_TENANT = "TENANT";

    /** 生效中的授权。 */
    public static final String GRANT_STATUS_ACTIVE = "ACTIVE";

    /** 已撤销的授权。 */
    public static final String GRANT_STATUS_REVOKED = "REVOKED";

    /** 允许申请部门工具授权的权限码。 */
    public static final String PERMISSION_GRANT_DEPT = "ai:mcp:grant:dept";

    /** 允许申请租户工具授权的权限码。 */
    public static final String PERMISSION_GRANT_TENANT = "ai:mcp:grant:tenant";

    private McpMarketConstants() {
    }
}
