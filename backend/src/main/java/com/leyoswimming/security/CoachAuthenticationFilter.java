package com.leyoswimming.security;

import com.leyoswimming.service.AdminAuthService;
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
public class CoachAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final AdminAuthService adminAuthService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (isPublic(path)) {
      chain.doFilter(request, response);
      return;
    }
    if (path.startsWith("/api/coach/")) {
      authenticateCoach(request);
    } else if (path.equals("/api/common/file/upload")) {
      authenticateForCommonUpload(request);
    }
    chain.doFilter(request, response);
  }

  private void authenticateCoach(HttpServletRequest request) {
    String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
    if (token != null
        && jwtTokenProvider.isTokenValid(token)
        && "coach".equals(jwtTokenProvider.getTokenType(token))) {
      Long coachId = jwtTokenProvider.getCoachId(token);
      setAuthentication(coachId, "ROLE_COACH", request);
    }
  }

  private void authenticateForCommonUpload(HttpServletRequest request) {
    String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
    if (token == null || !jwtTokenProvider.isTokenValid(token)) {
      return;
    }
    String tokenType = jwtTokenProvider.getTokenType(token);
    if ("coach".equals(tokenType)) {
      Long coachId = jwtTokenProvider.getCoachId(token);
      setAuthentication(coachId, "ROLE_COACH", request);
    } else if ("admin".equals(tokenType)
        && adminAuthService.isTokenActive(token)
        && adminAuthService.isAdminActive(jwtTokenProvider.getAdminUserId(token))) {
      Long adminId = jwtTokenProvider.getAdminUserId(token);
      setAuthentication(adminId, "ROLE_ADMIN", request);
    }
  }

  private void setAuthentication(
      Long principal, String role, HttpServletRequest request) {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority(role)));
    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private boolean isPublic(String path) {
    return path.startsWith("/api/coach/auth/wechat-login")
        || path.startsWith("/api/coach/auth/phone-login")
        || path.startsWith("/api/coach/auth/refresh")
        || path.startsWith("/api/common/sms/");
  }

  private String extractBearerToken(String header) {
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }
}
