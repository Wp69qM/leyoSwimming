package com.leyoswimming.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSessionService {

  private final UserSessionMapper userSessionMapper;
  private final UserMapper userMapper;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public RefreshTokenResponse refreshAccessToken(String refreshToken) {
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    UserSession session =
        userSessionMapper.selectOne(
            new LambdaQueryWrapper<UserSession>().eq(UserSession::getRefreshTokenHash, hash));
    if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    }
    User user = userMapper.selectById(session.getUserId());
    if (user == null || user.getStatus() != UserStatus.ACTIVE.getValue()) {
      throw new BusinessException(ErrorCode.USER_DISABLED);
    }
    String newAccessToken =
        jwtTokenProvider.generateUserAccessToken(
            user.getId(), Boolean.TRUE.equals(user.getProfileCompleted()));
    String newRefreshToken = jwtTokenProvider.generateRefreshToken();
    session.setRefreshTokenHash(jwtTokenProvider.hashRefreshToken(newRefreshToken));
    session.setExpiresAt(
        LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationSeconds()));
    session.setLastActiveAt(LocalDateTime.now());
    userSessionMapper.updateById(session);
    return new RefreshTokenResponse(
        newAccessToken, newRefreshToken, jwtTokenProvider.getExpirationSeconds());
  }

  @Transactional
  public void logout(Long userId, String refreshToken) {
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    userSessionMapper.delete(
        new LambdaQueryWrapper<UserSession>()
            .eq(UserSession::getUserId, userId)
            .eq(UserSession::getRefreshTokenHash, hash));
  }
}
