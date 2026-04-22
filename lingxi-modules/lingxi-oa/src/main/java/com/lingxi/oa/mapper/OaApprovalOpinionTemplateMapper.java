package com.lingxi.oa.mapper;

import com.lingxi.oa.domain.OaApprovalOpinionTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OaApprovalOpinionTemplateMapper {

    List<OaApprovalOpinionTemplate> selectEnabledList(@Param("businessType") String businessType);
}
