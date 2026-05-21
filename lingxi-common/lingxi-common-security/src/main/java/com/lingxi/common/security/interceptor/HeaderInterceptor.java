package com.lingxi.common.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.context.SecurityContextHolder;
import com.lingxi.common.core.utils.JwtUtils;
import com.lingxi.common.core.utils.ServletUtils;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.security.auth.AuthUtil;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.model.LoginUser;

/**
 * 自定义请求头拦截器，将Header数据封装到线程变量中方便获取
 * 注意：此拦截器会同时验证当前用户有效期自动刷新有效期
 *
 * @author cloud
 */
public class HeaderInterceptor implements AsyncHandlerInterceptor
{
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (!(handler instanceof HandlerMethod))
        {
            return true;
        }

        SecurityContextHolder.setUserId(ServletUtils.getHeader(request, SecurityConstants.DETAILS_USER_ID));
        SecurityContextHolder.setUserName(ServletUtils.getHeader(request, SecurityConstants.DETAILS_USERNAME));
        SecurityContextHolder.setUserKey(ServletUtils.getHeader(request, SecurityConstants.USER_KEY));

        String token = SecurityUtils.getToken();
        if (StringUtils.isNotEmpty(token))
        {
            LoginUser loginUser = AuthUtil.getLoginUser(token);
            fillSecurityContextByToken(token, loginUser);
            if (StringUtils.isNotNull(loginUser))
            {
                AuthUtil.verifyLoginUserExpire(loginUser);
                SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
            }
        }
        return true;
    }

    /**
     * 单体直连时没有网关写入用户请求头，需要从token缓存回填当前用户上下文
     *
     * @param token 请求token
     * @param loginUser token对应的登录用户
     */
    private void fillSecurityContextByToken(String token, LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && SecurityContextHolder.getUserId() == null
                && StringUtils.isNotNull(loginUser.getUserid()))
        {
            SecurityContextHolder.setUserId(String.valueOf(loginUser.getUserid()));
        }
        if (StringUtils.isNotNull(loginUser) && StringUtils.isEmpty(SecurityContextHolder.getUserName())
                && StringUtils.isNotEmpty(loginUser.getUsername()))
        {
            SecurityContextHolder.setUserName(loginUser.getUsername());
        }
        fillSecurityContextByJwt(token);
    }

    /**
     * 单体直连时 Redis 登录缓存可能取不到，但 JWT 本身仍携带用户基础身份，用它兜底补齐上下文。
     *
     * @param token 请求token
     */
    private void fillSecurityContextByJwt(String token)
    {
        try
        {
            if (SecurityContextHolder.getUserId() == null)
            {
                SecurityContextHolder.setUserId(JwtUtils.getUserId(token));
            }
            if (StringUtils.isEmpty(SecurityContextHolder.getUserName()))
            {
                SecurityContextHolder.setUserName(JwtUtils.getUserName(token));
            }
            if (StringUtils.isEmpty(SecurityContextHolder.getUserKey()))
            {
                SecurityContextHolder.setUserKey(JwtUtils.getUserKey(token));
            }
        }
        catch (Exception ignored)
        {
            // 非法token交给后续鉴权流程处理，这里只负责尽量补齐请求上下文。
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception
    {
        SecurityContextHolder.remove();
    }
}
