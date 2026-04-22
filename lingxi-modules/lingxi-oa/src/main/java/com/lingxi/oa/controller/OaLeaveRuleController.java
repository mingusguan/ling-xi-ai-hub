package com.lingxi.oa.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.oa.domain.OaLeaveRule;
import com.lingxi.oa.service.IOaLeaveRuleService;

/**
 * P1: 假期规则Controller
 */
@RestController
@RequestMapping("/oa/leave/rule")
public class OaLeaveRuleController extends BaseController
{
    @Autowired
    private IOaLeaveRuleService leaveRuleService;

    /**
     * 获取启用的假期类型列表（不分页）
     */
    @GetMapping("/enabled")
    public R<List<OaLeaveRule>> getEnabledLeaveTypes()
    {
        OaLeaveRule query = new OaLeaveRule();
        query.setStatus("0");
        List<OaLeaveRule> list = leaveRuleService.selectOaLeaveRuleList(query);
        return R.ok(list);
    }

    /**
     * 查询假期规则列表
     */
    @GetMapping("/list")
    public TableDataInfo list(OaLeaveRule oaLeaveRule)
    {
        startPage();
        List<OaLeaveRule> list = leaveRuleService.selectOaLeaveRuleList(oaLeaveRule);
        return getDataTable(list);
    }

    /**
     * 获取假期规则详细信息
     */
    @GetMapping(value = "/{ruleId}")
    public R<OaLeaveRule> getInfo(@PathVariable("ruleId") Long ruleId)
    {
        return R.ok(leaveRuleService.selectOaLeaveRuleById(ruleId));
    }

    /**
     * 新增假期规则
     */
    @PostMapping
    public R<String> add(@RequestBody OaLeaveRule oaLeaveRule)
    {
        leaveRuleService.insertOaLeaveRule(oaLeaveRule);
        return R.ok("新增成功");
    }

    /**
     * 修改假期规则
     */
    @PutMapping
    public R<String> edit(@RequestBody OaLeaveRule oaLeaveRule)
    {
        leaveRuleService.updateOaLeaveRule(oaLeaveRule);
        return R.ok("修改成功");
    }

    /**
     * 删除假期规则
     */
    @DeleteMapping("/{ruleIds}")
    public R<String> remove(@PathVariable Long[] ruleIds)
    {
        leaveRuleService.deleteOaLeaveRuleByIds(ruleIds);
        return R.ok("删除成功");
    }
}
