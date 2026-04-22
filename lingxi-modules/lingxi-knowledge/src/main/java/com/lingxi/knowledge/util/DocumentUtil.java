package com.lingxi.knowledge.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;

public class DocumentUtil {

    /**
     * 从 MultipartFile 提取纯文本
     */
    public static String extractText(MultipartFile file) throws Exception {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try (InputStream is = file.getInputStream()) {
            return extractFromStream(is, contentType, fileName);
        }
    }

    /**
     * 从 URL下载并提取纯文本（用于重新向量化）
     */
    public static String extractTextFromUrl(String fileUrl) throws Exception {
        URL url = new URL(fileUrl);
        String lowerUrl = fileUrl.toLowerCase();
        try (InputStream is = url.openStream()) {
            String contentType = url.openConnection().getContentType();
            return extractFromStream(is, contentType, lowerUrl);
        }
    }

    private static String extractFromStream(InputStream is, String contentType, String fileNameOrUrl) throws Exception {
        if ("application/pdf".equals(contentType) || fileNameOrUrl.endsWith(".pdf")) {
            try (PDDocument pdf = PDDocument.load(is)) {
                return new PDFTextStripper().getText(pdf);
            }
        } else if (fileNameOrUrl.endsWith(".docx")) {
            try (XWPFDocument docx = new XWPFDocument(is)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph p : docx.getParagraphs()) {
                    sb.append(p.getText()).append("\n");
                }
                return sb.toString();
            }
        } else {
            return new String(is.readAllBytes(), "UTF-8");
        }
    }
}
