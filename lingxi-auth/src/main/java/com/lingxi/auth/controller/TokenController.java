package com.lingxi.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lingxi.auth.form.LoginBody;
import com.lingxi.auth.form.RegisterBody;
import com.lingxi.auth.service.SysLoginService;
import com.lingxi.auth.service.ValidateCodeService;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.utils.JwtUtils;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.security.auth.AuthUtil;
import com.lingxi.common.security.service.TokenService;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.model.LoginUser;

/**
 * token 控制
 * 
 * @author cloud
 */
@RestController
@RequestMapping("/auth")
public class TokenController
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysLoginService sysLoginService;

    @Autowired
    private ValidateCodeService validateCodeService;

    @PostMapping("login")
    public R<?> login(@RequestBody LoginBody form)
    {
        validateCodeService.checkCaptcha(form.getCode(), form.getUuid());
        // 用户登录
        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword());
        // 获取登录token
        return R.ok(tokenService.createToken(userInfo));
    }

    @DeleteMapping("logout")
    public R<?> logout(HttpServletRequest request)
    {
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            String username = JwtUtils.getUserName(token);
            // 删除用户缓存记录
            AuthUtil.logoutByToken(token);
            // 记录用户退出日志
            sysLoginService.logout(username);
        }
        return R.ok();
    }

    @PostMapping("refresh")
    public R<?> refresh(HttpServletRequest request)
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            // 刷新令牌有效期
            tokenService.refreshToken(loginUser);
            return R.ok();
        }
        return R.ok();
    }

    @PostMapping("register")
    public R<?> register(@RequestBody RegisterBody registerBody)
    {
        validateCodeService.checkCaptcha(registerBody.getCode(), registerBody.getUuid());
        // 用户注册
        sysLoginService.register(registerBody.getUsername(), registerBody.getPassword());
        return R.ok();
    }
}
