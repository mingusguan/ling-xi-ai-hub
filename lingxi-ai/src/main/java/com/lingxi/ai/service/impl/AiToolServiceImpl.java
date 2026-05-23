package com.lingxi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lingxi.ai.agent.DocumentWritingAgent;
import com.lingxi.ai.agent.ReportAnalysisAgent;
import com.lingxi.ai.domain.dto.DocumentWritingRequest;
import com.lingxi.ai.domain.dto.ReportInterpretRequest;
import com.lingxi.ai.domain.vo.DocumentWritingResult;
import com.lingxi.ai.domain.vo.ReportInterpretResult;
import com.lingxi.ai.governance.service.IAiGovernanceService;
import com.lingxi.ai.service.IAiToolService;
import com.lingxi.ai.util.ExcelTableParser;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.utils.StringUtils;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 工具服务实现。
 */
@Service
public class AiToolServiceImpl implements IAiToolService
{
    private static final Logger log = LoggerFactory.getLogger(AiToolServiceImpl.class);

    private static final Pattern FULL_DATE_WEEKDAY_PATTERN =
            Pattern.compile(
                    "(\\d{4})\u5e74(\\d{1,2})\u6708(\\d{1,2})\u65e5\uff08\u661f\u671f"
                            + "[\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5\u5929]\uff09");

    private static final Pattern MONTH_DATE_WEEKDAY_PATTERN =
            Pattern.compile(
                    "(?<!\u5e74)(\\d{1,2})\u6708(\\d{1,2})\u65e5\uff08\u661f\u671f"
                            + "[\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5\u5929]\uff09");

    private static final Pattern YEAR_PATTERN = Pattern.compile("(20\\d{2})\u5e74");

    private static final String DOCUMENT_FAILURE_MESSAGE = "公文助手处理失败";

    private static final String REPORT_FAILURE_MESSAGE = "报表解读处理失败";

    private static final List<String> DEFAULT_OUTLINE = List.of("核心事项", "执行要求", "保障措施");

    private static final List<String> DEFAULT_POLISH_POINTS = List.of("突出正式表达", "增强结构条理");

    private static final List<String> DEFAULT_KEY_FINDINGS = List.of("建议结合重点指标进一步核对异常波动");

    private static final List<String> DEFAULT_RISKS = List.of("当前报表未识别出显著高风险项，建议持续监控关键指标");

    private static final List<String> DEFAULT_SUGGESTIONS = List.of("建议针对关键指标建立定期复盘机制");

    private final DocumentWritingAgent documentWritingAgent;

    private final ReportAnalysisAgent reportAnalysisAgent;

    private final IAiGovernanceService governanceService;

    public AiToolServiceImpl(DocumentWritingAgent documentWritingAgent,
            ReportAnalysisAgent reportAnalysisAgent,
            IAiGovernanceService governanceService)
    {
        this.documentWritingAgent = documentWritingAgent;
        this.reportAnalysisAgent = reportAnalysisAgent;
        this.governanceService = governanceService;
    }

    @Override
    public DocumentWritingResult writeDocument(DocumentWritingRequest request)
    {
        long startTime = System.currentTimeMillis();
        try
        {
            DocumentWritingResult result = executeAiTool(DOCUMENT_FAILURE_MESSAGE, () -> doWriteDocument(request));
            governanceService.recordSuccess("AI_TOOL", "document_write", request == null ? null : request.getTopic(),
                    result == null ? null : result.getSummary(), System.currentTimeMillis() - startTime);
            return result;
        }
        catch (Exception e)
        {
            governanceService.recordFailure("AI_TOOL", "document_write", request == null ? null : request.getTopic(),
                    System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }

    @Override
    public ReportInterpretResult interpretReport(ReportInterpretRequest request, MultipartFile file)
    {
        long startTime = System.currentTimeMillis();
        try
        {
            ReportInterpretResult result = executeAiTool(REPORT_FAILURE_MESSAGE, () -> doInterpretReport(request, file));
            governanceService.recordSuccess("AI_TOOL", "report_interpret",
                    request == null ? null : request.getReportTitle(),
                    result == null ? null : result.getSummary(), System.currentTimeMillis() - startTime);
            return result;
        }
        catch (Exception e)
        {
            governanceService.recordFailure("AI_TOOL", "report_interpret",
                    request == null ? null : request.getReportTitle(),
                    System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }

    private DocumentWritingResult doWriteDocument(DocumentWritingRequest request)
    {
        validateDocumentRequest(request);

        String prompt = buildDocumentPrompt(request);
        String resultJson = documentWritingAgent.write(prompt);
        log.info("AI 公文写作结果：{}", resultJson);
        DocumentWritingResult result = parseDocumentResult(resultJson, request);
        normalizeDocumentWeekdays(result);
        return result;
    }

    private ReportInterpretResult doInterpretReport(ReportInterpretRequest request, MultipartFile file)
    {
        validateReportRequest(request);

        String parsedTable = resolveTableContent(request, file);
        String prompt = buildReportPrompt(request, parsedTable);
        String resultJson = reportAnalysisAgent.analyze(prompt);
        return parseReportResult(resultJson, request, parsedTable);
    }

    private <T> T executeAiTool(String failureMessage, Supplier<T> action)
    {
        try
        {
            return action.get();
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error(failureMessage, e);
            throw new ServiceException(failureMessage + "，请稍后重试");
        }
    }

    private void validateDocumentRequest(DocumentWritingRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("公文写作请求不能为空");
        }
    }

    private void validateReportRequest(ReportInterpretRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("报表解读请求不能为空");
        }
    }

    private String resolveTableContent(ReportInterpretRequest request, MultipartFile file)
    {
        if (file != null && !file.isEmpty())
        {
            return ExcelTableParser.parse(file);
        }
        if (StringUtils.isBlank(request.getTableContent()))
        {
            throw new ServiceException("请填写表格内容或上传报表文件");
        }
        return request.getTableContent().trim();
    }

    private String buildDocumentPrompt(DocumentWritingRequest request)
    {
        StringBuilder builder = new StringBuilder(2048);
        LocalDate currentDate = LocalDate.now();
        appendLine(builder, "【当前日期】" + currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                + "（" + toChineseWeekday(currentDate.getDayOfWeek()) + "）");
        appendLine(builder, "【日期校验要求】所有日期对应的星期必须真实准确，不能臆造星期。");
        appendLine(builder, "【任务类型】公文写作与润色");
        appendLine(builder, "【当前日期】" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        appendLine(builder, "【日期要求】如正文需要出现年份、日期或时间节点，必须以当前日期为基准；不要使用过期年份。");
        appendLine(builder, "【处理模式】" + request.getMode());
        appendLine(builder, "【文稿类型】" + request.getDocumentType());
        appendLine(builder, "【语气风格】" + request.getTone());
        appendLine(builder, "【主题】" + request.getTopic());
        appendOptionalSection(builder, "背景信息", request.getBackground());
        appendOptionalSection(builder, "原始内容", request.getSourceContent());
        appendOptionalSection(builder, "附加要求", request.getRequirements());
        builder.append("【输出要求】请生成适合正式办公场景直接使用的中文结果。");
        return builder.toString();
    }

    private String buildReportPrompt(ReportInterpretRequest request, String parsedTable)
    {
        StringBuilder builder = new StringBuilder(4096);
        appendLine(builder, "【任务类型】报表解读");
        appendLine(builder, "【报表名称】" + request.getReportTitle());
        appendOptionalSection(builder, "业务背景", request.getBusinessContext());
        appendOptionalSection(builder, "分析重点", request.getAnalysisFocus());
        appendRequiredSection(builder, "报表内容", parsedTable);
        builder.append("【输出要求】请重点关注异常、趋势、风险、排名变化与管理建议。");
        return builder.toString();
    }

    private void appendLine(StringBuilder builder, String content)
    {
        builder.append(content).append('\n');
    }

    private void appendOptionalSection(StringBuilder builder, String title, String content)
    {
        if (StringUtils.isBlank(content))
        {
            return;
        }
        appendRequiredSection(builder, title, content.trim());
    }

    private void appendRequiredSection(StringBuilder builder, String title, String content)
    {
        builder.append('【').append(title).append("】\n")
                .append(content)
                .append('\n');
    }

    private DocumentWritingResult parseDocumentResult(String resultJson, DocumentWritingRequest request)
    {
        try
        {
            JSONObject jsonObject = JSON.parseObject(resultJson);
            DocumentWritingResult result = buildDocumentResult(jsonObject, request);
            fillDocumentDefaults(result);
            return result;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.warn("解析公文助手结果失败，原始结果: {}", resultJson, e);
            throw new ServiceException("公文助手返回结果解析失败");
        }
    }

    private DocumentWritingResult buildDocumentResult(JSONObject jsonObject, DocumentWritingRequest request)
    {
        DocumentWritingResult result = new DocumentWritingResult();
        result.setTitle(defaultIfBlank(jsonObject.getString("title"), request.getTopic()));
        result.setSummary(defaultIfBlank(jsonObject.getString("summary"), "已生成公文内容摘要"));
        result.setDocumentType(defaultIfBlank(jsonObject.getString("documentType"), request.getDocumentType()));
        result.setTone(defaultIfBlank(jsonObject.getString("tone"), request.getTone()));
        result.setContent(defaultIfBlank(jsonObject.getString("content"), request.getSourceContent()));
        result.setOutline(toStringList(jsonObject.getJSONArray("outline")));
        result.setPolishPoints(toStringList(jsonObject.getJSONArray("polishPoints")));
        return result;
    }

    private void fillDocumentDefaults(DocumentWritingResult result)
    {
        if (StringUtils.isBlank(result.getContent()))
        {
            throw new ServiceException("AI 未返回有效公文内容");
        }
        if (result.getOutline().isEmpty())
        {
            result.setOutline(DEFAULT_OUTLINE);
        }
        if (result.getPolishPoints().isEmpty())
        {
            result.setPolishPoints(DEFAULT_POLISH_POINTS);
        }
    }

    private ReportInterpretResult parseReportResult(String resultJson, ReportInterpretRequest request, String parsedTable)
    {
        try
        {
            JSONObject jsonObject = JSON.parseObject(resultJson);
            ReportInterpretResult result = buildReportResult(jsonObject, request, parsedTable);
            fillReportDefaults(result);
            return result;
        }
        catch (Exception e)
        {
            log.warn("解析报表解读结果失败，原始结果: {}", resultJson, e);
            throw new ServiceException("报表解读返回结果解析失败");
        }
    }

    private ReportInterpretResult buildReportResult(JSONObject jsonObject, ReportInterpretRequest request, String parsedTable)
    {
        ReportInterpretResult result = new ReportInterpretResult();
        result.setReportTitle(defaultIfBlank(jsonObject.getString("reportTitle"), request.getReportTitle()));
        result.setSummary(defaultIfBlank(jsonObject.getString("summary"), "已完成报表摘要"));
        result.setKeyFindings(toStringList(jsonObject.getJSONArray("keyFindings")));
        result.setRisks(toStringList(jsonObject.getJSONArray("risks")));
        result.setSuggestions(toStringList(jsonObject.getJSONArray("suggestions")));
        result.setTrendAnalysis(defaultIfBlank(jsonObject.getString("trendAnalysis"), "已完成趋势分析"));
        result.setManagementBrief(defaultIfBlank(jsonObject.getString("managementBrief"), "已完成管理汇报摘要"));
        result.setParsedTablePreview(parsedTable);
        return result;
    }

    private void fillReportDefaults(ReportInterpretResult result)
    {
        if (result.getKeyFindings().isEmpty())
        {
            result.setKeyFindings(DEFAULT_KEY_FINDINGS);
        }
        if (result.getRisks().isEmpty())
        {
            result.setRisks(DEFAULT_RISKS);
        }
        if (result.getSuggestions().isEmpty())
        {
            result.setSuggestions(DEFAULT_SUGGESTIONS);
        }
    }

    private void normalizeDocumentWeekdays(DocumentWritingResult result)
    {
        int referenceYear = resolveReferenceYear(result);
        result.setTitle(normalizeWeekdays(result.getTitle(), referenceYear));
        result.setSummary(normalizeWeekdays(result.getSummary(), referenceYear));
        result.setContent(normalizeWeekdays(result.getContent(), referenceYear));
        result.setOutline(normalizeWeekdayList(result.getOutline(), referenceYear));
        result.setPolishPoints(normalizeWeekdayList(result.getPolishPoints(), referenceYear));
    }

    private List<String> normalizeWeekdayList(List<String> values, int referenceYear)
    {
        List<String> normalized = new ArrayList<>();
        if (values == null)
        {
            return normalized;
        }
        for (String value : values)
        {
            normalized.add(normalizeWeekdays(value, referenceYear));
        }
        return normalized;
    }

    private int resolveReferenceYear(DocumentWritingResult result)
    {
        String text = String.join("\n",
                defaultIfBlank(result.getTitle(), ""),
                defaultIfBlank(result.getSummary(), ""),
                defaultIfBlank(result.getContent(), ""));
        Matcher matcher = YEAR_PATTERN.matcher(text);
        if (matcher.find())
        {
            return Integer.parseInt(matcher.group(1));
        }
        return LocalDate.now().getYear();
    }

    private String normalizeWeekdays(String text, int referenceYear)
    {
        if (StringUtils.isBlank(text))
        {
            return text;
        }
        String normalized = replaceFullDateWeekdays(text);
        return replaceMonthDateWeekdays(normalized, referenceYear);
    }

    private String replaceFullDateWeekdays(String text)
    {
        Matcher matcher = FULL_DATE_WEEKDAY_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find())
        {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            String replacement = year + "年" + month + "月" + day + "日（"
                    + toChineseWeekday(LocalDate.of(year, month, day).getDayOfWeek()) + "）";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String replaceMonthDateWeekdays(String text, int referenceYear)
    {
        Matcher matcher = MONTH_DATE_WEEKDAY_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find())
        {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            String replacement = month + "月" + day + "日（"
                    + toChineseWeekday(LocalDate.of(referenceYear, month, day).getDayOfWeek()) + "）";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String toChineseWeekday(DayOfWeek dayOfWeek)
    {
        return switch (dayOfWeek)
        {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }

    private List<String> toStringList(JSONArray jsonArray)
    {
        List<String> results = new ArrayList<>();
        if (jsonArray == null)
        {
            return results;
        }
        for (Object item : jsonArray)
        {
            String value = item == null ? "" : String.valueOf(item).trim();
            if (StringUtils.isNotBlank(value))
            {
                results.add(value);
            }
        }
        return results;
    }

    private String defaultIfBlank(String value, String defaultValue)
    {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }
}
