package com.lingxi.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lingxi.oa.domain.OaLeave;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OaLeaveMapper extends BaseMapper<OaLeave> {
    
    List<OaLeave> selectCompletedLeaves(@Param("limit") int limit, @Param("deptId") Long deptId);
}
