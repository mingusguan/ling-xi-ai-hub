package com.lingxi.auth.config;

import java.security.SecureRandom;
import com.google.code.kaptcha.text.impl.DefaultTextCreator;

/**
 * 数学验证码文本生成器。
 *
 * @author cloud
 */
public class KaptchaTextCreator extends DefaultTextCreator
{
    private static final String[] CNUMBERS = "0,1,2,3,4,5,6,7,8,9,10".split(",");

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String getText()
    {
        int result;
        int x = RANDOM.nextInt(10);
        int y = RANDOM.nextInt(10);
        StringBuilder captchaText = new StringBuilder();
        int randomOperands = RANDOM.nextInt(3);
        if (randomOperands == 0)
        {
            result = x * y;
            captchaText.append(CNUMBERS[x]);
            captchaText.append("*");
            captchaText.append(CNUMBERS[y]);
        }
        else if (randomOperands == 1)
        {
            if ((x != 0) && y % x == 0)
            {
                result = y / x;
                captchaText.append(CNUMBERS[y]);
                captchaText.append("/");
                captchaText.append(CNUMBERS[x]);
            }
            else
            {
                result = x + y;
                captchaText.append(CNUMBERS[x]);
                captchaText.append("+");
                captchaText.append(CNUMBERS[y]);
            }
        }
        else
        {
            if (x >= y)
            {
                result = x - y;
                captchaText.append(CNUMBERS[x]);
                captchaText.append("-");
                captchaText.append(CNUMBERS[y]);
            }
            else
            {
                result = y - x;
                captchaText.append(CNUMBERS[y]);
                captchaText.append("-");
                captchaText.append(CNUMBERS[x]);
            }
        }
        captchaText.append("=?@").append(result);
        return captchaText.toString();
    }
}
