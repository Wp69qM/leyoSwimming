package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminLoginRequest;
import com.leyoswimming.dto.response.AdminLoginResponse;
import com.leyoswimming.entity.AdminUser;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AdminLoginLogMapper;
import com.leyoswimming.repository.AdminSessionMapper;
import com.leyoswimming.repository.AdminUserMapper;
import com.leyoswimming.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

  @Mock private AdminUserMapper adminUserMapper;
  @Mock private AdminSessionMapper adminSessionMapper;
  @Mock private AdminLoginLogMapper adminLoginLogMapper;
  @Mock private PasswordEncoder passwordEncoder;
  private JwtTokenProvider jwtTokenProvider;
  private AdminAuthService adminAuthService;

  @BeforeEach
  void setUp() {
    jwtTokenProvider = new JwtTokenProvider("bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==", 86400000L);
    adminAuthService =
        new AdminAuthService(
            adminUserMapper, adminSessionMapper, adminLoginLogMapper, passwordEncoder, jwtTokenProvider);
  }

  @Test
  @DisplayName("正确的用户名密码登录成功")
  void login_validCredentials_returnsTokenAndAdminInfo() {
    AdminUser admin = enabledAdmin();
    when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);
    when(passwordEncoder.matches("correct_password", admin.getPasswordHash())).thenReturn(true);

    AdminLoginResponse response = adminAuthService.login(request("admin", "correct_password"), mockHttpRequest());

    assertThat(response.token()).isNotBlank();
    assertThat(response.expiresIn()).isEqualTo(86400L);
    assertThat(response.admin().username()).isEqualTo("admin");
    verify(adminSessionMapper).insert(any(com.leyoswimming.entity.AdminSession.class));
    verify(adminLoginLogMapper).insert(any(com.leyoswimming.entity.AdminLoginLog.class));
  }

  @Test
  @DisplayName("错误的用户名或密码抛出 UNAUTHORIZED")
  void login_invalidCredentials_throwsUnauthorized() {
    when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

    assertThatThrownBy(
            () -> adminAuthService.login(request("admin", "wrong_password"), mockHttpRequest()))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

    verify(adminLoginLogMapper).insert(any(com.leyoswimming.entity.AdminLoginLog.class));
    verify(adminSessionMapper, never()).insert(any(com.leyoswimming.entity.AdminSession.class));
  }

  @Test
  @DisplayName("被禁用的账号抛出 ADMIN_DISABLED")
  void login_disabledAdmin_throwsAdminDisabled() {
    AdminUser admin = enabledAdmin();
    admin.setStatus(1);
    when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);
    when(passwordEncoder.matches("correct_password", admin.getPasswordHash())).thenReturn(true);

    assertThatThrownBy(
            () -> adminAuthService.login(request("disabled_admin", "correct_password"), mockHttpRequest()))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ADMIN_DISABLED));
  }

  @Test
  @DisplayName("登出使 token 失效")
  void logout_validToken_revokesSession() {
    String token = jwtTokenProvider.generateAdminToken(1L, "admin");

    adminAuthService.logout(token);

    ArgumentCaptor<com.leyoswimming.entity.AdminSession> captor =
        ArgumentCaptor.forClass(com.leyoswimming.entity.AdminSession.class);
    verify(adminSessionMapper).selectOne(any(LambdaQueryWrapper.class));
  }

  private AdminUser enabledAdmin() {
    AdminUser admin = new AdminUser();
    admin.setId(1L);
    admin.setUsername("admin");
    admin.setPasswordHash("hashed_password");
    admin.setName("系统管理员");
    admin.setRole("super_admin");
    admin.setStatus(0);
    return admin;
  }

  private AdminLoginRequest request(String username, String password) {
    return new AdminLoginRequest(username, password);
  }

  private HttpServletRequest mockHttpRequest() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    lenient().when(request.getHeader("User-Agent")).thenReturn("JUnit");
    lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    return request;
  }
}
