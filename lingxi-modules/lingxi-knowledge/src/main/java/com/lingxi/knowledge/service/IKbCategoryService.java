package com.lingxi.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lingxi.knowledge.domain.KbCategory;

import java.util.List;

public interface IKbCategoryService extends IService<KbCategory> {

    /** 查询部门下所有已启用分类（平铺） */
    List<KbCategory> getListByDept(Long deptId);

    /** 新增分类 */
    Long addCategory(KbCategory category);

    /** 修改分类名称/排序/状态 */
    void updateCategory(KbCategory category);

    /** 删除分类（不允许删除有子分类的） */
    void deleteCategory(Long categoryId);
}
