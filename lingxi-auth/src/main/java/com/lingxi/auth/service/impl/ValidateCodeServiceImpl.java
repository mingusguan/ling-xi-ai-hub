package com.lingxi.auth.service.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;
import com.google.code.kaptcha.Producer;
import com.lingxi.auth.config.properties.CaptchaProperties;
import com.lingxi.auth.service.ValidateCodeService;
import com.lingxi.common.core.constant.CacheConstants;
import com.lingxi.common.core.constant.Constants;
import com.lingxi.common.core.exception.CaptchaException;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.utils.sign.Base64;
import com.lingxi.common.core.utils.uuid.IdUtils;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.redis.service.RedisService;

/**
 * 验证码实现处理。
 *
 * @author cloud
 */
@Service
public class ValidateCodeServiceImpl implements ValidateCodeService
{
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    @Autowired
    private RedisService redisService;

    @Autowired
    private CaptchaProperties captchaProperties;

    /**
     * 生成验证码。
     */
    @Override
    public AjaxResult createCaptcha() throws IOException, CaptchaException
    {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = Boolean.TRUE.equals(captchaProperties.getEnabled());
        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled)
        {
            return ajax;
        }

        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        String capStr;
        String code;
        BufferedImage image;

        String captchaType = StringUtils.nvl(captchaProperties.getType(), "math");
        if ("char".equals(captchaType))
        {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }
        else
        {
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(capStr);
        }

        redisService.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        ImageIO.write(image, "jpg", os);

        ajax.put("uuid", uuid);
        ajax.put("img", Base64.encode(os.toByteArray()));
        return ajax;
    }

    /**
     * 校验验证码。
     */
    @Override
    public void checkCaptcha(String code, String uuid) throws CaptchaException
    {
        if (!Boolean.TRUE.equals(captchaProperties.getEnabled()))
        {
            return;
        }
        if (StringUtils.isEmpty(code))
        {
            throw new CaptchaException("验证码不能为空");
        }

        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisService.getCacheObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaException("验证码已失效");
        }
        redisService.deleteObject(verifyKey);
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException("验证码错误");
        }
    }
}
