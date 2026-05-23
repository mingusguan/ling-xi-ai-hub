package com.lingxi.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.utils.file.FileUtils;
import com.lingxi.common.core.utils.file.FileUrlUtils;
import com.lingxi.file.utils.FileUploadUtils;

/**
 * 本地文件存储
 * 
 * @author cloud
 */
@Primary
@Service
@ConditionalOnProperty(prefix = "file", name = "storage", havingValue = "local", matchIfMissing = true)
public class LocalSysFileServiceImpl implements ISysFileService
{
    /**
     * 资源映射路径 前缀
     */
    @Value("${file.prefix}")
    public String localFilePrefix;

    /**
     * 域名或本机访问地址
     */
    @Value("${file.domain}")
    public String domain;
    
    /**
     * 上传文件存储在本地的根路径
     */
    @Value("${file.path}")
    private String localFilePath;

    /**
     * 本地文件上传接口
     * 
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        String name = FileUploadUtils.upload(localFilePath, file);
        String url = domain + localFilePrefix + name;
        return url;
    }

    @Override
    public String getAccessibleUrl(String fileUrl)
    {
        return fileUrl;
    }

    @Override
    public String parseFileKey(String fileUrl)
    {
        return StringUtils.substringAfter(fileUrl, localFilePrefix);
    }

    @Override
    public String normalizeFileUrl(String fileUrl)
    {
        return FileUrlUtils.normalizeForStorage(fileUrl);
    }

    /**
     * 本地文件删除接口
     * 
     * @param fileUrl 文件访问URL
     * @throws Exception
     */
    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        String localFile = parseFileKey(fileUrl);
        FileUtils.deleteFile(localFilePath + localFile);
    }

}
