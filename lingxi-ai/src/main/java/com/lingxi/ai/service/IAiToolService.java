package com.lingxi.ai.service;

import com.lingxi.ai.domain.dto.DocumentWritingRequest;
import com.lingxi.ai.domain.dto.ReportInterpretRequest;
import com.lingxi.ai.domain.vo.DocumentWritingResult;
import com.lingxi.ai.domain.vo.ReportInterpretResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 工具服务接口。
 */
public interface IAiToolService
{
    /**
     * 公文写作与润色。
     *
     * @param request 请求参数
     * @return 结果
     */
    DocumentWritingResult writeDocument(DocumentWritingRequest request);

    /**
     * 报表解读。
     *
     * @param request 请求参数
     * @param file 报表文件，可为空
     * @return 结果
     */
    ReportInterpretResult interpretReport(ReportInterpretRequest request, MultipartFile file);
}
