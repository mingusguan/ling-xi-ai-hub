package com.lingxi.ai.controller;

import com.lingxi.ai.mcp.service.McpPlatformService;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 工具市场控制器。
 *
 * <p>提供只读的工具市场清单查询能力，方便管理端查看当前已发布的 MCP 工具、权限和版本信息。</p>
 */
@RestController
@RequestMapping("/ai/mcp")
public class McpToolMarketController {

    private final McpPlatformService platformService;

    public McpToolMarketController(McpPlatformService platformService) {
        this.platformService = platformService;
    }

    /**
     * 查询当前已发布到工具市场的 MCP 工具清单。
     *
     * @return 工具市场列表
     */
    @RequiresPermissions("ai:mcp:market:list")
    @GetMapping("/market/tools")
    public AjaxResult listTools() {
        return AjaxResult.success(platformService.listMarketplaceTools());
    }
}
