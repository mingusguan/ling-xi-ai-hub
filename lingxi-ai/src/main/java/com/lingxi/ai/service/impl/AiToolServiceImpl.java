package com.lingxi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lingxi.ai.agent.DocumentWritingAgent;
import com.lingxi.ai.agent.ReportAnalysisAgent;
import com.lingxi.ai.domain.dto.DocumentWritingRequest;
import com.lingxi.ai.domain.dto.ReportInterpretRequest;
import com.lingxi.ai.domain.vo.AiMindmapPayload;
import com.lingxi.ai.domain.vo.DocumentWritingResult;
import com.lingxi.ai.domain.vo.ReportInterpretResult;
import com.lingxi.ai.service.IAiToolService;
import com.lingxi.ai.service.IMindmapService;
import com.lingxi.ai.util.ExcelTableParser;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
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
    private static final String DEFAULT_LAYOUT = "logical";

    private final DocumentWritingAgent documentWritingAgent;

    private final ReportAnalysisAgent reportAnalysisAgent;

    private final IMindmapService mindmapService;

    public AiToolServiceImpl(DocumentWritingAgent documentWritingAgent,
            ReportAnalysisAgent reportAnalysisAgent,
            IMindmapService mindmapService)
    {
        this.documentWritingAgent = documentWritingAgent;
        this.reportAnalysisAgent = reportAnalysisAgent;
        this.mindmapService = mindmapService;
    }

    @Override
    public DocumentWritingResult writeDocument(DocumentWritingRequest request)
    {
        try
        {
            String prompt = buildDocumentPrompt(request);
            String resultJson = documentWritingAgent.write(prompt);
            DocumentWritingResult result = parseDocumentResult(resultJson, request);
            attachMindmapIfNeeded(Boolean.TRUE.equals(request.getGenerateMindmap()),
                    request.getMindmapLayoutType(),
                    result.getMindmapPrompt(),
                    result::setMindmap);
            return result;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("公文助手处理失败", e);
            throw new ServiceException("公文助手处理失败，请稍后重试");
        }
    }

    @Override
    public ReportInterpretResult interpretReport(ReportInterpretRequest request, MultipartFile file)
    {
        try
        {
            String parsedTable = resolveTableContent(request, file);
            String prompt = buildReportPrompt(request, parsedTable);
            String resultJson = reportAnalysisAgent.analyze(prompt);
            ReportInterpretResult result = parseReportResult(resultJson, request, parsedTable);
            attachMindmapIfNeeded(Boolean.TRUE.equals(request.getGenerateMindmap()),
                    request.getMindmapLayoutType(),
                    result.getMindmapPrompt(),
                    result::setMindmap);
            return result;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("报表解读处理失败", e);
            throw new ServiceException("报表解读处理失败，请稍后重试");
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
        builder.append("【任务类型】公文写作与润色\n");
        builder.append("【处理模式】").append(request.getMode()).append('\n');
        builder.append("【文稿类型】").append(request.getDocumentType()).append('\n');
        builder.append("【语气风格】").append(request.getTone()).append('\n');
        builder.append("【主题】").append(request.getTopic()).append('\n');
        if (StringUtils.isNotBlank(request.getBackground()))
        {
            builder.append("【背景信息】\n").append(request.getBackground().trim()).append('\n');
        }
        if (StringUtils.isNotBlank(request.getSourceContent()))
        {
            builder.append("【原始内容】\n").append(request.getSourceContent().trim()).append('\n');
        }
        if (StringUtils.isNotBlank(request.getRequirements()))
        {
            builder.append("【附加要求】\n").append(request.getRequirements().trim()).append('\n');
        }
        builder.append("【输出要求】请生成适合正式办公场景直接使用的中文结果。");
        return builder.toString();
    }

    private String buildReportPrompt(ReportInterpretRequest request, String parsedTable)
    {
        StringBuilder builder = new StringBuilder(4096);
        builder.append("【任务类型】报表解读\n");
        builder.append("【报表名称】").append(request.getReportTitle()).append('\n');
        if (StringUtils.isNotBlank(request.getBusinessContext()))
        {
            builder.append("【业务背景】\n").append(request.getBusinessContext().trim()).append('\n');
        }
        if (StringUtils.isNotBlank(request.getAnalysisFocus()))
        {
            builder.append("【分析重点】\n").append(request.getAnalysisFocus().trim()).append('\n');
        }
        builder.append("【报表内容】\n").append(parsedTable).append('\n');
        builder.append("【输出要求】请重点关注异常、趋势、风险、排名变化与管理建议。");
        return builder.toString();
    }

    private DocumentWritingResult parseDocumentResult(String resultJson, DocumentWritingRequest request)
    {
        try
        {
            JSONObject jsonObject = JSON.parseObject(resultJson);
            DocumentWritingResult result = new DocumentWritingResult();
            result.setTitle(StringUtils.defaultIfBlank(jsonObject.getString("title"), request.getTopic()));
            result.setSummary(StringUtils.defaultIfBlank(jsonObject.getString("summary"), "已生成公文内容摘要"));
            result.setDocumentType(StringUtils.defaultIfBlank(jsonObject.getString("documentType"), request.getDocumentType()));
            result.setTone(StringUtils.defaultIfBlank(jsonObject.getString("tone"), request.getTone()));
            result.setContent(StringUtils.defaultIfBlank(jsonObject.getString("content"), request.getSourceContent()));
            result.setOutline(toStringList(jsonObject.getJSONArray("outline")));
            result.setPolishPoints(toStringList(jsonObject.getJSONArray("polishPoints")));
            result.setMindmapPrompt(StringUtils.defaultIfBlank(jsonObject.getString("mindmapPrompt"),
                    result.getTitle() + " 思维导图"));
            if (StringUtils.isBlank(result.getContent()))
            {
                throw new ServiceException("AI 未返回有效公文内容");
            }
            if (result.getOutline().isEmpty())
            {
                result.setOutline(List.of("核心事项", "执行要求", "保障措施"));
            }
            if (result.getPolishPoints().isEmpty())
            {
                result.setPolishPoints(List.of("突出正式表达", "增强结构条理"));
            }
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

    private ReportInterpretResult parseReportResult(String resultJson, ReportInterpretRequest request, String parsedTable)
    {
        try
        {
            JSONObject jsonObject = JSON.parseObject(resultJson);
            ReportInterpretResult result = new ReportInterpretResult();
            result.setReportTitle(StringUtils.defaultIfBlank(jsonObject.getString("reportTitle"), request.getReportTitle()));
            result.setSummary(StringUtils.defaultIfBlank(jsonObject.getString("summary"), "已完成报表摘要"));
            result.setKeyFindings(toStringList(jsonObject.getJSONArray("keyFindings")));
            result.setRisks(toStringList(jsonObject.getJSONArray("risks")));
            result.setSuggestions(toStringList(jsonObject.getJSONArray("suggestions")));
            result.setTrendAnalysis(StringUtils.defaultIfBlank(jsonObject.getString("trendAnalysis"), "已完成趋势分析"));
            result.setManagementBrief(StringUtils.defaultIfBlank(jsonObject.getString("managementBrief"), "已完成管理汇报摘要"));
            result.setMindmapPrompt(StringUtils.defaultIfBlank(jsonObject.getString("mindmapPrompt"),
                    result.getReportTitle() + " 解读思维导图"));
            result.setParsedTablePreview(parsedTable);
            if (result.getKeyFindings().isEmpty())
            {
                result.setKeyFindings(List.of("建议结合重点指标进一步核对异常波动"));
            }
            if (result.getRisks().isEmpty())
            {
                result.setRisks(List.of("当前报表未识别出显著高风险项，建议持续监控关键指标"));
            }
            if (result.getSuggestions().isEmpty())
            {
                result.setSuggestions(List.of("建议针对关键指标建立定期复盘机制"));
            }
            return result;
        }
        catch (Exception e)
        {
            log.warn("解析报表解读结果失败，原始结果: {}", resultJson, e);
            throw new ServiceException("报表解读返回结果解析失败");
        }
    }

    private void attachMindmapIfNeeded(boolean generateMindmap,
            String layoutType,
            String mindmapPrompt,
            java.util.function.Consumer<AiMindmapPayload> consumer)
    {
        if (!generateMindmap)
        {
            return;
        }
        String actualPrompt = StringUtils.defaultIfBlank(mindmapPrompt, "业务主题思维导图");
        String actualLayout = StringUtils.defaultIfBlank(layoutType, DEFAULT_LAYOUT);
        String dataJson = mindmapService.generateMindmapByAI(actualPrompt, actualLayout);
        AiMindmapPayload payload = new AiMindmapPayload();
        payload.setDataJson(dataJson);
        payload.setLayoutType(actualLayout);
        payload.setTopic(extractTopic(dataJson, actualPrompt));
        consumer.accept(payload);
    }

    private String extractTopic(String dataJson, String fallback)
    {
        try
        {
            JSONObject jsonObject = JSON.parseObject(dataJson);
            return StringUtils.defaultIfBlank(jsonObject.getString("topic"), fallback);
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private List<String> toStringList(com.alibaba.fastjson2.JSONArray jsonArray)
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
}
