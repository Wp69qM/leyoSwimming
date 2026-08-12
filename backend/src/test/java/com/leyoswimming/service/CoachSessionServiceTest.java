package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.RefreshTokenResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachSession;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachSessionMapper;
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
class CoachSessionServiceTest {

  @Mock private CoachSessionMapper coachSessionMapper;
  @Mock private CoachMapper coachMapper;
  private JwtTokenProvider jwtTokenProvider;
  private CoachSessionService coachSessionService;

  @BeforeEach
  void setUp() {
    jwtTokenProvider =
        new JwtTokenProvider(
            "bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==",
            86400000L);
    coachSessionService =
        new CoachSessionService(coachSessionMapper, coachMapper, jwtTokenProvider);
  }

  @Test
  @DisplayName("刷新 token：有效 refresh token 返回新 access token")
  void refreshAccessToken_validToken_returnsNewTokens() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    CoachSession session = activeSession(hash, 1L);
    when(coachSessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);
    Coach coach = activeCoach(1L, CoachStatus.APPROVED);
    when(coachMapper.selectById(1L)).thenReturn(coach);

    RefreshTokenResponse response = coachSessionService.refreshAccessToken(refreshToken);

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.expiresInSeconds()).isEqualTo(86400L);
    ArgumentCaptor<CoachSession> captor = ArgumentCaptor.forClass(CoachSession.class);
    verify(coachSessionMapper).updateById(captor.capture());
    assertThat(captor.getValue().getRefreshTokenHash())
        .isEqualTo(jwtTokenProvider.hashRefreshToken(response.refreshToken()));
  }

  @Test
  @DisplayName("刷新 token：过期的 session 抛出 TOKEN_EXPIRED")
  void refreshAccessToken_expiredSession_throwsTokenExpired() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    CoachSession session = activeSession(hash, 1L);
    session.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    when(coachSessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);

    assertThatThrownBy(() -> coachSessionService.refreshAccessToken(refreshToken))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TOKEN_EXPIRED));
  }

  @Test
  @DisplayName("刷新 token：教练不存在抛出 COACH_NOT_FOUND")
  void refreshAccessToken_coachNotFound_throwsCoachNotFound() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    CoachSession session = activeSession(hash, 1L);
    when(coachSessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);
    when(coachMapper.selectById(1L)).thenReturn(null);

    assertThatThrownBy(() -> coachSessionService.refreshAccessToken(refreshToken))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_NOT_FOUND));
  }

  @Test
  @DisplayName("刷新 token：已离职教练抛出 COACH_NOT_FOUND")
  void refreshAccessToken_resignedCoach_throwsCoachNotFound() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    CoachSession session = activeSession(hash, 1L);
    Coach coach = activeCoach(1L, CoachStatus.RESIGNED);
    when(coachSessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);
    when(coachMapper.selectById(1L)).thenReturn(coach);

    assertThatThrownBy(() -> coachSessionService.refreshAccessToken(refreshToken))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_NOT_FOUND));
  }

  @Test
  @DisplayName("登出：删除对应 coach_id + refresh_token_hash 的 session")
  void logout_validToken_deletesSession() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();

    coachSessionService.logout(1L, refreshToken);

    verify(coachSessionMapper).delete(any(LambdaQueryWrapper.class));
  }

  @Test
  @DisplayName("登出：重复调用保持幂等")
  void logout_calledTwice_deletesSessionTwice() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();

    coachSessionService.logout(1L, refreshToken);
    coachSessionService.logout(1L, refreshToken);

    verify(coachSessionMapper, times(2)).delete(any(LambdaQueryWrapper.class));
  }

  private CoachSession activeSession(String hash, Long coachId) {
    CoachSession session = new CoachSession();
    session.setId(1L);
    session.setCoachId(coachId);
    session.setRefreshTokenHash(hash);
    session.setExpiresAt(LocalDateTime.now().plusDays(7));
    session.setSessionKeyEncrypted("encrypted_session_key");
    return session;
  }

  private Coach activeCoach(Long id, CoachStatus status) {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setStatus(status.getValue());
    return coach;
  }
}
