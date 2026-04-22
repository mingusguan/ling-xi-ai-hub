package com.lingxi.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lingxi.oa.domain.OaExpense;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OaExpenseMapper extends BaseMapper<OaExpense> {
    
    List<OaExpense> selectCompletedExpenses(@Param("limit") int limit, @Param("deptId") Long deptId);
}
