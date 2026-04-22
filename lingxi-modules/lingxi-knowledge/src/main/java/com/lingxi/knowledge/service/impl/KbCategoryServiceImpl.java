package com.lingxi.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lingxi.knowledge.domain.KbCategory;
import com.lingxi.knowledge.mapper.KbCategoryMapper;
import com.lingxi.knowledge.service.IKbCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class KbCategoryServiceImpl extends ServiceImpl<KbCategoryMapper, KbCategory> implements IKbCategoryService {

    private final KbCategoryMapper categoryMapper;

    @Override
    public List<KbCategory> getListByDept(Long deptId) {
        if (Objects.isNull(deptId)) {
            return lambdaQuery()
                    .eq(KbCategory::getStatus, 1)
                    .orderByAsc(KbCategory::getSort)
                    .list();
        }
        return categoryMapper.selectListByDeptWithChildren(deptId);
    }

    @Override
    public Long addCategory(KbCategory category) {
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        save(category);
        return category.getCategoryId();
    }

    @Override
    public void updateCategory(KbCategory category) {
        updateById(category);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        boolean hasChildren = lambdaQuery()
                .eq(KbCategory::getParentId, categoryId)
                .exists();
        if (hasChildren) {
            throw new RuntimeException("请先删除子分类再进行删除");
        }
        removeById(categoryId);
    }
}
