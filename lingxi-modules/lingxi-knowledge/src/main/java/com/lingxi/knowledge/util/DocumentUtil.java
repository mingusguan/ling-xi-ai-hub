package com.lingxi.knowledge.util;

import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.utils.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DocumentUtil
{
    private static final List<String> ALLOWED_REEMBED_PREFIXES = List.of(
            "http://127.0.0.1",
            "http://localhost",
            "https://127.0.0.1",
            "https://localhost"
    );

    private DocumentUtil()
    {
    }

    /**
     * 从 MultipartFile 提取纯文本。
     */
    public static String extractText(MultipartFile file) throws Exception
    {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try (InputStream is = file.getInputStream()) {
            return extractFromStream(is, contentType, fileName);
        }
    }

    /**
     * 从已登记文件地址重新提取文本。
     */
    public static String extractTextFromUrl(String fileUrl) throws Exception
    {
        validateReEmbedUrl(fileUrl);
        URL url = URI.create(fileUrl).toURL();
        String lowerUrl = fileUrl.toLowerCase();
        try (InputStream is = url.openStream()) {
            String contentType = url.openConnection().getContentType();
            return extractFromStream(is, contentType, lowerUrl);
        }
    }

    private static void validateReEmbedUrl(String fileUrl)
    {
        if (StringUtils.isBlank(fileUrl)) {
            throw new ServiceException("文件地址不能为空");
        }
        boolean allowed = ALLOWED_REEMBED_PREFIXES.stream().anyMatch(fileUrl::startsWith);
        if (!allowed) {
            throw new ServiceException("文件地址不在允许的回源范围内");
        }
    }

    private static String extractFromStream(InputStream is, String contentType, String fileNameOrUrl) throws Exception
    {
        if ("application/pdf".equals(contentType) || fileNameOrUrl.endsWith(".pdf")) {
            try (PDDocument pdf = PDDocument.load(is)) {
                return new PDFTextStripper().getText(pdf);
            }
        }
        if (fileNameOrUrl.endsWith(".docx")) {
            try (XWPFDocument docx = new XWPFDocument(is)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph paragraph : docx.getParagraphs()) {
                    sb.append(paragraph.getText()).append("\n");
                }
                return sb.toString();
            }
        }
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
