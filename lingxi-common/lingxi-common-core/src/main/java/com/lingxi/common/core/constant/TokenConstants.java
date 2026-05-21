package com.lingxi.common.core.constant;

/**
 * Token 常量。
 *
 * @author cloud
 */
public class TokenConstants
{
    /**
     * 令牌前缀。
     */
    public static final String PREFIX = "Bearer ";

    /**
     * JWT 密钥环境变量名。
     */
    public static final String SECRET_ENV = "LINGXI_JWT_SECRET";

    /**
     * 本地开发默认占位密钥。
     */
    public static final String DEFAULT_SECRET = "change-me-in-production";

    private TokenConstants()
    {
    }
}
