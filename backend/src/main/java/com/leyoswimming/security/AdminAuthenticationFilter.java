package com.leyoswimming.security;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.service.AdminAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class AdminAuthenticationFilter extends OncePerRequestFilter {

  private final AdminAuthService adminAuthService;
  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    if (path.startsWith("/api/admin/auth/login")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
    if (token != null
        && jwtTokenProvider.isTokenValid(token)
        && "admin".equals(jwtTokenProvider.getTokenType(token))
        && adminAuthService.isTokenActive(token)) {
      Long adminId = jwtTokenProvider.getAdminUserId(token);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              adminId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private String extractBearerToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    return authorization.substring(7);
  }

  private void writeUnauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        ApiResponse.error(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.TOKEN_EXPIRED.getMessage()));
  }
}
