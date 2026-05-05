package com.lingxi.ai.mcp.service.impl;

import com.lingxi.ai.mapper.McpToolApplicationMapper;
import com.lingxi.ai.mapper.McpToolAuditLogMapper;
import com.lingxi.ai.mapper.McpToolBindingMapper;
import com.lingxi.ai.mapper.McpToolGrantMapper;
import com.lingxi.ai.mapper.McpToolRegistryMapper;
import com.lingxi.ai.mcp.constant.McpMarketConstants;
import com.lingxi.ai.mcp.domain.McpToolApplication;
import com.lingxi.ai.mcp.domain.McpToolAuditLog;
import com.lingxi.ai.mcp.domain.McpToolBinding;
import com.lingxi.ai.mcp.domain.McpToolGrant;
import com.lingxi.ai.mcp.domain.McpToolRegistry;
import com.lingxi.ai.mcp.domain.request.McpToolApplyRequest;
import com.lingxi.ai.mcp.domain.request.McpToolApprovalRequest;
import com.lingxi.ai.mcp.domain.request.McpToolBindingRequest;
import com.lingxi.ai.mcp.domain.request.McpToolQueryRequest;
import com.lingxi.ai.mcp.domain.request.McpToolSaveRequest;
import com.lingxi.ai.mcp.domain.vo.McpGrantScopeOptionVO;
import com.lingxi.ai.mcp.domain.vo.McpToolMarketStatsVO;
import com.lingxi.ai.mcp.service.IMcpToolMarketService;
import com.lingxi.ai.mcp.service.McpDynamicClientRegistry;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.utils.ServletUtils;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.security.auth.AuthUtil;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MCP 工具市场服务实现，集中处理工具发布、授权申请、审批生效、审计监控和复用编排规则。
 */
@Service
public class McpToolMarketServiceImpl implements IMcpToolMarketService {

    private static final String YES = "1";

    private final McpToolRegistryMapper registryMapper;

    private final McpToolApplicationMapper applicationMapper;

    private final McpToolAuditLogMapper auditLogMapper;

    private final McpToolBindingMapper bindingMapper;

    private final McpToolGrantMapper grantMapper;

    private final McpDynamicClientRegistry clientRegistry;

    public McpToolMarketServiceImpl(McpToolRegistryMapper registryMapper,
            McpToolApplicationMapper applicationMapper,
            McpToolAuditLogMapper auditLogMapper,
            McpToolBindingMapper bindingMapper,
            McpToolGrantMapper grantMapper,
            McpDynamicClientRegistry clientRegistry) {
        this.registryMapper = registryMapper;
        this.applicationMapper = applicationMapper;
        this.auditLogMapper = auditLogMapper;
        this.bindingMapper = bindingMapper;
        this.grantMapper = grantMapper;
        this.clientRegistry = clientRegistry;
    }

    @Override
    public List<McpToolRegistry> listTools(McpToolQueryRequest query) {
        McpToolQueryRequest safeQuery = query == null ? new McpToolQueryRequest() : query;
        if (StringUtils.isEmpty(safeQuery.getTenantId()) && ServletUtils.getRequest() != null) {
            safeQuery.setTenantId(ServletUtils.getRequest().getHeader("X-Tenant-Id"));
        }
        return registryMapper.selectRegistryList(safeQuery);
    }

    @Override
    public McpToolRegistry getTool(Long toolId) {
        McpToolRegistry registry = registryMapper.selectRegistryById(toolId);
        if (registry == null) {
            throw new ServiceException("工具不存在");
        }
        return registry;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveTool(McpToolSaveRequest request) {
        Date now = new Date();
        String username = SecurityUtils.getUsername();
        McpToolRegistry registry = buildRegistry(request);
        registry.setUpdateBy(username);
        registry.setUpdateTime(now);
        if (request.getToolId() == null) {
            assertToolNameAvailable(request.getToolName());
            registry.setStatus(StringUtils.defaultIfEmpty(request.getStatus(), McpMarketConstants.TOOL_STATUS_DRAFT));
            registry.setProtocol(McpMarketConstants.DEFAULT_PROTOCOL);
            registry.setAuditEnabled(StringUtils.defaultIfEmpty(request.getAuditEnabled(), YES));
            registry.setMarketplaceVisible(StringUtils.defaultIfEmpty(request.getMarketplaceVisible(), YES));
            registry.setApprovalRequired(StringUtils.defaultIfEmpty(request.getApprovalRequired(), "0"));
            registry.setCreateBy(username);
            registry.setCreateTime(now);
            int rows = registryMapper.insertRegistry(registry);
            clientRegistry.refresh();
            return rows;
        }
        registry.setToolId(request.getToolId());
        assertToolExists(request.getToolId());
        int rows = registryMapper.updateRegistry(registry);
        clientRegistry.refresh();
        return rows;
    }

    @Override
    public int publishTool(Long toolId) {
        McpToolRegistry registry = getTool(toolId);
        validatePublishReady(registry);
        int rows = registryMapper.updateRegistryStatus(toolId, McpMarketConstants.TOOL_STATUS_PUBLISHED,
                SecurityUtils.getUsername());
        clientRegistry.refresh();
        return rows;
    }

    @Override
    public int disableTool(Long toolId) {
        assertToolExists(toolId);
        int rows = registryMapper.updateRegistryStatus(toolId, McpMarketConstants.TOOL_STATUS_DISABLED,
                SecurityUtils.getUsername());
        clientRegistry.refresh();
        return rows;
    }

    @Override
    public int deprecateTool(Long toolId) {
        assertToolExists(toolId);
        int rows = registryMapper.updateRegistryStatus(toolId, McpMarketConstants.TOOL_STATUS_DEPRECATED,
                SecurityUtils.getUsername());
        clientRegistry.refresh();
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int applyTool(McpToolApplyRequest request) {
        McpToolRegistry registry = getTool(request.getToolId());
        if (!McpMarketConstants.TOOL_STATUS_PUBLISHED.equals(registry.getStatus())) {
            throw new ServiceException("只能申请已发布工具");
        }

        GrantTarget target = resolveGrantTarget(request.getGrantType());
        McpToolApplication pending = applicationMapper.selectPendingByToolAndTarget(request.getToolId(),
                target.grantType, target.grantTargetId);
        if (pending != null) {
            throw new ServiceException("该工具和授权范围已有待审批申请");
        }

        Date now = new Date();
        McpToolApplication application = new McpToolApplication();
        application.setToolId(registry.getToolId());
        application.setToolName(registry.getToolName());
        application.setApplicantUserId(SecurityUtils.getUserId());
        application.setApplicantName(SecurityUtils.getUsername());
        application.setApplicantDeptId(getCurrentDeptId());
        application.setTenantId(getCurrentTenantId());
        application.setGrantType(target.grantType);
        application.setGrantTargetId(target.grantTargetId);
        application.setGrantTargetName(target.grantTargetName);
        application.setPurpose(request.getPurpose());
        application.setStatus(McpMarketConstants.APPLY_STATUS_PENDING);
        application.setCreateTime(now);
        application.setUpdateTime(now);
        return applicationMapper.insertApplication(application);
    }

    @Override
    public List<McpGrantScopeOptionVO> listGrantScopeOptions() {
        List<McpGrantScopeOptionVO> options = new ArrayList<>();
        options.add(new McpGrantScopeOptionVO(McpMarketConstants.GRANT_TYPE_USER, "仅本人使用"));
        if (AuthUtil.hasPermi(McpMarketConstants.PERMISSION_GRANT_DEPT)
                || AuthUtil.hasPermi(McpMarketConstants.PERMISSION_GRANT_TENANT)) {
            options.add(new McpGrantScopeOptionVO(McpMarketConstants.GRANT_TYPE_DEPT, "本部门使用"));
        }
        if (AuthUtil.hasPermi(McpMarketConstants.PERMISSION_GRANT_TENANT)) {
            options.add(new McpGrantScopeOptionVO(McpMarketConstants.GRANT_TYPE_TENANT, "本租户使用"));
        }
        return options;
    }

    @Override
    public List<McpToolApplication> listApplications(String toolName, String status) {
        return applicationMapper.selectApplicationList(toolName, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveApplication(McpToolApprovalRequest request) {
        int rows = updateApplicationStatus(request, McpMarketConstants.APPLY_STATUS_APPROVED);
        McpToolApplication application = applicationMapper.selectApplicationById(request.getApplicationId());
        createOrUpdateGrant(application);
        return rows;
    }

    @Override
    public int rejectApplication(McpToolApprovalRequest request) {
        return updateApplicationStatus(request, McpMarketConstants.APPLY_STATUS_REJECTED);
    }

    @Override
    public List<McpToolAuditLog> listAuditLogs(String toolName, String status, String tenantId) {
        return auditLogMapper.selectAuditLogList(toolName, status, tenantId);
    }

    @Override
    public McpToolMarketStatsVO getStats() {
        McpToolMarketStatsVO stats = new McpToolMarketStatsVO();
        stats.setTotalTools(nvl(registryMapper.countToolsByStatus(null)));
        stats.setPublishedTools(nvl(registryMapper.countToolsByStatus(McpMarketConstants.TOOL_STATUS_PUBLISHED)));
        stats.setPendingApplications(nvl(applicationMapper.countApplicationsByStatus(McpMarketConstants.APPLY_STATUS_PENDING)));
        stats.setTodayCalls(nvl(auditLogMapper.countTodayCalls(null)));
        stats.setTodayFailures(nvl(auditLogMapper.countTodayCalls(McpMarketConstants.CALL_STATUS_FAILURE)));
        stats.setAverageCostMillis(nvl(auditLogMapper.selectTodayAverageCostMillis()));
        return stats;
    }

    @Override
    public List<McpToolBinding> listBindings(String agentCode) {
        return bindingMapper.selectBindingList(agentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveBinding(McpToolBindingRequest request) {
        McpToolRegistry registry = getTool(request.getToolId());
        if (!McpMarketConstants.TOOL_STATUS_PUBLISHED.equals(registry.getStatus())) {
            throw new ServiceException("只能编排已发布工具");
        }
        Date now = new Date();
        String username = SecurityUtils.getUsername();
        McpToolBinding binding = new McpToolBinding();
        binding.setAgentCode(request.getAgentCode());
        binding.setToolId(request.getToolId());
        binding.setToolName(registry.getToolName());
        binding.setEnabled(StringUtils.defaultIfEmpty(request.getEnabled(), YES));
        binding.setConfigJson(request.getConfigJson());
        binding.setUpdateBy(username);
        binding.setUpdateTime(now);
        if (request.getBindingId() != null) {
            binding.setBindingId(request.getBindingId());
            return bindingMapper.updateBinding(binding);
        }
        McpToolBinding existing = bindingMapper.selectBindingByAgentAndTool(request.getAgentCode(), request.getToolId());
        if (existing != null) {
            throw new ServiceException("该助手已绑定此工具");
        }
        binding.setCreateBy(username);
        binding.setCreateTime(now);
        return bindingMapper.insertBinding(binding);
    }

    @Override
    public int deleteBinding(Long bindingId) {
        McpToolBinding binding = bindingMapper.selectBindingById(bindingId);
        if (binding == null) {
            throw new ServiceException("工具绑定不存在");
        }
        return bindingMapper.deleteBindingById(bindingId);
    }

    @Override
    public boolean isToolEnabledForAgent(String agentCode, String toolName) {
        if (StringUtils.isEmpty(agentCode) || StringUtils.isEmpty(toolName)) {
            return false;
        }
        return bindingMapper.countEnabledBindingByAgentAndToolName(agentCode, toolName) > 0;
    }

    @Override
    public boolean hasGrantForCurrentUser(String toolName) {
        if (SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            return true;
        }
        Long userId = SecurityUtils.getUserId();
        if (userId != null && hasActiveGrant(toolName, McpMarketConstants.GRANT_TYPE_USER, String.valueOf(userId))) {
            return true;
        }
        Long deptId = getCurrentDeptId();
        if (deptId != null && hasActiveGrant(toolName, McpMarketConstants.GRANT_TYPE_DEPT, String.valueOf(deptId))) {
            return true;
        }
        String tenantId = getCurrentTenantId();
        return StringUtils.isNotBlank(tenantId)
                && hasActiveGrant(toolName, McpMarketConstants.GRANT_TYPE_TENANT, tenantId);
    }

    @Override
    public boolean isApprovalRequired(String toolName) {
        McpToolRegistry registry = registryMapper.selectRegistryByName(toolName);
        return registry != null && YES.equals(registry.getApprovalRequired());
    }

    @Override
    public void refreshClientRegistry() {
        clientRegistry.refresh();
    }

    private GrantTarget resolveGrantTarget(String grantType) {
        if (McpMarketConstants.GRANT_TYPE_USER.equals(grantType)) {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                throw new ServiceException("当前用户信息缺失，无法申请本人授权");
            }
            return new GrantTarget(grantType, String.valueOf(userId), SecurityUtils.getUsername());
        }
        if (McpMarketConstants.GRANT_TYPE_DEPT.equals(grantType)) {
            if (!AuthUtil.hasPermi(McpMarketConstants.PERMISSION_GRANT_DEPT)
                    && !AuthUtil.hasPermi(McpMarketConstants.PERMISSION_GRANT_TENANT)) {
                throw new ServiceException("当前用户无权申请部门授权");
            }
            Long deptId = getCurrentDeptId();
            if (deptId == null) {
                throw new ServiceException("当前用户部门信息缺失，无法申请部门授权");
            }
            return new GrantTarget(grantType, String.valueOf(deptId), "部门：" + deptId);
        }
        if (McpMarketConstants.GRANT_TYPE_TENANT.equals(grantType)) {
            if (!AuthUtil.hasPermi(McpMarketConstants.PERMISSION_GRANT_TENANT)) {
                throw new ServiceException("当前用户无权申请租户授权");
            }
            String tenantId = getCurrentTenantId();
            if (StringUtils.isBlank(tenantId)) {
                throw new ServiceException("当前租户信息缺失，无法申请租户授权");
            }
            return new GrantTarget(grantType, tenantId, "租户：" + tenantId);
        }
        throw new ServiceException("不支持的授权范围：" + grantType);
    }

    private void createOrUpdateGrant(McpToolApplication application) {
        if (application == null) {
            throw new ServiceException("申请记录不存在");
        }
        Date now = new Date();
        McpToolGrant grant = new McpToolGrant();
        grant.setToolId(application.getToolId());
        grant.setToolName(application.getToolName());
        grant.setGrantType(application.getGrantType());
        grant.setGrantTargetId(application.getGrantTargetId());
        grant.setGrantTargetName(application.getGrantTargetName());
        grant.setApplicationId(application.getApplicationId());
        grant.setStatus(McpMarketConstants.GRANT_STATUS_ACTIVE);
        grant.setCreateBy(SecurityUtils.getUsername());
        grant.setCreateTime(now);
        grant.setUpdateBy(SecurityUtils.getUsername());
        grant.setUpdateTime(now);
        grantMapper.upsertGrant(grant);
    }

    private boolean hasActiveGrant(String toolName, String grantType, String grantTargetId) {
        return grantMapper.countActiveGrant(toolName, grantType, grantTargetId) > 0;
    }

    private McpToolRegistry buildRegistry(McpToolSaveRequest request) {
        McpToolRegistry registry = new McpToolRegistry();
        registry.setToolName(request.getToolName());
        registry.setDisplayName(request.getDisplayName());
        registry.setDescription(request.getDescription());
        registry.setServerName(request.getServerName());
        registry.setServerUrl(request.getServerUrl());
        registry.setSseEndpoint(request.getSseEndpoint());
        registry.setCategory(request.getCategory());
        registry.setVersion(request.getVersion());
        registry.setOwnerTeam(request.getOwnerTeam());
        registry.setOwnerUserId(request.getOwnerUserId());
        registry.setRequiredPermission(request.getRequiredPermission());
        registry.setTenantIds(request.getTenantIds());
        registry.setDeptIds(request.getDeptIds());
        registry.setStatus(request.getStatus());
        registry.setAuditEnabled(request.getAuditEnabled());
        registry.setMarketplaceVisible(request.getMarketplaceVisible());
        registry.setApprovalRequired(request.getApprovalRequired());
        registry.setInputSchema(request.getInputSchema());
        registry.setOutputSchema(request.getOutputSchema());
        registry.setExampleUsage(request.getExampleUsage());
        registry.setTags(request.getTags());
        registry.setReleaseNotes(request.getReleaseNotes());
        return registry;
    }

    private void assertToolNameAvailable(String toolName) {
        if (registryMapper.selectRegistryByName(toolName) != null) {
            throw new ServiceException("工具名称已存在");
        }
    }

    private void assertToolExists(Long toolId) {
        if (registryMapper.selectRegistryById(toolId) == null) {
            throw new ServiceException("工具不存在");
        }
    }

    private void validatePublishReady(McpToolRegistry registry) {
        if (StringUtils.isEmpty(registry.getToolName()) || StringUtils.isEmpty(registry.getDisplayName())
                || StringUtils.isEmpty(registry.getServerName()) || StringUtils.isEmpty(registry.getServerUrl())
                || StringUtils.isEmpty(registry.getVersion())) {
            throw new ServiceException("工具名称、展示名称、服务名称、服务地址和版本号不能为空");
        }
    }

    private int updateApplicationStatus(McpToolApprovalRequest request, String status) {
        McpToolApplication application = applicationMapper.selectApplicationById(request.getApplicationId());
        if (application == null) {
            throw new ServiceException("申请记录不存在");
        }
        if (!McpMarketConstants.APPLY_STATUS_PENDING.equals(application.getStatus())) {
            throw new ServiceException("只能审批待处理申请");
        }
        application.setStatus(status);
        application.setApproverUserId(SecurityUtils.getUserId());
        application.setApproverName(SecurityUtils.getUsername());
        application.setApprovalComment(request.getApprovalComment());
        application.setApprovalTime(new Date());
        application.setUpdateTime(new Date());
        return applicationMapper.updateApplication(application);
    }

    private Long getCurrentDeptId() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return null;
        }
        SysUser sysUser = loginUser.getSysUser();
        return sysUser == null ? null : sysUser.getDeptId();
    }

    private String getCurrentTenantId() {
        return ServletUtils.getRequest() == null ? null : ServletUtils.getRequest().getHeader("X-Tenant-Id");
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 后端计算后的授权目标，避免信任前端传入目标 ID。
     */
    private static class GrantTarget {

        private final String grantType;

        private final String grantTargetId;

        private final String grantTargetName;

        private GrantTarget(String grantType, String grantTargetId, String grantTargetName) {
            this.grantType = grantType;
            this.grantTargetId = grantTargetId;
            this.grantTargetName = grantTargetName;
        }
    }
}
