package com.lingxi.oa.mapper;

import com.lingxi.oa.domain.OaProcessTimeoutConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OaProcessTimeoutConfigMapper {

    List<OaProcessTimeoutConfig> selectEnabledList();

    OaProcessTimeoutConfig selectByTaskKey(
            @Param("processDefinitionKey") String processDefinitionKey,
            @Param("taskDefinitionKey") String taskDefinitionKey);
}
