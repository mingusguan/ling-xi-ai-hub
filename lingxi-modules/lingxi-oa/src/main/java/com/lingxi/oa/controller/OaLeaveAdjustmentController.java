package com.lingxi.oa.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.oa.domain.OaLeaveAdjustment;
import com.lingxi.oa.service.IOaLeaveAdjustmentService;

/**
 * P2: 假期额度调整记录Controller
 */
@RestController
@RequestMapping("/oa/leave/adjustment")
public class OaLeaveAdjustmentController extends BaseController
{
    @Autowired
    private IOaLeaveAdjustmentService leaveAdjustmentService;

    /**
     * 查询额度调整记录列表
     */
    @GetMapping("/list")
    public TableDataInfo list(OaLeaveAdjustment oaLeaveAdjustment)
    {
        startPage();
        List<OaLeaveAdjustment> list = leaveAdjustmentService.selectOaLeaveAdjustmentList(oaLeaveAdjustment);
        return getDataTable(list);
    }
}
