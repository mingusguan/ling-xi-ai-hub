package com.lingxi.ai.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

import com.lingxi.common.security.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lingxi.common.log.annotation.Log;
import com.lingxi.common.log.enums.BusinessType;
import com.lingxi.ai.domain.Mindmap;
import com.lingxi.ai.service.IMindmapService;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.core.utils.poi.ExcelUtil;
import com.lingxi.common.core.web.page.TableDataInfo;

/**
 * 思维导图Controller
 * 
 * @author cloud
 */
@RestController
@RequestMapping("/ai/mindmap")
public class MindmapController extends BaseController
{
    @Autowired
    private IMindmapService mindmapService;

    /**
     * 查询思维导图列表
     */
    @RequiresPermissions("ai:mindmap:list")
    @GetMapping("/list")
    public TableDataInfo list(Mindmap mindmap)
    {
        startPage();
        List<Mindmap> list = mindmapService.selectMindmapList(mindmap);
        return getDataTable(list);
    }

    /**
     * 导出思维导图列表
     */
    @RequiresPermissions("ai:mindmap:export")
    @Log(title = "思维导图", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Mindmap mindmap)
    {
        List<Mindmap> list = mindmapService.selectMindmapList(mindmap);
        ExcelUtil<Mindmap> util = new ExcelUtil<Mindmap>(Mindmap.class);
        util.exportExcel(response, list, "思维导图数据");
    }

    /**
     * 获取思维导图详细信息
     */
    @RequiresPermissions("ai:mindmap:query")
    @GetMapping(value = "/{mindmapId}")
    public AjaxResult getInfo(@PathVariable("mindmapId") Long mindmapId)
    {
        return success(mindmapService.selectMindmapById(mindmapId));
    }

    /**
     * 新增思维导图
     */
    @RequiresPermissions("ai:mindmap:add")
    @Log(title = "思维导图", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Mindmap mindmap)
    {
        return toAjax(mindmapService.insertMindmap(mindmap));
    }

    /**
     * 修改思维导图
     */
    @RequiresPermissions("ai:mindmap:edit")
    @Log(title = "思维导图", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Mindmap mindmap)
    {
        return toAjax(mindmapService.updateMindmap(mindmap));
    }

    /**
     * 删除思维导图
     */
    @RequiresPermissions("ai:mindmap:remove")
    @Log(title = "思维导图", businessType = BusinessType.DELETE)
	@DeleteMapping("/{mindmapIds}")
    public AjaxResult remove(@PathVariable Long[] mindmapIds)
    {
        return toAjax(mindmapService.deleteMindmapByIds(mindmapIds));
    }

    /**
     * AI生成思维导图
     */
    @RequiresPermissions("ai:mindmap:generate")
    @Log(title = "AI生成思维导图", businessType = BusinessType.OTHER)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody GenerateRequest request)
    {
        String dataJson = mindmapService.generateMindmapByAI(request.getPrompt(), request.getLayoutType());
        return AjaxResult.success("生成成功",dataJson);
    }

    /**
     * 生成请求参数类
     */
    public static class GenerateRequest {
        private String prompt;
        private String layoutType;

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getLayoutType() {
            return layoutType;
        }

        public void setLayoutType(String layoutType) {
            this.layoutType = layoutType;
        }
    }
}
