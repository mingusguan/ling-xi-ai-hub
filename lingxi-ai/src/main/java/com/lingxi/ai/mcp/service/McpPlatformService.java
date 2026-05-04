package com.lingxi.ai.mcp.service;

import com.lingxi.ai.mcp.config.McpPlatformProperties;
import com.lingxi.ai.mcp.config.McpPlatformProperties.ServerDefinition;
import com.lingxi.ai.mcp.config.McpPlatformProperties.ToolDefinition;
import com.lingxi.ai.mcp.domain.McpToolCallContext;
import com.lingxi.ai.mcp.domain.McpToolMarketplaceItem;
import com.lingxi.common.core.exception.auth.NotPermissionException;
import com.lingxi.common.core.utils.ServletUtils;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.security.auth.AuthUtil;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * MCP 平台治理服务。
 *
 * <p>统一处理 MCP 工具调用前的开关、权限、租户校验，以及调用后的审计记录和工具市场清单转换。
 * 该服务不直接持有 MCP Client，避免和底层通信实现耦合。</p>
 */
@Service
public class McpPlatformService {

    private static final Logger auditLog = LoggerFactory.getLogger("MCP_TOOL_AUDIT");

    private final McpPlatformProperties properties;

    public McpPlatformService(McpPlatformProperties properties) {
        this.properties = properties;
    }

    /**
     * MCP 工具调用前置治理。
     *
     * @param toolName MCP 工具名称
     * @param arguments 调用入参，仅用于审计扩展和后续策略判断，当前不记录明细以避免敏感数据泄露
     * @return 已通过校验的工具定义
     */
    public ToolDefinition beforeCall(String toolName, Map<String, Object> arguments) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("MCP 平台已关闭。");
        }

        ToolDefinition definition = properties.getTool(toolName);
        if (definition == null) {
            throw new IllegalStateException("MCP 工具未注册：" + toolName);
        }
        if (!definition.isEnabled()) {
            throw new IllegalStateException("MCP 工具已关闭：" + toolName);
        }

        ServerDefinition server = properties.getServer(definition.getServerName());
        if (server == null) {
            throw new IllegalStateException("MCP Server 未注册：" + definition.getServerName());
        }
        if (!server.isEnabled()) {
            throw new IllegalStateException("MCP Server 已关闭：" + definition.getServerName());
        }

        checkPermission(definition);
        McpToolCallContext context = currentContext();
        checkTenant(server.getTenantIds(), context, "MCP Server", server.getName());
        checkTenant(definition.getTenantIds(), context, "MCP 工具", toolName);
        return definition;
    }

    /**
     * 记录 MCP 工具调用成功审计。
     *
     * @param definition 工具定义
     * @param toolName MCP 工具名称
     * @param costMillis 调用耗时，单位毫秒
     */
    public void auditSuccess(ToolDefinition definition, String toolName, long costMillis) {
        if (definition != null && definition.isAuditEnabled()) {
            McpToolCallContext context = currentContext();
            auditLog.info("mcp_tool_call status=success tool={} server={} version={} userId={} username={} deptId={} tenantId={} costMillis={}",
                    toolName,
                    definition.getServerName(),
                    definition.getVersion(),
                    context.getUserId(),
                    context.getUsername(),
                    context.getDeptId(),
                    context.getTenantId(),
                    costMillis);
        }
    }

    /**
     * 记录 MCP 工具调用失败审计。
     *
     * @param definition 工具定义；工具未注册时可能为空
     * @param toolName MCP 工具名称
     * @param costMillis 调用耗时，单位毫秒
     * @param e 失败异常
     */
    public void auditFailure(ToolDefinition definition, String toolName, long costMillis, Exception e) {
        boolean auditEnabled = definition == null || definition.isAuditEnabled();
        if (auditEnabled) {
            McpToolCallContext context = currentContext();
            auditLog.warn("mcp_tool_call status=failure tool={} server={} version={} userId={} username={} deptId={} tenantId={} costMillis={} error={}",
                    toolName,
                    definition == null ? null : definition.getServerName(),
                    definition == null ? null : definition.getVersion(),
                    context.getUserId(),
                    context.getUsername(),
                    context.getDeptId(),
                    context.getTenantId(),
                    costMillis,
                    e.getMessage());
        }
    }

    /**
     * 查询工具市场中可展示的 MCP 工具列表。
     *
     * @return 按分类和展示名称排序后的工具市场条目
     */
    public List<McpToolMarketplaceItem> listMarketplaceTools() {
        List<McpToolMarketplaceItem> items = new ArrayList<>();
        if (properties.getTools() == null) {
            return items;
        }

        properties.getTools().forEach((toolName, definition) -> {
            if (definition.isMarketplaceVisible()) {
                items.add(toMarketplaceItem(toolName, definition));
            }
        });
        items.sort(Comparator.comparing(McpToolMarketplaceItem::getCategory, Comparator.nullsLast(String::compareTo))
                .thenComparing(McpToolMarketplaceItem::getDisplayName, Comparator.nullsLast(String::compareTo)));
        return items;
    }

    private McpToolMarketplaceItem toMarketplaceItem(String toolName, ToolDefinition definition) {
        McpToolMarketplaceItem item = new McpToolMarketplaceItem();
        item.setName(StringUtils.isBlank(definition.getName()) ? toolName : definition.getName());
        item.setDisplayName(definition.getDisplayName());
        item.setDescription(definition.getDescription());
        item.setServerName(definition.getServerName());
        item.setVersion(definition.getVersion());
        item.setCategory(definition.getCategory());
        item.setOwnerTeam(definition.getOwnerTeam());
        item.setRequiredPermission(definition.getRequiredPermission());
        item.setEnabled(definition.isEnabled());
        item.setAuditEnabled(definition.isAuditEnabled());
        item.setTenantIds(definition.getTenantIds());
        return item;
    }

    private void checkPermission(ToolDefinition definition) {
        if (StringUtils.isBlank(definition.getRequiredPermission())) {
            return;
        }
        if (!AuthUtil.hasPermi(definition.getRequiredPermission())) {
            throw new NotPermissionException(definition.getRequiredPermission());
        }
    }

    private void checkTenant(List<String> tenantIds, McpToolCallContext context, String resourceType, String resourceName) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return;
        }
        if (StringUtils.isBlank(context.getTenantId()) || !tenantIds.contains(context.getTenantId())) {
            throw new IllegalStateException("当前租户无权访问" + resourceType + "：" + resourceName);
        }
    }

    private McpToolCallContext currentContext() {
        McpToolCallContext context = new McpToolCallContext();
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null) {
                context.setUserId(loginUser.getUserid());
                context.setUsername(loginUser.getUsername());
                SysUser sysUser = loginUser.getSysUser();
                if (sysUser != null) {
                    context.setDeptId(sysUser.getDeptId());
                    if (context.getUserId() == null) {
                        context.setUserId(sysUser.getUserId());
                    }
                    if (StringUtils.isBlank(context.getUsername())) {
                        context.setUsername(sysUser.getUserName());
                    }
                }
            }
        } catch (Exception ignored) {
            // Tool calls may also happen outside an HTTP login context.
        }

        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            context.setTenantId(request.getHeader("X-Tenant-Id"));
        }
        return context;
    }
}
