package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.RefreshTokenResponse;
import com.leyoswimming.entity.User;
import com.leyoswimming.entity.UserSession;
import com.leyoswimming.enums.UserStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.repository.UserSessionMapper;
import com.leyoswimming.security.JwtTokenProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

  @Mock private UserSessionMapper userSessionMapper;
  @Mock private UserMapper userMapper;
  private JwtTokenProvider jwtTokenProvider;
  private UserSessionService userSessionService;

  @BeforeEach
  void setUp() {
    jwtTokenProvider =
        new JwtTokenProvider(
            "bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==",
            86400000L);
    userSessionService = new UserSessionService(userSessionMapper, userMapper, jwtTokenProvider);
  }

  @Test
  @DisplayName("刷新 token：有效 refresh token 返回新 access token")
  void refreshAccessToken_validToken_returnsNewTokens() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    UserSession session = activeSession(hash, 1L);
    when(userSessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);
    User user = activeUser(1L);
    when(userMapper.selectById(1L)).thenReturn(user);

    RefreshTokenResponse response = userSessionService.refreshAccessToken(refreshToken);

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.expiresInSeconds()).isEqualTo(86400L);
    ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
    verify(userSessionMapper).updateById(captor.capture());
    assertThat(captor.getValue().getRefreshTokenHash())
        .isEqualTo(jwtTokenProvider.hashRefreshToken(response.refreshToken()));
  }

  @Test
  @DisplayName("刷新 token：过期的 session 抛出 TOKEN_EXPIRED")
  void refreshAccessToken_expiredSession_throwsTokenExpired() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    UserSession session = activeSession(hash, 1L);
    session.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    when(userSessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);

    assertThatThrownBy(() -> userSessionService.refreshAccessToken(refreshToken))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TOKEN_EXPIRED));
  }

  @Test
  @DisplayName("刷新 token：用户不存在抛出 USER_DISABLED")
  void refreshAccessToken_userNotFound_throwsUserDisabled() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    UserSession session = activeSession(hash, 1L);
    when(userSessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);
    when(userMapper.selectById(1L)).thenReturn(null);

    assertThatThrownBy(() -> userSessionService.refreshAccessToken(refreshToken))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_DISABLED));
  }

  @Test
  @DisplayName("刷新 token：用户被封禁抛出 USER_DISABLED")
  void refreshAccessToken_bannedUser_throwsUserDisabled() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    UserSession session = activeSession(hash, 1L);
    User user = activeUser(1L);
    user.setStatus(UserStatus.BANNED.getValue());
    when(userSessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);
    when(userMapper.selectById(1L)).thenReturn(user);

    assertThatThrownBy(() -> userSessionService.refreshAccessToken(refreshToken))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_DISABLED));
  }

  @Test
  @DisplayName("登出：删除对应 session")
  void logout_validToken_deletesSession() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);

    userSessionService.logout(1L, refreshToken);

    verify(userSessionMapper).delete(any(LambdaQueryWrapper.class));
  }

  private UserSession activeSession(String hash, Long userId) {
    UserSession session = new UserSession();
    session.setId(1L);
    session.setUserId(userId);
    session.setRefreshTokenHash(hash);
    session.setExpiresAt(LocalDateTime.now().plusDays(7));
    session.setSessionKeyEncrypted("encrypted_session_key");
    return session;
  }

  private User activeUser(Long id) {
    User user = new User();
    user.setId(id);
    user.setStatus(UserStatus.ACTIVE.getValue());
    user.setProfileCompleted(false);
    return user;
  }
}
