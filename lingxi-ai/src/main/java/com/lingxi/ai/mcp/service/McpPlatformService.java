package com.lingxi.ai.mcp.service;

import com.lingxi.ai.mapper.McpToolAuditLogMapper;
import com.lingxi.ai.mapper.McpToolRegistryMapper;
import com.lingxi.ai.mcp.config.McpPlatformProperties;
import com.lingxi.ai.mcp.config.McpPlatformProperties.ToolDefinition;
import com.lingxi.ai.mcp.constant.McpMarketConstants;
import com.lingxi.ai.mcp.domain.McpToolAuditLog;
import com.lingxi.ai.mcp.domain.McpToolCallContext;
import com.lingxi.ai.mcp.domain.McpToolMarketplaceItem;
import com.lingxi.ai.mcp.domain.McpToolRegistry;
import com.lingxi.common.core.exception.auth.NotPermissionException;
import com.lingxi.common.core.utils.ServletUtils;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.security.auth.AuthUtil;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * MCP 平台治理服务。
 *
 * <p>统一处理 MCP 工具调用前的总开关、发布状态、权限、租户/部门隔离校验，以及调用后的审计记录。
 * 工具定义来自 MCP 工具市场数据库，配置文件只保留平台总开关和默认超时等基础参数。</p>
 */
@Service
public class McpPlatformService {

    private static final Logger auditLog = LoggerFactory.getLogger("MCP_TOOL_AUDIT");

    private final McpPlatformProperties properties;

    private final McpToolRegistryMapper registryMapper;

    private final McpToolAuditLogMapper auditLogMapper;

    public McpPlatformService(McpPlatformProperties properties, McpToolRegistryMapper registryMapper,
            McpToolAuditLogMapper auditLogMapper) {
        this.properties = properties;
        this.registryMapper = registryMapper;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * MCP 工具调用前置治理。
     *
     * @param toolName MCP 工具名称
     * @param arguments 调用入参，仅保留扩展位，当前不记录明细以避免敏感数据泄露
     * @return 已通过校验的工具定义
     */
    public ToolDefinition beforeCall(String toolName, Map<String, Object> arguments) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("MCP 平台已关闭。");
        }

        McpToolRegistry registry = registryMapper.selectRegistryByName(toolName);
        if (registry == null) {
            throw new IllegalStateException("MCP 工具未注册：" + toolName);
        }

        ToolDefinition definition = toToolDefinition(registry);
        if (!definition.isEnabled()) {
            throw new IllegalStateException("MCP 工具未发布或已停用：" + toolName);
        }

        checkPermission(definition);
        McpToolCallContext context = currentContext();
        checkTenant(definition.getTenantIds(), context, "MCP 工具", toolName);
        checkDept(parseCsv(registry.getDeptIds()), context, toolName);
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
            persistAudit(definition, toolName, costMillis, McpMarketConstants.CALL_STATUS_SUCCESS, null, context);
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
            persistAudit(definition, toolName, costMillis, McpMarketConstants.CALL_STATUS_FAILURE, e.getMessage(),
                    context);
        }
    }

    /**
     * 查询工具市场中可展示的 MCP 工具列表。
     *
     * @return 按分类和展示名称排序后的工具市场条目
     */
    public List<McpToolMarketplaceItem> listMarketplaceTools() {
        List<McpToolMarketplaceItem> items = new ArrayList<>();
        registryMapper.selectRegistryList(null).forEach(registry -> {
            ToolDefinition definition = toToolDefinition(registry);
            if (definition.isMarketplaceVisible()) {
                items.add(toMarketplaceItem(registry.getToolName(), definition));
            }
        });
        items.sort(Comparator.comparing(McpToolMarketplaceItem::getCategory, Comparator.nullsLast(String::compareTo))
                .thenComparing(McpToolMarketplaceItem::getDisplayName, Comparator.nullsLast(String::compareTo)));
        return items;
    }

    private ToolDefinition toToolDefinition(McpToolRegistry registry) {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StringUtils.defaultIfEmpty(registry.getToolName(), registry.getDisplayName()));
        definition.setDisplayName(registry.getDisplayName());
        definition.setDescription(registry.getDescription());
        definition.setServerName(registry.getServerName());
        definition.setEnabled(McpMarketConstants.TOOL_STATUS_PUBLISHED.equals(registry.getStatus()));
        definition.setVersion(registry.getVersion());
        definition.setCategory(registry.getCategory());
        definition.setOwnerTeam(registry.getOwnerTeam());
        definition.setRequiredPermission(registry.getRequiredPermission());
        definition.setTenantIds(parseCsv(registry.getTenantIds()));
        definition.setAuditEnabled(!"0".equals(registry.getAuditEnabled()));
        definition.setMarketplaceVisible(!"0".equals(registry.getMarketplaceVisible()));
        return definition;
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

    private void persistAudit(ToolDefinition definition, String toolName, long costMillis, String status,
            String errorMessage, McpToolCallContext context) {
        try {
            McpToolAuditLog log = new McpToolAuditLog();
            log.setToolName(toolName);
            log.setServerName(definition == null ? null : definition.getServerName());
            log.setVersion(definition == null ? null : definition.getVersion());
            log.setUserId(context.getUserId());
            log.setUsername(context.getUsername());
            log.setDeptId(context.getDeptId());
            log.setTenantId(context.getTenantId());
            log.setStatus(status);
            log.setCostMillis(costMillis);
            log.setErrorMessage(errorMessage);
            log.setCreateTime(new java.util.Date());
            auditLogMapper.insertAuditLog(log);
        } catch (Exception ex) {
            auditLog.warn("mcp_tool_call audit persistence failed tool={} status={} error={}", toolName, status,
                    ex.getMessage());
        }
    }

    private void checkPermission(ToolDefinition definition) {
        if (StringUtils.isBlank(definition.getRequiredPermission())) {
            return;
        }
        if (!AuthUtil.hasPermi(definition.getRequiredPermission())) {
            throw new NotPermissionException(definition.getRequiredPermission());
        }
    }

    private void checkTenant(List<String> tenantIds, McpToolCallContext context, String resourceType,
            String resourceName) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return;
        }
        if (StringUtils.isBlank(context.getTenantId()) || !tenantIds.contains(context.getTenantId())) {
            throw new IllegalStateException("当前租户无权访问" + resourceType + "：" + resourceName);
        }
    }

    private void checkDept(List<String> deptIds, McpToolCallContext context, String toolName) {
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }
        String deptId = context.getDeptId() == null ? null : String.valueOf(context.getDeptId());
        if (StringUtils.isBlank(deptId) || !deptIds.contains(deptId)) {
            throw new IllegalStateException("当前部门无权访问 MCP 工具：" + toolName);
        }
    }

    private List<String> parseCsv(String csv) {
        if (StringUtils.isBlank(csv)) {
            return new ArrayList<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
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
