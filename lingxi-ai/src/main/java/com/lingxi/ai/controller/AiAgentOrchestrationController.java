package com.lingxi.ai.controller;

import com.lingxi.ai.agent.domain.dto.AiAgentSaveRequest;
import com.lingxi.ai.agent.domain.dto.AiAgentStepSaveRequest;
import com.lingxi.ai.agent.service.IAiAgentOrchestrationService;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.log.annotation.Log;
import com.lingxi.common.log.enums.BusinessType;
import com.lingxi.common.security.annotation.RequiresPermissions;
import jakarta.validation.Valid;
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
 * 智能体编排控制器。
 */
@RestController
@RequestMapping("/ai/agent")
public class AiAgentOrchestrationController extends BaseController {

    private final IAiAgentOrchestrationService agentService;

    public AiAgentOrchestrationController(IAiAgentOrchestrationService agentService) {
        this.agentService = agentService;
    }

    /**
     * 分页查询智能体列表。
     */
    @RequiresPermissions("ai:agent:list")
    @GetMapping("/list")
    public TableDataInfo listAgents(@RequestParam(required = false) String agentCode,
            @RequestParam(required = false) String agentName,
            @RequestParam(required = false) String status) {
        startPage();
        return getDataTable(agentService.listAgents(agentCode, agentName, status));
    }

    /**
     * 查询智能体详情。
     */
    @RequiresPermissions("ai:agent:list")
    @GetMapping("/{agentId}")
    public AjaxResult detail(@PathVariable Long agentId) {
        return AjaxResult.success(agentService.getAgentDetail(agentId));
    }

    /**
     * 新增智能体。
     */
    @RequiresPermissions("ai:agent:edit")
    @Log(title = "AI智能体新增", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult addAgent(@Valid @RequestBody AiAgentSaveRequest request) {
        request.setAgentId(null);
        return toAjax(agentService.saveAgent(request));
    }

    /**
     * 更新智能体。
     */
    @RequiresPermissions("ai:agent:edit")
    @Log(title = "AI智能体编辑", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult editAgent(@Valid @RequestBody AiAgentSaveRequest request) {
        return toAjax(agentService.saveAgent(request));
    }

    /**
     * 发布智能体。
     */
    @RequiresPermissions("ai:agent:edit")
    @Log(title = "AI智能体发布", businessType = BusinessType.UPDATE)
    @PutMapping("/{agentId}/publish")
    public AjaxResult publishAgent(@PathVariable Long agentId) {
        return toAjax(agentService.publishAgent(agentId));
    }

    /**
     * 停用智能体。
     */
    @RequiresPermissions("ai:agent:edit")
    @Log(title = "AI智能体停用", businessType = BusinessType.UPDATE)
    @PutMapping("/{agentId}/disable")
    public AjaxResult disableAgent(@PathVariable Long agentId) {
        return toAjax(agentService.disableAgent(agentId));
    }

    /**
     * 查询智能体步骤。
     */
    @RequiresPermissions("ai:agent:list")
    @GetMapping("/steps")
    public TableDataInfo listSteps(@RequestParam String agentCode) {
        startPage();
        return getDataTable(agentService.listSteps(agentCode));
    }

    /**
     * 保存智能体步骤。
     */
    @RequiresPermissions("ai:agent:edit")
    @Log(title = "AI智能体步骤保存", businessType = BusinessType.UPDATE)
    @PostMapping("/steps")
    public AjaxResult saveStep(@Valid @RequestBody AiAgentStepSaveRequest request) {
        return toAjax(agentService.saveStep(request));
    }

    /**
     * 删除智能体步骤。
     */
    @RequiresPermissions("ai:agent:edit")
    @Log(title = "AI智能体步骤删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/steps/{stepId}")
    public AjaxResult deleteStep(@PathVariable Long stepId) {
        return toAjax(agentService.deleteStep(stepId));
    }
}
