package com.lingxi.auth.service;

import java.io.IOException;
import com.lingxi.common.core.exception.CaptchaException;
import com.lingxi.common.core.web.domain.AjaxResult;

/**
 * 验证码处理。
 *
 * @author cloud
 */
public interface ValidateCodeService
{
    /**
     * 生成验证码。
     *
     * @return 验证码响应
     * @throws IOException 图片写入失败
     * @throws CaptchaException 验证码异常
     */
    AjaxResult createCaptcha() throws IOException, CaptchaException;

    /**
     * 校验验证码。
     *
     * @param code 用户输入的验证码
     * @param uuid 验证码唯一标识
     * @throws CaptchaException 验证码异常
     */
    void checkCaptcha(String code, String uuid) throws CaptchaException;
}
