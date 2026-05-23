package com.lingxi.file.service;

import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.utils.file.FileUrlUtils;
import com.lingxi.file.config.CosConfig;
import com.lingxi.file.utils.FileUploadUtils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * 鑵捐浜?COS 鏂囦欢瀛樺偍銆? */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "file", name = "storage", havingValue = "cos")
public class CosSysFileServiceImpl implements ISysFileService {

    private final CosConfig cosConfig;

    private final COSClient cosClient;
    public CosSysFileServiceImpl(CosConfig cosConfig, COSClient cosClient) {
        this.cosConfig = cosConfig;
        this.cosClient = cosClient;
    }

    /**
     * 涓婁紶鏂囦欢鍒拌吘璁簯 COS銆?     *
     * @param file 涓婁紶鐨勬枃浠?     * @return 璁块棶鍦板潃
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception {
        InputStream inputStream = null;
        String fileName = null;
        try {
            fileName = FileUploadUtils.extractFilename(file);
            inputStream = file.getInputStream();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest request = new PutObjectRequest(cosConfig.getBucketName(), fileName, inputStream, metadata);
            cosClient.putObject(request);
            return getAccessibleUrl(fileName);
        } catch (CosServiceException e) {
            log.error("Tencent COS failed to upload file, bucket={}, region={}, objectKey={}, statusCode={}, errorCode={}, requestId={}",
                    cosConfig.getBucketName(), cosConfig.getRegion(), fileName, e.getStatusCode(), e.getErrorCode(), e.getRequestId(), e);
            throw new RuntimeException("Tencent COS failed to upload file", e);
        } catch (Exception e) {
            log.error("Tencent COS failed to upload file", e);
            throw new RuntimeException("Tencent COS failed to upload file", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * 为私有桶对象生成短时效签名地址。
     *
     * @param fileUrl 业务访问地址或对象 key
     * @return COS 短时效签名访问地址
     */
    @Override
    public String getAccessibleUrl(String fileUrl) {
        String objectKey = parseFileKey(fileUrl);
        Date expiration = new Date(System.currentTimeMillis() + resolveSignedUrlExpireMillis());
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(cosConfig.getBucketName(), objectKey, HttpMethodName.GET);
        request.setExpiration(expiration);
        return cosClient.generatePresignedUrl(request).toString();
    }

    @Override
    public String parseFileKey(String fileUrl) {
        return parseObjectKey(fileUrl);
    }

    @Override
    public String normalizeFileUrl(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return fileUrl;
        }
        String normalized = Arrays.stream(fileUrl.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(item -> FileUrlUtils.toPreviewPath(parseObjectKey(item)))
                .collect(Collectors.joining(","));
        return StringUtils.isBlank(normalized) ? fileUrl : normalized;
    }

    /**
     * 浠庤吘璁簯 COS 鍒犻櫎鏂囦欢銆?     *
     * @param fileUrl 鏂囦欢璁块棶URL
     */
    @Override
    public void deleteFile(String fileUrl) throws Exception {
        try {
            String objectKey = parseObjectKey(fileUrl);
            cosClient.deleteObject(cosConfig.getBucketName(), objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Tencent COS failed to delete file", e);
        }
    }

    private long resolveSignedUrlExpireMillis() {
        Integer expireSeconds = cosConfig.getSignedUrlExpireSeconds();
        if (expireSeconds == null || expireSeconds <= 0) {
            expireSeconds = 600;
        }
        return expireSeconds * 1000L;
    }

    private String buildDefaultCosDomain() {
        return String.format("%s.cos.%s.myqcloud.com", cosConfig.getBucketName(), cosConfig.getRegion());
    }

    private String normalizeDomain(String domain) {
        if (StringUtils.isEmpty(domain)) {
            return StringUtils.EMPTY;
        }
        domain = trimEndSlash(domain);
        if (domain.startsWith("https://")) {
            return domain.substring("https://".length());
        }
        if (domain.startsWith("http://")) {
            return domain.substring("http://".length());
        }
        return domain;
    }

    private String parseObjectKey(String fileUrl) {
        fileUrl = StringUtils.substringBefore(fileUrl, "?");
        String previewPrefix = "/file/preview/";
        if (StringUtils.contains(fileUrl, previewPrefix)) {
            return trimStartSlash(decodePath(StringUtils.substringAfter(fileUrl, previewPrefix)));
        }
        if (!StringUtils.contains(fileUrl, "://") && !StringUtils.startsWith(fileUrl, "/")) {
            return trimStartSlash(decodePath(fileUrl));
        }
        String domain = cosConfig.getDomain();
        if (StringUtils.isNotEmpty(domain)) {
            String objectKey = StringUtils.substringAfter(fileUrl, normalizeDomain(domain));
            if (StringUtils.isNotEmpty(objectKey)) {
                return trimStartSlash(decodePath(objectKey));
            }
        }
        String objectKey = StringUtils.substringAfter(fileUrl, buildDefaultCosDomain());
        if (StringUtils.isNotEmpty(objectKey)) {
            return trimStartSlash(decodePath(objectKey));
        }
        objectKey = parseUriPath(fileUrl);
        if (StringUtils.isNotEmpty(objectKey)) {
            return trimStartSlash(objectKey);
        }
        return trimStartSlash(FilenameUtils.separatorsToUnix(fileUrl));
    }

    private String parseUriPath(String fileUrl) {
        try {
            URI uri = URI.create(fileUrl);
            if (StringUtils.isNotEmpty(uri.getPath())) {
                return decodePath(uri.getPath());
            }
        } catch (Exception ignored) {
            return StringUtils.EMPTY;
        }
        return StringUtils.EMPTY;
    }

    private String decodePath(String path) {
        String decoded = path;
        for (int i = 0; i < 3; i++) {
            String next = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
            if (StringUtils.equals(next, decoded)) {
                return next;
            }
            decoded = next;
        }
        return decoded;
    }

    private String trimEndSlash(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String trimStartSlash(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value;
    }
}
