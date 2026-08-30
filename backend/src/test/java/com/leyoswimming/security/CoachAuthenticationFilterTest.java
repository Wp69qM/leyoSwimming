package com.leyoswimming.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leyoswimming.service.AdminAuthService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class CoachAuthenticationFilterTest {

  private JwtTokenProvider jwtTokenProvider;
  private AdminAuthService adminAuthService;
  private CoachAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    jwtTokenProvider =
        new JwtTokenProvider(
            "bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==",
            86400000L);
    adminAuthService = mock(AdminAuthService.class);
    filter = new CoachAuthenticationFilter(jwtTokenProvider, adminAuthService);
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("教练文件上传路径应解析 coach token 并设置认证")
  void doFilterInternal_commonFileUploadWithCoachToken_setsAuthentication() throws Exception {
    String token = jwtTokenProvider.generateCoachAccessToken(1L, -1);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/common/file/upload");
    request.addHeader("Authorization", "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
    assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
        .anyMatch(auth -> auth.getAuthority().equals("ROLE_COACH"));
  }

  @Test
  @DisplayName("教练文件上传路径无 token 不设置认证")
  void doFilterInternal_commonFileUploadWithoutToken_noAuthentication() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/common/file/upload");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("非教练相关路径不解析 coach token")
  void doFilterInternal_userPath_noAuthentication() throws Exception {
    String token = jwtTokenProvider.generateCoachAccessToken(1L, -1);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
    request.addHeader("Authorization", "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
