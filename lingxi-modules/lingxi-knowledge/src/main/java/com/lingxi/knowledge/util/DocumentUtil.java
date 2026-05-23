package com.lingxi.knowledge.util;

import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.constant.TokenConstants;
import com.lingxi.common.core.utils.ServletUtils;
import com.lingxi.common.core.utils.SpringUtils;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.utils.file.FileUrlUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.core.env.Environment;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLConnection;
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
            "https://localhost",
            "/file/preview/"
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
        String accessUrl = buildReEmbedAccessUrl(fileUrl);
        validateReEmbedUrl(accessUrl);
        URL url = URI.create(accessUrl).toURL();
        String lowerUrl = fileUrl.toLowerCase();
        URLConnection connection = url.openConnection();
        appendCurrentToken(connection);
        try (InputStream is = connection.getInputStream()) {
            String contentType = connection.getContentType();
            return extractFromStream(is, contentType, lowerUrl);
        }
    }

    private static void validateReEmbedUrl(String fileUrl)
    {
        if (StringUtils.isBlank(fileUrl)) {
            throw new ServiceException("文件地址不能为空");
        }
        boolean allowed = ALLOWED_REEMBED_PREFIXES.stream().anyMatch(fileUrl::startsWith);
        Environment environment = SpringUtils.getBean(Environment.class);
        String fileDomain = environment.getProperty("file.domain");
        allowed = allowed || (StringUtils.isNotEmpty(fileDomain) && fileUrl.startsWith(fileDomain));
        if (!allowed) {
            throw new ServiceException("文件地址不在允许的回源范围内");
        }
    }

    private static String buildReEmbedAccessUrl(String fileUrl)
    {
        if (StringUtils.isBlank(fileUrl)) {
            return fileUrl;
        }
        if (StringUtils.startsWith(fileUrl, "http://") || StringUtils.startsWith(fileUrl, "https://")) {
            return fileUrl;
        }
        Environment environment = SpringUtils.getBean(Environment.class);
        String port = environment.getProperty("server.port", "8080");
        return "http://127.0.0.1:" + port + FileUrlUtils.toPreviewPath(fileUrl);
    }

    /**
     * 重新向量化从私有文件预览地址回读时，需要复用当前请求登录态。
     */
    private static void appendCurrentToken(URLConnection connection)
    {
        try {
            String token = ServletUtils.getRequest().getHeader(SecurityConstants.AUTHORIZATION_HEADER);
            if (StringUtils.isNotEmpty(token)) {
                connection.setRequestProperty(SecurityConstants.AUTHORIZATION_HEADER, token);
                return;
            }
            String cookie = ServletUtils.getRequest().getHeader("Cookie");
            if (StringUtils.isNotEmpty(cookie)) {
                connection.setRequestProperty("Cookie", cookie);
                return;
            }
            String rawToken = com.lingxi.common.security.utils.SecurityUtils.getToken();
            if (StringUtils.isNotEmpty(rawToken)) {
                connection.setRequestProperty(SecurityConstants.AUTHORIZATION_HEADER, TokenConstants.PREFIX + rawToken);
            }
        } catch (Exception ignored) {
            // 非请求线程触发重新向量化时不强制附加登录态，交给目标地址自身权限处理。
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
