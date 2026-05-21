package com.lingxi.knowledge.controller;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.security.annotation.Logical;
import com.lingxi.common.security.annotation.RequiresPermissions;
import com.lingxi.knowledge.domain.KbCategory;
import com.lingxi.knowledge.service.IKbCategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge/category")
public class KbCategoryController
{
    @Resource
    private IKbCategoryService categoryService;

    /**
     * 查询部门下所有已启用分类（平铺列表）。
     */
    @RequiresPermissions("knowledge:category:list")
    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) Long deptId)
    {
        return R.ok(categoryService.getListByDept(deptId));
    }

    /**
     * 查询分类树。
     */
    @RequiresPermissions("knowledge:category:list")
    @GetMapping("/tree")
    public R<?> tree(@RequestParam(required = false) Long deptId)
    {
        List<KbCategory> all = categoryService.getListByDept(deptId);
        return R.ok(buildTree(all, 0L));
    }

    /**
     * 新增分类。
     */
    @RequiresPermissions(value = {"knowledge:category:add", "knowledge:category:list"}, logical = Logical.OR)
    @PostMapping
    public R<?> add(@RequestBody KbCategory category)
    {
        Long id = categoryService.addCategory(category);
        return R.ok(Map.of("categoryId", id));
    }

    /**
     * 修改分类。
     */
    @RequiresPermissions(value = {"knowledge:category:edit", "knowledge:category:list"}, logical = Logical.OR)
    @PutMapping
    public R<?> update(@RequestBody KbCategory category)
    {
        categoryService.updateCategory(category);
        return R.ok();
    }

    /**
     * 删除分类。
     */
    @RequiresPermissions(value = {"knowledge:category:remove", "knowledge:category:list"}, logical = Logical.OR)
    @DeleteMapping("/{categoryId}")
    public R<?> delete(@PathVariable Long categoryId)
    {
        categoryService.deleteCategory(categoryId);
        return R.ok();
    }

    private List<Map<String, Object>> buildTree(List<KbCategory> all, Long parentId)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        for (KbCategory category : all) {
            if (parentId.equals(category.getParentId())) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("categoryId", category.getCategoryId());
                node.put("categoryName", category.getCategoryName());
                node.put("parentId", category.getParentId());
                node.put("deptId", category.getDeptId());
                node.put("sort", category.getSort());
                node.put("status", category.getStatus());
                node.put("children", buildTree(all, category.getCategoryId()));
                result.add(node);
            }
        }
        return result;
    }
}
