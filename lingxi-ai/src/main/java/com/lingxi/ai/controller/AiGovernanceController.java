package com.lingxi.ai.controller;

import com.lingxi.ai.governance.service.IAiGovernanceService;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.security.annotation.RequiresPermissions;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI治理审计控制器。
 */
@RestController
@RequestMapping("/ai/governance")
public class AiGovernanceController extends BaseController {

    private final IAiGovernanceService governanceService;

    public AiGovernanceController(IAiGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    /**
     * 查询治理中心概览。
     */
    @RequiresPermissions("ai:governance:view")
    @GetMapping("/stats")
    public AjaxResult stats() {
        return AjaxResult.success(governanceService.getStats());
    }

    /**
     * 分页查询AI治理审计日志。
     */
    @RequiresPermissions("ai:governance:view")
    @GetMapping("/logs")
    public TableDataInfo listLogs(@RequestParam(required = false) String sceneType,
            @RequestParam(required = false) String sceneName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String sensitiveHit) {
        startPage();
        return getDataTable(governanceService.listLogs(sceneType, sceneName, status, riskLevel, sensitiveHit));
    }

    /**
     * 记录其他模块传入的治理日志。
     */
    @PostMapping("/record")
    public AjaxResult record(@RequestBody Map<String, Object> payload) {
        governanceService.recordExternal(payload);
        return AjaxResult.success();
    }
}
