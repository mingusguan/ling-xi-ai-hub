package com.lingxi.oa.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.ai.api.RemoteAiModelService;
import com.lingxi.oa.enums.ApprovalStatusEnum;
import com.lingxi.system.api.RemoteUserService;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import com.lingxi.oa.domain.OaAiOpinionSuggest;
import com.lingxi.oa.domain.OaApprovalOpinionTemplate;
import com.lingxi.oa.domain.OaApprovalRecord;
import com.lingxi.oa.domain.OaExpense;
import com.lingxi.oa.domain.OaLeave;
import com.lingxi.oa.dto.OaAiSuggestionDTO;
import com.lingxi.oa.dto.OaBusinessFormDataDTO;
import com.lingxi.oa.dto.OaSimilarCaseDTO;
import com.lingxi.oa.mapper.OaAiOpinionSuggestMapper;
import com.lingxi.oa.mapper.OaApprovalOpinionTemplateMapper;
import com.lingxi.oa.mapper.OaExpenseMapper;
import com.lingxi.oa.mapper.OaLeaveMapper;
import com.lingxi.oa.service.IOaAiOpinionService;
import com.lingxi.oa.service.IOaApprovalRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;

@Service
public class OaAiOpinionServiceImpl implements IOaAiOpinionService {
    private static final Logger log = LoggerFactory.getLogger(OaAiOpinionServiceImpl.class);

    // AI建议保存线程池
    private static final ExecutorService aiSaveExecutor = new ThreadPoolExecutor(
            2, 5, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Autowired
    private OaApprovalOpinionTemplateMapper templateMapper;

    @Autowired
    private OaAiOpinionSuggestMapper suggestMapper;

    @Autowired
    private OaLeaveMapper leaveMapper;

    @Autowired
    private OaExpenseMapper expenseMapper;

    @Autowired
    private IOaApprovalRecordService approvalRecordService;

    @Autowired
    private RemoteAiModelService remoteAiModelService;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public AjaxResult generateSuggestion(String businessType, Long businessId, String taskId, Long approverId) {
        OaBusinessFormDataDTO formData = getFormData(businessType, businessId);
        if (formData == null) {
            return AjaxResult.error("获取表单数据失败");
        }
        Long suggestId = null;
        // 检查是否已存在AI建议
        OaAiOpinionSuggest existSuggest = suggestMapper.selectLatestByBusiness(businessType, businessId);
    
        // 如果已生成完成，直接返回
        if (existSuggest != null && !"AI正在分析中，请稍候...".equals(existSuggest.getAiSuggestion())) {
            List<OaSimilarCaseDTO> similarCases = getSimilarCases(businessType, formData);
    
            OaAiSuggestionDTO result = new OaAiSuggestionDTO();
            result.setSuggestId(existSuggest.getSuggestId());
            result.setAiSuggestion(existSuggest.getAiSuggestion());
            result.setRiskLevel(existSuggest.getRiskLevel());
            result.setRiskPoints(JSON.parseArray(existSuggest.getRiskPoints(), String.class));
            result.setSimilarCases(similarCases);
            
            // 优先使用AI生成的模板，如果没有则使用数据库模板
            List<String> suggestions = new ArrayList<>();
            if (existSuggest.getTemplates() != null && !existSuggest.getTemplates().isEmpty()) {
                try {
                    suggestions = JSON.parseArray(existSuggest.getTemplates(), String.class);
                } catch (Exception e) {
                    log.warn("解析AI模板失败，使用数据库模板", e);
                }
            }
            
            // 如果AI模板为空，使用数据库模板
            if (suggestions.isEmpty()) {
                List<OaApprovalOpinionTemplate> templates = templateMapper.selectEnabledList(businessType);
                for (OaApprovalOpinionTemplate template : templates) {
                    suggestions.add(template.getTemplateContent());
                }
            }
            
            result.setTemplates(suggestions);
            return AjaxResult.success(result);
        }
    
        // 获取相似案例
        List<OaSimilarCaseDTO> similarCases = getSimilarCases(businessType, formData);
    
        // 如果不存在，先创建
        OaAiOpinionSuggest loadingSuggest;
        if (existSuggest == null) {
            // 创建新的loading记录
            loadingSuggest = new OaAiOpinionSuggest();
            loadingSuggest.setProcessInstanceId(formData.getProcessInstanceId());
            loadingSuggest.setTaskId(taskId);
            loadingSuggest.setBusinessType(businessType);
            loadingSuggest.setBusinessId(businessId);
            loadingSuggest.setApproverId(approverId);
            loadingSuggest.setApplicantName(formData.getApplicantName());
            loadingSuggest.setFormData(JSON.toJSONString(formData));
            loadingSuggest.setAiSuggestion("AI正在分析中，请稍候...");
            loadingSuggest.setRiskLevel("loading");
            loadingSuggest.setRiskPoints("[]");
            loadingSuggest.setSimilarCases(JSON.toJSONString(similarCases));
            loadingSuggest.setUsed("0");
            loadingSuggest.setCreateTime(new Date());
            suggestMapper.insertOaAiOpinionSuggest(loadingSuggest);
            suggestId = loadingSuggest.getSuggestId();
        }else{
            suggestId = existSuggest.getSuggestId();
        }
    
        // 返回loading状态（模板先用数据库的）
        List<OaApprovalOpinionTemplate> templates = templateMapper.selectEnabledList(businessType);
        List<String> suggestions = new ArrayList<>();
        for (OaApprovalOpinionTemplate template : templates) {
            suggestions.add(template.getTemplateContent());
        }
        
        OaAiSuggestionDTO result = new OaAiSuggestionDTO();
        result.setSuggestId(null);
        result.setAiSuggestion("AI正在分析中，请稍候...");
        result.setRiskLevel("loading");
        result.setRiskPoints(new ArrayList<>());
        result.setSimilarCases(similarCases);
        result.setTemplates(suggestions);
    
        // 异步调用AI（耗时操作）
        final OaBusinessFormDataDTO finalFormData = formData;
        Long finalSuggestId = suggestId;
        aiSaveExecutor.submit(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // 构建上下文
                Map<String, Object> aiContext = buildAiContext(businessType, finalFormData, similarCases, "low", new ArrayList<>());
    
                // 一次性调用AI获取完整分析结果
                JSONObject analyzeResult = null;
                try {
                    R<String> aiResult = remoteAiModelService.analyzeAndSuggest(aiContext, SecurityConstants.INNER);
                    if (aiResult != null && aiResult.getData() != null) {
                        analyzeResult = JSON.parseObject(aiResult.getData());
                    }
                } catch (Exception e) {
                    log.error("AI完整分析失败", e);
                }
    
                // 解析结果
                String riskLevel = "normal";
                List<String> riskPoints = new ArrayList<>();
                String aiSuggestion = "";
                List<String> aiTemplates = new ArrayList<>();
                
                if (analyzeResult != null) {
                    riskLevel = analyzeResult.getString("riskLevel");
                    riskPoints = analyzeResult.getJSONArray("riskPoints").toJavaList(String.class);
                    aiSuggestion = analyzeResult.getString("aiSuggestion");
                    aiTemplates = analyzeResult.getJSONArray("templates").toJavaList(String.class);
                } else {
                    // 降级：使用默认值
                    aiSuggestion = buildDefaultSuggestion(businessType, finalFormData, similarCases, riskLevel, riskPoints);
                    List<OaApprovalOpinionTemplate> dbTemplates = templateMapper.selectEnabledList(businessType);
                    for (OaApprovalOpinionTemplate template : dbTemplates) {
                        aiTemplates.add(template.getTemplateContent());
                    }
                }
    
                // 更新记录
                OaAiOpinionSuggest suggest = new OaAiOpinionSuggest();
                suggest.setSuggestId(finalSuggestId);
                suggest.setAiSuggestion(aiSuggestion);
                suggest.setRiskLevel(riskLevel);
                suggest.setRiskPoints(JSON.toJSONString(riskPoints));
                suggest.setTemplates(JSON.toJSONString(aiTemplates));
                suggestMapper.updateOaAiOpinionSuggest(suggest);
                    
                long costTime = System.currentTimeMillis() - startTime;
                log.info("AI建议生成完成，suggestId: {}, 耗时: {}ms, aiSuggestion: {}", finalSuggestId, costTime, aiSuggestion);
            } catch (Exception e) {
                log.error("AI建议生成失败", e);
            }
        });
    
        return AjaxResult.success(result);
    }

    @Override
    public List<OaSimilarCaseDTO> getSimilarCases(String businessType, OaBusinessFormDataDTO formData) {
        List<OaSimilarCaseDTO> cases = new ArrayList<>();
        
        try {
            // 获取当前申请人姓名
            String applicantName = formData.getApplicantName();
            
            if ("leave".equals(businessType)) {
                // 查询该申请人最近的5条已完成的请假记录（排除当前申请）
                List<OaLeave> leaves = leaveMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OaLeave>()
                        .eq(OaLeave::getApplicantUserId, formData.getApplicantUserId())
                        .in(OaLeave::getApprovalStatus, ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode())
                        .ne(formData.getBusinessId() != null, OaLeave::getLeaveId, formData.getBusinessId())
                        .orderByDesc(OaLeave::getCreateTime)
                        .last("LIMIT 5"));
                
                for (OaLeave leave : leaves) {
                    // 查询该请假的所有审批记录
                    List<OaApprovalRecord> records = approvalRecordService.listByBusinessKey("leave:" + leave.getLeaveId());
                    
                    if (records == null || records.isEmpty()) {
                        continue;
                    }
                    
                    // 取最后一条审批记录
                    OaApprovalRecord targetRecord = records.get(records.size() - 1);
                    
                    OaSimilarCaseDTO caseDTO = new OaSimilarCaseDTO();
                    caseDTO.setApplicant(getUserNameById(leave.getApplicantUserId()));
                    caseDTO.setDept(leave.getDeptName());
                    caseDTO.setType(convertLeaveType(leave.getLeaveType()));
                    caseDTO.setValue(leave.getLeaveHours() != null ? leave.getLeaveHours().divide(new BigDecimal("24"), 1, RoundingMode.HALF_UP) + " 天" : null);
                    caseDTO.setApplyTime(leave.getCreateTime());
                    caseDTO.setApprovalResult("approved".equals(leave.getApprovalStatus()) ? "通过" : "驳回");
                    caseDTO.setApproverName(targetRecord.getApproverName());
                    caseDTO.setApproverOpinion(targetRecord.getCommentText());
                    caseDTO.setBusinessType(businessType);
                    cases.add(caseDTO);
                }
            } else if ("expense".equals(businessType)) {
                // 查询该申请人最近的5条已完成的报销记录（排除当前申请）
                List<OaExpense> expenses = expenseMapper.selectList(new LambdaQueryWrapper<OaExpense>()
                        .eq(OaExpense::getApplicantUserId, formData.getApplicantUserId())
                        .in(OaExpense::getApprovalStatus, ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode())
                        .ne(formData.getBusinessId() != null, OaExpense::getExpenseId, formData.getBusinessId())
                        .orderByDesc(OaExpense::getCreateTime)
                        .last("LIMIT 5"));
                
                for (OaExpense expense : expenses) {
                    // 查询该报销的所有审批记录
                    List<OaApprovalRecord> records = approvalRecordService.listByBusinessKey("expense:" + expense.getExpenseId());
                    
                    if (records == null || records.isEmpty()) {
                        continue;
                    }
                    
                    // 取最后一条审批记录
                    OaApprovalRecord targetRecord = records.get(records.size() - 1);
                    
                    OaSimilarCaseDTO caseDTO = new OaSimilarCaseDTO();
                    caseDTO.setApplicant(getUserNameById(expense.getApplicantUserId()));
                    caseDTO.setDept(expense.getDeptName());
                    caseDTO.setType(convertExpenseType(expense.getExpenseType()));
                    caseDTO.setValue(expense.getAmount() != null ? expense.getAmount() + " 元" : null);
                    caseDTO.setApplyTime(expense.getCreateTime());
                    caseDTO.setApprovalResult("approved".equals(expense.getApprovalStatus()) ? "通过" : "驳回");
                    caseDTO.setApproverName(targetRecord.getApproverName());
                    caseDTO.setApproverOpinion(targetRecord.getCommentText());
                    caseDTO.setBusinessType(businessType);
                    cases.add(caseDTO);
                }
            }
            
            cases.sort((a, b) -> {
                if ("驳回".equals(a.getApprovalResult()) && !"驳回".equals(b.getApprovalResult())) {
                    return -1;
                }
                if (!"驳回".equals(a.getApprovalResult()) && "驳回".equals(b.getApprovalResult())) {
                    return 1;
                }
                return 0;
            });
        } catch (Exception e) {
            log.error("获取相似案例失败", e);
        }
        
        return cases;
    }

    @Override
    public AjaxResult getSuggestionStatus(String businessType, Long businessId) {
        try {
            // 查询最新的AI建议记录
            OaAiOpinionSuggest suggest = suggestMapper.selectLatestByBusiness(businessType, businessId);

            if (suggest == null) {
                return AjaxResult.error("未找到AI建议记录");
            }

            // 如果还在生成中
            if ("AI正在分析中，请稍候...".equals(suggest.getAiSuggestion())) {
                Map<String, Object> result = new HashMap<>();
                result.put("status", "generating");
                result.put("message", "AI正在分析中...");
                return AjaxResult.success(result);
            }

            // 已生成完成
            Map<String, Object> result = new HashMap<>();
            result.put("status", "completed");
            result.put("aiSuggestion", suggest.getAiSuggestion());
            result.put("riskLevel", suggest.getRiskLevel());
            result.put("riskPoints", JSON.parseArray(suggest.getRiskPoints(), String.class));
            
            // 返回AI生成的推荐意见
            if (suggest.getTemplates() != null && !suggest.getTemplates().isEmpty()) {
                try {
                    List<String> aiTemplates = JSON.parseArray(suggest.getTemplates(), String.class);
                    result.put("templates", aiTemplates);
                } catch (Exception e) {
                    log.warn("解析AI推荐意见失败", e);
                    result.put("templates", new ArrayList<>());
                }
            } else {
                result.put("templates", new ArrayList<>());
            }
            
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("查询AI建议状态失败", e);
            return AjaxResult.error("查询失败");
        }
    }

    @Override
    public AjaxResult analyzeRisk(String businessType, OaBusinessFormDataDTO formData) {
        List<String> riskPoints = new ArrayList<>();
        String riskLevel = "low";

        if ("expense".equals(businessType)) {
            BigDecimal amount = formData.getAmount();
            if (amount != null && amount.compareTo(new BigDecimal("5000")) >= 0) {
                riskPoints.add("报销金额较大，建议核实票据真实性");
                riskLevel = "normal";
            }
            if (amount != null && amount.compareTo(new BigDecimal("20000")) >= 0) {
                riskPoints.add("大额报销，建议升级审批");
                riskLevel = "high";
            }
        }

        if ("leave".equals(businessType)) {
            Integer days = formData.getLeaveDays();
            if (days != null && days >= 5) {
                riskPoints.add("请假时间较长，请确认工作已交接");
                riskLevel = "normal";
            }
            Date startDate = formData.getStartDate();
            if (startDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.MONDAY) {
                    riskPoints.add("临近周末，请关注项目进度");
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("riskLevel", riskLevel);
        result.put("riskPoints", riskPoints);
        
        return AjaxResult.success(result);
    }

    private OaBusinessFormDataDTO getFormData(String businessType, Long businessId) {
        try {
            OaBusinessFormDataDTO result = new OaBusinessFormDataDTO();
            if ("leave".equals(businessType)) {
                OaLeave leave = leaveMapper.selectById(businessId);
                result.setBusinessId(leave.getLeaveId());
                result.setApplicantUserId(leave.getApplicantUserId());
                result.setApplicantName(getUserNameById(leave.getApplicantUserId()));
                result.setLeaveType(leave.getLeaveType());
                result.setStartDate(leave.getStartTime());
                result.setEndDate(leave.getEndTime());
                result.setLeaveDays(leave.getLeaveHours() != null ? leave.getLeaveHours().intValue() / 8 : 0);
                result.setReason(leave.getLeaveReason());
                result.setProcessInstanceId(leave.getProcessInstanceId());
                result.setApprovalStatus(leave.getApprovalStatus());
            } else if ("expense".equals(businessType)) {
                OaExpense expense = expenseMapper.selectById(businessId);
                result.setBusinessId(expense.getExpenseId());
                result.setApplicantUserId(expense.getApplicantUserId());
                result.setApplicantName(getUserNameById(expense.getApplicantUserId()));
                result.setExpenseType(expense.getExpenseType());
                result.setAmount(expense.getAmount());
                result.setReason(expense.getExpenseReason());
                result.setProcessInstanceId(expense.getProcessInstanceId());
                result.setApprovalStatus(expense.getApprovalStatus());
            }
            return result;
        } catch (Exception e) {
            log.error("获取表单数据失败", e);
            return null;
        }
    }

    /**
     * 构建AI上下文
     */
    private Map<String, Object> buildAiContext(String businessType, OaBusinessFormDataDTO formData, 
                                               List<OaSimilarCaseDTO> similarCases, String riskLevel,
                                               List<String> riskPoints) {
        Map<String, Object> context = new HashMap<>();
        context.put("businessType", businessType);
        context.put("applicantName", formData.getApplicantName());
        
        if ("leave".equals(businessType)) {
            context.put("leaveType", convertLeaveType(formData.getLeaveType()));
            context.put("leaveDays", formData.getLeaveDays());
            context.put("startDate", formData.getStartDate());
            context.put("endDate", formData.getEndDate());
            context.put("reason", formData.getReason());
        } else if ("expense".equals(businessType)) {
            context.put("expenseType", convertExpenseType(formData.getExpenseType()));
            context.put("amount", formData.getAmount());
            context.put("reason", formData.getReason());
        }
        
        // 添加相似案例
        if (similarCases != null && !similarCases.isEmpty()) {
            context.put("similarCases", JSON.toJSONString(similarCases));
        }
        
        // 添加风险信息
        Map<String, Object> riskInfo = new HashMap<>();
        riskInfo.put("riskLevel", riskLevel);
        riskInfo.put("riskPoints", riskPoints);
        context.put("riskInfo", JSON.toJSONString(riskInfo));
        
        return context;
    }

    /**
     * 构建表单数据Map用于风险分析
     */
    private Map<String, Object> buildFormDataMap(String businessType, OaBusinessFormDataDTO formData) {
        Map<String, Object> formDataMap = new HashMap<>();
        formDataMap.put("businessType", businessType);
        formDataMap.put("applicantName", formData.getApplicantName());
        
        if ("leave".equals(businessType)) {
            formDataMap.put("leaveType", formData.getLeaveType());
            formDataMap.put("leaveDays", formData.getLeaveDays());
            formDataMap.put("startDate", formData.getStartDate());
            formDataMap.put("reason", formData.getReason());
        } else if ("expense".equals(businessType)) {
            formDataMap.put("expenseType", formData.getExpenseType());
            formDataMap.put("amount", formData.getAmount());
            formDataMap.put("reason", formData.getReason());
        }
        
        return formDataMap;
    }

    /**
     * 构建默认建议（AI服务失败时的降级方案）
     */
    private String buildDefaultSuggestion(String businessType, OaBusinessFormDataDTO formData,
                                         List<OaSimilarCaseDTO> similarCases, String riskLevel,
                                         List<String> riskPoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("【AI审批建议】\n\n");
        
        if ("leave".equals(businessType)) {
            sb.append(String.format("申请人%s申请%s%d天。\n", 
                formData.getApplicantName(),
                convertLeaveType(formData.getLeaveType()),
                formData.getLeaveDays()));
        } else if ("expense".equals(businessType)) {
            sb.append(String.format("申请人%s申请%s报销%.2f元。\n",
                formData.getApplicantName(),
                convertExpenseType(formData.getExpenseType()),
                formData.getAmount()));
        }
        
        // 显示风险等级
        String riskLevelText = "low".equals(riskLevel) ? "低风险" : "normal".equals(riskLevel) ? "中风险" : "高风险";
        sb.append(String.format("风险等级：%s\n", riskLevelText));
        
        if (!riskPoints.isEmpty()) {
            sb.append("风险提示：\n");
            for (String point : riskPoints) {
                sb.append("- ").append(point).append("\n");
            }
        }
        
        if (similarCases != null && !similarCases.isEmpty()) {
            long passedCount = similarCases.stream()
                .filter(c -> "通过".equals(c.getApprovalResult()))
                .count();
            double passRate = (double) passedCount / similarCases.size() * 100;
            sb.append(String.format("\n历史相似案例通过率：%.0f%%\n", passRate));
        }
        
        sb.append("\n建议：请根据实际情况审慎审批。");
        return sb.toString();
    }

    private String convertLeaveType(String type) {
        switch (type) {
            case "1": return "事假";
            case "2": return "病假";
            case "3": return "年假";
            default: return "请假";
        }
    }

    private String convertExpenseType(String type) {
        switch (type) {
            case "1": return "差旅";
            case "2": return "交通";
            case "3": return "餐饮";
            default: return "费用";
        }
    }

    /**
     * 根据用户ID获取用户名
     */
    private String getUserNameById(Long userId) {
        if (userId == null) {
            return "";
        }
        try {
            R<SysUser> result = remoteUserService.getUserById(userId, SecurityConstants.INNER);
            if (result != null && result.getData() != null) {
                return result.getData().getNickName();
            }
        } catch (Exception e) {
            log.error("查询用户 {} 信息失败", userId, e);
        }
        return "";
    }
}
