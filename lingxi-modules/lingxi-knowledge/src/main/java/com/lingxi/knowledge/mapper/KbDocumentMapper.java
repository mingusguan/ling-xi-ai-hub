package com.lingxi.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lingxi.knowledge.domain.KbDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KbDocumentMapper extends BaseMapper<KbDocument> {

    /**
     * 查询部门文档列表（支持按分类过滤）
     * SQL 在 XML 中定义
     */
    List<KbDocument> selectListByDeptWithChildren(@Param("deptId") Long deptId, @Param("categoryId") Long categoryId);

    /**
     * 按入库状态统计文档数量。
     */
    Long countDocumentsByStatus(@Param("status") Integer status);
}
