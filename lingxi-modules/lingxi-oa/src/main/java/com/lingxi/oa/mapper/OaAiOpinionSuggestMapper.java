package com.lingxi.oa.mapper;

import com.lingxi.oa.domain.OaAiOpinionSuggest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OaAiOpinionSuggestMapper {

    int insertOaAiOpinionSuggest(OaAiOpinionSuggest suggest);

    int updateOaAiOpinionSuggest(OaAiOpinionSuggest suggest);

    OaAiOpinionSuggest selectLatestByBusiness(
            @Param("businessType") String businessType,
            @Param("businessId") Long businessId);

    List<OaAiOpinionSuggest> selectSimilarCases(
            @Param("businessType") String businessType,
            @Param("riskLevel") String riskLevel);
}
