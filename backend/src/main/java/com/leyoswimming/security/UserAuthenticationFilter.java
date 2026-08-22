package com.leyoswimming.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class UserAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (isPublic(path)) {
      chain.doFilter(request, response);
      return;
    }
    if (path.startsWith("/api/user/")
        || path.startsWith("/api/order/")
        || path.startsWith("/api/payment/")
        || path.startsWith("/api/ai-assistant/")) {
      String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
      if (token != null
          && jwtTokenProvider.isTokenValid(token)
          && "user".equals(jwtTokenProvider.getTokenType(token))) {
        Long userId = jwtTokenProvider.getUserId(token);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    chain.doFilter(request, response);
  }

  private boolean isPublic(String path) {
    return path.startsWith("/api/user/auth/wechat-login")
        || path.startsWith("/api/user/auth/phone-login")
        || path.startsWith("/api/user/auth/refresh")
        || path.startsWith("/api/common/sms/");
  }

  private String extractBearerToken(String header) {
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }
}
