package com.leyoswimming.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.service.AdminAuthService;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationFilterTest {

  private JwtTokenProvider jwtTokenProvider;
  private AdminAuthenticationFilter filter;

  @Mock private AdminAuthService adminAuthService;
  @Mock private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    jwtTokenProvider =
        new JwtTokenProvider(
            "bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==",
            86400000L);
    filter = new AdminAuthenticationFilter(adminAuthService, jwtTokenProvider, new ObjectMapper());
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("应跳过非 /api/admin/ 路径")
  void shouldNotFilter_nonAdminPath_returnsTrue() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
    assertThat(filter.shouldNotFilter(request)).isTrue();
  }

  @Test
  @DisplayName("应跳过 /api/admin/auth/ 路径")
  void shouldNotFilter_adminAuthPath_returnsTrue() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/auth/login");
    assertThat(filter.shouldNotFilter(request)).isTrue();
  }

  @Test
  @DisplayName("应处理 /api/admin/ 下非 auth 路径")
  void shouldNotFilter_adminManagementPath_returnsFalse() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/account/list");
    assertThat(filter.shouldNotFilter(request)).isFalse();
  }

  @Test
  @DisplayName("有效且启用管理员 token 设置认证并放行")
  void doFilterInternal_validEnabledAdminToken_setsAuthentication() throws Exception {
    String token = jwtTokenProvider.generateAdminToken(1L, "admin");
    MockHttpServletRequest request = adminRequest(token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(adminAuthService.isTokenActive(token)).thenReturn(true);
    when(adminAuthService.isAdminActive(1L)).thenReturn(true);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
  }

  @Test
  @DisplayName("被禁用管理员 token 返回 401")
  void doFilterInternal_disabledAdminToken_returnsUnauthorized() throws Exception {
    String token = jwtTokenProvider.generateAdminToken(1L, "admin");
    MockHttpServletRequest request = adminRequest(token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(adminAuthService.isTokenActive(token)).thenReturn(true);
    when(adminAuthService.isAdminActive(1L)).thenReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    assertThat(response.getStatus()).isEqualTo(401);
    ApiResponse<?> body =
        new ObjectMapper()
            .readValue(response.getContentAsString(StandardCharsets.UTF_8), ApiResponse.class);
    assertThat(body.code()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
  }

  @Test
  @DisplayName("无 token 请求返回 401")
  void doFilterInternal_noToken_returnsUnauthorized() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/account/list");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  @DisplayName("非 admin 类型 token 返回 401")
  void doFilterInternal_nonAdminToken_returnsUnauthorized() throws Exception {
    String token = jwtTokenProvider.generateUserAccessToken(1L, true);
    MockHttpServletRequest request = adminRequest(token);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    assertThat(response.getStatus()).isEqualTo(401);
  }

  private MockHttpServletRequest adminRequest(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/account/list");
    request.addHeader("Authorization", "Bearer " + token);
    return request;
  }
}
