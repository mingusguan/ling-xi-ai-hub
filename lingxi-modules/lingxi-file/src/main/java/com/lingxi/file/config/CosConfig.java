package com.lingxi.file.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 配置信息。
 */
@Configuration
@ConfigurationProperties(prefix = "cos")
public class CosConfig {

    /**
     * SecretId。
     */
    private String secretId;

    /**
     * SecretKey。
     */
    private String secretKey;

    /**
     * 存储桶所在地域，例如 ap-guangzhou。
     */
    private String region;

    /**
     * 存储桶名称，格式通常为 bucket-appid。
     */
    private String bucketName;

    /**
     * 访问域名，不配置时使用 COS 默认域名。
     */
    private String domain;

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "file", name = "storage", havingValue = "cos")
    public COSClient cosClient() {
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(credentials, clientConfig);
    }
}
