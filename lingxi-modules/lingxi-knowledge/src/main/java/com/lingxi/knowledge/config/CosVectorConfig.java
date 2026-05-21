package com.lingxi.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 向量桶配置。
 */
@Configuration
@ConfigurationProperties(prefix = "cos-vector")
public class CosVectorConfig {

    /**
     * SecretId。
     */
    private String secretId;

    /**
     * SecretKey。
     */
    private String secretKey;

    /**
     * 临时密钥 Token，使用永久密钥时可为空。
     */
    private String sessionToken;

    /**
     * 向量桶地域，例如 ap-guangzhou。
     */
    private String region;

    /**
     * 向量桶名称，格式通常为 bucket-appid。
     */
    private String bucketName;

    /**
     * 向量索引名称。
     */
    private String indexName;

    /**
     * 向量桶访问域名，不配置时使用 vectors.${region}.coslake.com。
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

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
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

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
