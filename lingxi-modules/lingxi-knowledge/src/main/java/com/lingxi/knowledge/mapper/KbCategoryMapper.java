package com.lingxi.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lingxi.knowledge.domain.KbCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KbCategoryMapper extends BaseMapper<KbCategory> {

    List<KbCategory> selectListByDeptWithChildren(@Param("deptId") Long deptId);
}
