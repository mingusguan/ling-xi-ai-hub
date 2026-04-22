package com.lingxi.oa.mapper;

import com.lingxi.oa.domain.OaProcessTimeoutWarning;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OaProcessTimeoutWarningMapper {

    int insertOaProcessTimeoutWarning(OaProcessTimeoutWarning warning);

    int updateOaProcessTimeoutWarning(OaProcessTimeoutWarning warning);

    List<OaProcessTimeoutWarning> selectUnprocessed();

    OaProcessTimeoutWarning selectByTaskId(@Param("taskId") String taskId);
    
    /**
     * 根据任务ID和审批人查询预警记录
     */
    OaProcessTimeoutWarning selectByTaskIdAndAssignee(@Param("taskId") String taskId, @Param("assignee") String assignee);
    
    /**
     * 查询当前用户未处理的超时预警
     */
    List<OaProcessTimeoutWarning> selectUnprocessedByAssignee(@Param("assignee") String assignee);
}
