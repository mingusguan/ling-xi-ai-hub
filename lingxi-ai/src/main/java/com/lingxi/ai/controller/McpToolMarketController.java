package com.lingxi.ai.controller;

import com.lingxi.ai.mcp.domain.request.McpToolApplyRequest;
import com.lingxi.ai.mcp.domain.request.McpToolApprovalRequest;
import com.lingxi.ai.mcp.domain.request.McpToolBindingRequest;
import com.lingxi.ai.mcp.domain.request.McpToolQueryRequest;
import com.lingxi.ai.mcp.domain.request.McpToolSaveRequest;
import com.lingxi.ai.mcp.service.IMcpToolMarketService;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.log.annotation.Log;
import com.lingxi.common.log.enums.BusinessType;
import com.lingxi.common.security.annotation.RequiresPermissions;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 工具市场控制器，提供工具发现、发布、权限申请、审计监控和编排复用管理接口。
 */
@Validated
@RestController
@RequestMapping("/ai/mcp/market")
public class McpToolMarketController extends BaseController {

    private final IMcpToolMarketService toolMarketService;

    public McpToolMarketController(IMcpToolMarketService toolMarketService) {
        this.toolMarketService = toolMarketService;
    }

    /**
     * 分页查询工具市场工具清单。
     *
     * @param query 查询条件
     * @return 工具分页列表
     */
    @RequiresPermissions("ai:mcp:market:list")
    @GetMapping("/tools")
    public TableDataInfo listTools(McpToolQueryRequest query) {
        startPage();
        return getDataTable(toolMarketService.listTools(query));
    }

    /**
     * 查询工具注册详情。
     *
     * @param toolId 工具注册主键
     * @return 工具详情
     */
    @RequiresPermissions("ai:mcp:market:list")
    @GetMapping("/tools/{toolId}")
    public AjaxResult getTool(@PathVariable Long toolId) {
        return AjaxResult.success(toolMarketService.getTool(toolId));
    }

    /**
     * 新增工具注册信息。
     *
     * @param request 工具保存请求
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:edit")
    @Log(title = "MCP工具发布", businessType = BusinessType.INSERT)
    @PostMapping("/tools")
    public AjaxResult addTool(@Valid @RequestBody McpToolSaveRequest request) {
        request.setToolId(null);
        return toAjax(toolMarketService.saveTool(request));
    }

    /**
     * 更新工具注册信息。
     *
     * @param request 工具保存请求
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:edit")
    @Log(title = "MCP工具编辑", businessType = BusinessType.UPDATE)
    @PutMapping("/tools")
    public AjaxResult editTool(@Valid @RequestBody McpToolSaveRequest request) {
        return toAjax(toolMarketService.saveTool(request));
    }

    /**
     * 发布工具。
     *
     * @param toolId 工具注册主键
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:edit")
    @Log(title = "MCP工具发布", businessType = BusinessType.UPDATE)
    @PutMapping("/tools/{toolId}/publish")
    public AjaxResult publishTool(@PathVariable Long toolId) {
        return toAjax(toolMarketService.publishTool(toolId));
    }

    /**
     * 停用工具。
     *
     * @param toolId 工具注册主键
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:edit")
    @Log(title = "MCP工具停用", businessType = BusinessType.UPDATE)
    @PutMapping("/tools/{toolId}/disable")
    public AjaxResult disableTool(@PathVariable Long toolId) {
        return toAjax(toolMarketService.disableTool(toolId));
    }

    /**
     * 废弃工具版本。
     *
     * @param toolId 工具注册主键
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:edit")
    @Log(title = "MCP工具废弃", businessType = BusinessType.UPDATE)
    @PutMapping("/tools/{toolId}/deprecate")
    public AjaxResult deprecateTool(@PathVariable Long toolId) {
        return toAjax(toolMarketService.deprecateTool(toolId));
    }

    /**
     * 提交工具权限申请。
     *
     * @param request 申请请求
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:apply")
    @Log(title = "MCP工具权限申请", businessType = BusinessType.INSERT)
    @PostMapping("/applications")
    public AjaxResult applyTool(@Valid @RequestBody McpToolApplyRequest request) {
        return toAjax(toolMarketService.applyTool(request));
    }

    /**
     * 查询当前用户可申请的工具授权范围。
     *
     * @return 授权范围选项
     */
    @RequiresPermissions("ai:mcp:market:apply")
    @GetMapping("/applications/grant-scopes")
    public AjaxResult grantScopes() {
        return AjaxResult.success(toolMarketService.listGrantScopeOptions());
    }

    /**
     * 分页查询权限申请列表。
     *
     * @param toolName 工具名称
     * @param status 申请状态
     * @return 权限申请分页列表
     */
    @RequiresPermissions("ai:mcp:market:approve")
    @GetMapping("/applications")
    public TableDataInfo listApplications(@RequestParam(required = false) String toolName,
            @RequestParam(required = false) String status) {
        startPage();
        return getDataTable(toolMarketService.listApplications(toolName, status));
    }

    /**
     * 通过工具权限申请。
     *
     * @param request 审批请求
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:approve")
    @Log(title = "MCP工具申请通过", businessType = BusinessType.UPDATE)
    @PutMapping("/applications/approve")
    public AjaxResult approveApplication(@Valid @RequestBody McpToolApprovalRequest request) {
        return toAjax(toolMarketService.approveApplication(request));
    }

    /**
     * 拒绝工具权限申请。
     *
     * @param request 审批请求
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:approve")
    @Log(title = "MCP工具申请拒绝", businessType = BusinessType.UPDATE)
    @PutMapping("/applications/reject")
    public AjaxResult rejectApplication(@Valid @RequestBody McpToolApprovalRequest request) {
        return toAjax(toolMarketService.rejectApplication(request));
    }

    /**
     * 分页查询工具调用审计日志。
     *
     * @param toolName 工具名称
     * @param status 调用状态
     * @param tenantId 租户 ID
     * @return 审计日志分页列表
     */
    @RequiresPermissions("ai:mcp:market:audit")
    @GetMapping("/audit-logs")
    public TableDataInfo listAuditLogs(@RequestParam(required = false) String toolName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tenantId) {
        startPage();
        return getDataTable(toolMarketService.listAuditLogs(toolName, status, tenantId));
    }

    /**
     * 查询工具市场统计。
     *
     * @return 统计数据
     */
    @RequiresPermissions("ai:mcp:market:audit")
    @GetMapping("/stats")
    public AjaxResult stats() {
        return AjaxResult.success(toolMarketService.getStats());
    }

    /**
     * 分页查询工具编排绑定列表。
     *
     * @param agentCode 助手或业务场景编码
     * @return 编排绑定分页列表
     */
    @RequiresPermissions("ai:mcp:market:bind")
    @GetMapping("/bindings")
    public TableDataInfo listBindings(@RequestParam(required = false) String agentCode) {
        startPage();
        return getDataTable(toolMarketService.listBindings(agentCode));
    }

    /**
     * 保存工具编排绑定。
     *
     * @param request 编排绑定请求
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:bind")
    @Log(title = "MCP工具编排绑定", businessType = BusinessType.UPDATE)
    @PostMapping("/bindings")
    public AjaxResult saveBinding(@Valid @RequestBody McpToolBindingRequest request) {
        return toAjax(toolMarketService.saveBinding(request));
    }

    /**
     * 删除工具编排绑定。
     *
     * @param bindingId 编排绑定主键
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:bind")
    @Log(title = "MCP工具编排解绑", businessType = BusinessType.DELETE)
    @DeleteMapping("/bindings/{bindingId}")
    public AjaxResult deleteBinding(@PathVariable Long bindingId) {
        return toAjax(toolMarketService.deleteBinding(bindingId));
    }

    /**
     * 刷新运行时 MCP Client 连接缓存。
     *
     * @return 操作结果
     */
    @RequiresPermissions("ai:mcp:market:edit")
    @Log(title = "MCP客户端连接刷新", businessType = BusinessType.UPDATE)
    @PutMapping("/clients/refresh")
    public AjaxResult refreshClients() {
        toolMarketService.refreshClientRegistry();
        return AjaxResult.success();
    }
}
