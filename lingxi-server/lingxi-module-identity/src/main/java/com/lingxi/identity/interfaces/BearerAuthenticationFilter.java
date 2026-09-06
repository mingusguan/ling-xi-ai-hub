package com.lingxi.identity.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.AuthenticatedSession;
import com.lingxi.identity.api.AdminAuthorizationFacade;
import com.lingxi.identity.api.AuthenticationFacade;
import com.lingxi.kernel.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 统一校验 PC Web 与 HarmonyOS 使用的 Bearer Token。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class BearerAuthenticationFilter extends OncePerRequestFilter {
  private final AuthenticationFacade authenticationFacade;
  private final AdminAuthorizationFacade adminAuthorizationFacade;
  private final ObjectMapper objectMapper;

  public BearerAuthenticationFilter(
      AuthenticationFacade authenticationFacade,
      AdminAuthorizationFacade adminAuthorizationFacade,
      ObjectMapper objectMapper) {
    this.authenticationFacade = authenticationFacade;
    this.adminAuthorizationFacade = adminAuthorizationFacade;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    try {
      if (authorization != null && authorization.startsWith("Bearer ")) {
        String requestId = String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID));
        String token = authorization.substring(7).trim();
        if (request.getRequestURI().startsWith("/admin-api/")) {
          var admin = adminAuthorizationFacade.authenticate(token);
          ActorContextHolder.set(new ActorContext(admin.adminId(), ActorContext.ActorType.ADMIN,
              "admin-web", 0, admin.recentAuthentication(), requestId));
        } else {
          AuthenticatedSession session = authenticationFacade.authenticate(token);
          ActorContextHolder.set(new ActorContext(session.userId(), ActorContext.ActorType.USER,
              session.deviceId(), session.authorizationVersion(),
              session.recentAuthentication(), requestId));
        }
      }
      chain.doFilter(request, response);
    } catch (BusinessException exception) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      objectMapper.writeValue(
          response.getWriter(),
          ApiResponse.failure(
              exception.getCode(),
              exception.getMessage(),
              String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID))));
    } finally {
      ActorContextHolder.clear();
    }
  }
}
