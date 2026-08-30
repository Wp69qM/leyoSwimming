package com.leyoswimming.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

  @Value("${leyo.ai-service.internal-api-token:}")
  private String internalApiToken;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (!path.startsWith("/api/internal/ai/")) {
      chain.doFilter(request, response);
      return;
    }

    String token = request.getHeader(INTERNAL_TOKEN_HEADER);
    if (token == null || !token.equals(internalApiToken) || internalApiToken.isBlank()) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\":401,\"message\":\"未授权的内部访问\",\"data\":null}");
      return;
    }

    chain.doFilter(request, response);
  }
}
