package com.lingxi.knowledge.controller;

import com.lingxi.common.core.domain.R;
import com.lingxi.knowledge.domain.KbCategory;
import com.lingxi.knowledge.service.IKbCategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge/category")
public class KbCategoryController {

    @Resource
    private IKbCategoryService categoryService;

    /**
     * 查询部门下所有已启用分类（平铺列表）
     */
    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) Long deptId) {
        return R.ok(categoryService.getListByDept(deptId));
    }

    /**
     * 查询分类树（按 parentId 层级嵌套）
     */
    @GetMapping("/tree")
    public R<?> tree(@RequestParam(required = false) Long deptId) {
        List<KbCategory> all = categoryService.getListByDept(deptId);
        return R.ok(buildTree(all, 0L));
    }

    /**
     * 新增分类
     */
    @PostMapping
    public R<?> add(@RequestBody KbCategory category) {
        Long id = categoryService.addCategory(category);
        return R.ok(Map.of("categoryId", id));
    }

    /**
     * 修改分类
     */
    @PutMapping
    public R<?> update(@RequestBody KbCategory category) {
        categoryService.updateCategory(category);
        return R.ok();
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{categoryId}")
    public R<?> delete(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return R.ok();
    }

    // ----- 树形构建辅助 -----
    private List<Map<String, Object>> buildTree(List<KbCategory> all, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (KbCategory c : all) {
            if (parentId.equals(c.getParentId())) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("categoryId", c.getCategoryId());
                node.put("categoryName", c.getCategoryName());
                node.put("parentId", c.getParentId());
                node.put("deptId", c.getDeptId());
                node.put("sort", c.getSort());
                node.put("status", c.getStatus());
                List<Map<String, Object>> children = buildTree(all, c.getCategoryId());
                node.put("children", children);
                result.add(node);
            }
        }
        return result;
    }
}
