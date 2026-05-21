package com.lingxi.auth.controller;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lingxi.auth.service.ValidateCodeService;
import com.lingxi.common.core.exception.CaptchaException;
import com.lingxi.common.core.web.domain.AjaxResult;

/**
 * 验证码接口。
 *
 * @author cloud
 */
@RestController
public class ValidateCodeController
{
    @Autowired
    private ValidateCodeService validateCodeService;

    /**
     * 兼容前端登录页原有 /code 地址。
     */
    @GetMapping("/code")
    public AjaxResult getCode() throws IOException, CaptchaException
    {
        return validateCodeService.createCaptcha();
    }
}
