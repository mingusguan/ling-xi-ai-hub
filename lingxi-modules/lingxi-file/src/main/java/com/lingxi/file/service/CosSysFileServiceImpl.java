package com.lingxi.file.service;

import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.file.config.CosConfig;
import com.lingxi.file.utils.FileUploadUtils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 腾讯云 COS 文件存储。
 */
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
     * 上传文件到腾讯云 COS。
     *
     * @param file 上传的文件
     * @return 访问地址
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception {
        InputStream inputStream = null;
        try {
            String fileName = FileUploadUtils.extractFilename(file);
            inputStream = file.getInputStream();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest request = new PutObjectRequest(cosConfig.getBucketName(), fileName, inputStream, metadata);
            cosClient.putObject(request);
            return buildFileUrl(fileName);
        } catch (Exception e) {
            throw new RuntimeException("Tencent COS failed to upload file", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * 从腾讯云 COS 删除文件。
     *
     * @param fileUrl 文件访问URL
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

    private String buildFileUrl(String fileName) {
        String domain = cosConfig.getDomain();
        if (StringUtils.isEmpty(domain)) {
            domain = String.format("https://%s.cos.%s.myqcloud.com", cosConfig.getBucketName(), cosConfig.getRegion());
        }
        return trimEndSlash(domain) + "/" + fileName;
    }

    private String parseObjectKey(String fileUrl) {
        String domain = cosConfig.getDomain();
        if (StringUtils.isNotEmpty(domain)) {
            String objectKey = StringUtils.substringAfter(fileUrl, trimEndSlash(domain));
            return trimStartSlash(objectKey);
        }
        String defaultDomain = String.format("%s.cos.%s.myqcloud.com", cosConfig.getBucketName(), cosConfig.getRegion());
        String objectKey = StringUtils.substringAfter(fileUrl, defaultDomain);
        return trimStartSlash(objectKey);
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
