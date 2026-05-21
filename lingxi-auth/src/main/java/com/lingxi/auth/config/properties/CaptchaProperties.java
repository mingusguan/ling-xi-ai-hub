package com.lingxi.auth.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 验证码配置。
 *
 * @author cloud
 */
@Configuration
@ConfigurationProperties(prefix = "security.captcha")
public class CaptchaProperties
{
    /**
     * 验证码开关。
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * 验证码类型，math 为数学计算，char 为字符。
     */
    private String type = "math";

    public Boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
