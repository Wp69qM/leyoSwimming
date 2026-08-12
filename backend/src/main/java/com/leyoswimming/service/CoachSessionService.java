package com.leyoswimming.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoachSessionService {

  private final CoachSessionMapper coachSessionMapper;
  private final CoachMapper coachMapper;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public RefreshTokenResponse refreshAccessToken(String refreshToken) {
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    CoachSession session =
        coachSessionMapper.selectOne(
            new LambdaQueryWrapper<CoachSession>().eq(CoachSession::getRefreshTokenHash, hash));
    if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    }
    Coach coach = coachMapper.selectById(session.getCoachId());
    if (coach == null || coach.getStatus() == CoachStatus.RESIGNED.getValue()) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }
    String newAccessToken =
        jwtTokenProvider.generateCoachAccessToken(coach.getId(), coach.getStatus());
    String newRefreshToken = jwtTokenProvider.generateRefreshToken();
    session.setRefreshTokenHash(jwtTokenProvider.hashRefreshToken(newRefreshToken));
    session.setExpiresAt(
        LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationSeconds()));
    session.setLastActiveAt(LocalDateTime.now());
    coachSessionMapper.updateById(session);
    return new RefreshTokenResponse(
        newAccessToken, newRefreshToken, jwtTokenProvider.getExpirationSeconds());
  }

  @Transactional
  public void logout(Long coachId, String refreshToken) {
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    coachSessionMapper.delete(
        new LambdaQueryWrapper<CoachSession>()
            .eq(CoachSession::getCoachId, coachId)
            .eq(CoachSession::getRefreshTokenHash, hash));
  }
}
