package com.lingxi.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口
 * 
 * @author cloud
 */
public interface ISysFileService
{
    /**
     * 文件上传接口
     * 
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    public String uploadFile(MultipartFile file) throws Exception;

    /**
     * 生成当前可访问的文件地址。
     *
     * @param fileUrl 文件访问URL
     * @return 当前可访问地址
     */
    public String getAccessibleUrl(String fileUrl) throws Exception;

    /**
     * 解析文件存储对象标识。
     *
     * @param fileUrl 文件访问URL
     * @return 文件存储对象标识
     */
    public String parseFileKey(String fileUrl);

    /**
     * 规范化业务保存地址，去掉域名和临时签名参数。
     *
     * @param fileUrl 文件访问URL
     * @return 适合保存到业务表的稳定地址
     */
    public String normalizeFileUrl(String fileUrl);

    /**
     * 文件删除接口
     * 
     * @param fileUrl 文件访问URL
     * @throws Exception
     */
    public void deleteFile(String fileUrl) throws Exception;
}
