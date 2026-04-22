package com.lingxi.oa.controller;

import com.lingxi.common.core.domain.R;
import com.lingxi.oa.service.IOaWorkflowBizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OA工作台控制器
 * 提供工作台概览数据查询功能
 *
 * @author lingxi
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/oa/dashboard")
public class OaDashboardController {

    private final IOaWorkflowBizService workflowBizService;

    /**
     * 获取工作台概览数据
     * 包括待办任务、已办任务等统计信息
     *
     * @return 工作台概览数据
     */
    @GetMapping("/summary")
    public R<?> summary() {
        return R.ok(workflowBizService.dashboardSummary());
    }
}
