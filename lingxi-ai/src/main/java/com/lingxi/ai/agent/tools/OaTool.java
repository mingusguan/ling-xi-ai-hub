package com.lingxi.ai.agent.tools;

import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.oa.api.RemoteOaService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OA业务查询工具
 */
@Component
public class OaTool {

    @Autowired
    private RemoteOaService remoteOaService;

    @Tool("查询用户的待审批任务列表")
    public String queryPendingTasks(Long userId) {
        try {
            R<List<Map<String, Object>>> result = remoteOaService.getPendingTasks(userId, SecurityConstants.INNER);
            if (result != null && result.getData() != null) {
                List<Map<String, Object>> tasks = result.getData();
                if (tasks.isEmpty()) {
                    return "您目前没有待审批的任务";
                }
                StringBuilder sb = new StringBuilder("您有").append(tasks.size()).append("个待审批任务：\n");
                for (int i = 0; i < Math.min(tasks.size(), 5); i++) {
                    Map<String, Object> task = tasks.get(i);
                    sb.append(i + 1).append(". ").append(task.get("taskName"))
                      .append(" (创建时间: ").append(task.get("createTime")).append(")\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "查询待办任务失败：" + e.getMessage();
        }
        return "无法获取待办任务信息";
    }

    @Tool("查询用户的假期余额，包括年假、调休假等")
    public String queryLeaveBalance(Long userId) {
        try {
            R<Map<String, Object>> result = remoteOaService.getLeaveBalance(userId, SecurityConstants.INNER);
            if (result != null && result.getData() != null) {
                Map<String, Object> balance = result.getData();
                StringBuilder sb = new StringBuilder("您的假期余额：\n");
                balance.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
                return sb.toString();
            }
        } catch (Exception e) {
            return "查询假期余额失败：" + e.getMessage();
        }
        return "无法获取假期余额信息";
    }

    @Tool("查询用户的报销记录状态")
    public String queryExpenseStatus(Long userId) {
        try {
            R<List<Map<String, Object>>> result = remoteOaService.getExpenseStatus(userId, SecurityConstants.INNER);
            if (result != null && result.getData() != null) {
                List<Map<String, Object>> expenses = result.getData();
                if (expenses.isEmpty()) {
                    return "您目前没有报销记录";
                }
                StringBuilder sb = new StringBuilder("最近的报销记录：\n");
                for (Map<String, Object> expense : expenses) {
                    sb.append("- ").append(expense.get("expenseType"))
                      .append(" ¥").append(expense.get("amount"))
                      .append(" [").append(expense.get("approvalStatus")).append("]\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "查询报销状态失败：" + e.getMessage();
        }
        return "无法获取报销状态信息";
    }

    @Tool("查询超时未审批的任务预警信息")
    public String queryTimeoutWarning(Long userId) {
        try {
            R<List<Map<String, Object>>> result = remoteOaService.getTimeoutWarning(userId, SecurityConstants.INNER);
            if (result != null && result.getData() != null) {
                List<Map<String, Object>> warnings = result.getData();
                if (warnings.isEmpty()) {
                    return "目前没有超时未审批的任务";
                }
                StringBuilder sb = new StringBuilder("警告！有").append(warnings.size()).append("个任务已超时未审批：\n");
                for (Map<String, Object> warning : warnings) {
                    sb.append("- ").append(warning.get("taskName"))
                      .append(" (超时").append(warning.get("timeoutDays")).append("天")
                      .append(", 申请人: ").append(warning.get("applicantName")).append(")\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "查询超时预警失败：" + e.getMessage();
        }
        return "无法获取超时预警信息";
    }

    @Tool("计算请假工作时长，自动排除周末和法定节假日，只计算工作时间（9:00-12:00, 13:00-18:00）")
    public String calculateLeaveDuration(String startDate, String endDate) {
        try {
            // 如果时间字符串不包含时分秒，补充默认时间
            String startTime = startDate.length() == 10 ? startDate + " 09:00:00" : startDate;
            String endTime = endDate.length() == 10 ? endDate + " 18:00:00" : endDate;
            
            R<Map<String, Object>> result = remoteOaService.calculateLeaveDuration(startTime, endTime, SecurityConstants.INNER);
            if (result != null && result.getData() != null) {
                Map<String, Object> data = result.getData();
                double workHours = Double.parseDouble(data.get("workHours").toString());
                double workDays = Double.parseDouble(data.get("workDays").toString());
                
                StringBuilder sb = new StringBuilder();
                sb.append("请假时长计算结果：\n");
                sb.append("- 工作小时数：").append(String.format("%.2f", workHours)).append(" 小时\n");
                sb.append("- 工作天数：").append(String.format("%.2f", workDays)).append(" 天（按8小时/天计算）\n");
                
                // 如果有休息日列表，显示详细信息
                if (data.containsKey("restDays") && data.get("restDays") != null) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> restDays = (java.util.List<Map<String, Object>>) data.get("restDays");
                    if (!restDays.isEmpty()) {
                        sb.append("- 包含的休息日（共").append(restDays.size()).append("天）：\n");
                        for (Map<String, Object> restDay : restDays) {
                            sb.append("  * ").append(restDay.get("holidayDate"))
                              .append(" (").append(restDay.get("holidayName"))
                              .append(")\n");
                        }
                        sb.append("- 说明：以上休息日已从请假时长中排除，只计算工作日的工作时间（9:00-12:00, 13:00-18:00）\n");
                    } else {
                        sb.append("- 说明：时间范围内无休息日，已按工作日计算工作时间（9:00-12:00, 13:00-18:00）\n");
                    }
                }
                
                return sb.toString();
            }
        } catch (Exception e) {
            return "计算请假时长失败：" + e.getMessage();
        }
        return "无法计算请假时长";
    }
}
