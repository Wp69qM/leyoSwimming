package com.leyoswimming.service;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachPhoneLoginRequest;
import com.leyoswimming.dto.request.CoachWechatLoginRequest;
import com.leyoswimming.dto.response.CoachLoginResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachLoginLog;
import com.leyoswimming.entity.CoachSession;
import com.leyoswimming.enums.ActorType;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachLoginLogMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachSessionMapper;
import com.leyoswimming.security.JwtTokenProvider;
import com.leyoswimming.service.wechat.WechatClient;
import com.leyoswimming.service.wechat.WechatSession;
import com.leyoswimming.util.PhoneEncryptor;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachAuthService {

  private static final int LOGIN_LOG_SUCCESS = 0;

  private final CoachMapper coachMapper;
  private final CoachSessionMapper coachSessionMapper;
  private final CoachLoginLogMapper coachLoginLogMapper;
  private final WechatClient wechatClient;
  private final SmsCodeService smsCodeService;
  private final JwtTokenProvider jwtTokenProvider;
  private final PhoneEncryptor phoneEncryptor;
  private final PolicyService policyService;

  @Transactional
  public CoachLoginResponse wechatLogin(
      CoachWechatLoginRequest request, String ip, String userAgent) {
    validateTerms(request.termsAccepted(), request.privacyAccepted());
    WechatSession session = wechatClient.code2session(request.code());
    String phone =
        wechatClient.decryptPhone(
            session.sessionKey(), request.phoneEncryptedData(), request.phoneIv());
    String encryptedPhone = encryptPhone(phone);
    String phoneHash = hashPhone(phone);
    Coach coach = coachMapper.findActiveByUnionId(session.unionId());
    boolean isNewCoach = false;
    if (coach == null) {
      coach =
          createCoach(
              session.openid(),
              session.unionId(),
              encryptedPhone,
              phoneHash,
              request.avatarUrl(),
              request.nickName());
      isNewCoach = true;
    }
    recordLoginConsent(coach.getId(), request.termsVersion(), request.privacyVersion());
    return buildLoginResponse(coach, isNewCoach, session.sessionKey(), ip, userAgent);
  }

  @Transactional
  public CoachLoginResponse phoneLogin(
      CoachPhoneLoginRequest request, String ip, String userAgent) {
    validateTerms(request.termsAccepted(), request.privacyAccepted());
    smsCodeService.verify(request.phone(), request.code(), "login", AppType.coach);
    String encryptedPhone = encryptPhone(request.phone());
    String phoneHash = hashPhone(request.phone());
    Coach coach = coachMapper.findActiveByPhone(phoneHash);
    boolean isNewCoach = false;
    if (coach == null) {
      coach = createCoachFromPhone(request.phone(), encryptedPhone, phoneHash);
      isNewCoach = true;
    }
    recordLoginConsent(coach.getId(), request.termsVersion(), request.privacyVersion());
    return buildLoginResponse(
        coach, isNewCoach, "phone_session_" + request.phone(), ip, userAgent);
  }

  private Coach createCoach(
      String openid, String unionId, String encryptedPhone, String phoneHash, String avatarUrl, String nickName) {
    Coach coach = new Coach();
    coach.setOpenid(openid);
    coach.setUnionId(unionId);
    coach.setPhone(encryptedPhone);
    coach.setPhoneHash(phoneHash);
    coach.setAvatarUrl(avatarUrl);
    coach.setName(nickName);
    coach.setStatus(CoachStatus.NOT_SUBMITTED.getValue());
    coachMapper.insert(coach);
    return coach;
  }

  private Coach createCoachFromPhone(String phone, String encryptedPhone, String phoneHash) {
    Coach coach = new Coach();
    coach.setOpenid("phone_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    coach.setPhone(encryptedPhone);
    coach.setPhoneHash(phoneHash);
    coach.setStatus(CoachStatus.NOT_SUBMITTED.getValue());
    coachMapper.insert(coach);
    return coach;
  }

  private CoachLoginResponse buildLoginResponse(
      Coach coach, boolean isNewCoach, String sessionKey, String ip, String userAgent) {
    if (coach.getStatus() == CoachStatus.RESIGNED.getValue()) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }
    String accessToken =
        jwtTokenProvider.generateCoachAccessToken(coach.getId(), coach.getStatus());
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String refreshTokenHash = jwtTokenProvider.hashRefreshToken(refreshToken);
    CoachSession session = new CoachSession();
    session.setCoachId(coach.getId());
    session.setSessionKeyEncrypted(encryptPhone(sessionKey));
    session.setRefreshTokenHash(refreshTokenHash);
    session.setExpiresAt(
        LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationSeconds()));
    session.setLastActiveAt(LocalDateTime.now());
    coachSessionMapper.insert(session);
    coach.setLastLoginAt(LocalDateTime.now());
    coach.setLoginIp(ip);
    coachMapper.updateById(coach);
    coachLoginLogMapper.insert(buildLog(coach.getId(), ip, userAgent, LOGIN_LOG_SUCCESS, null));
    return new CoachLoginResponse(
        accessToken,
        refreshToken,
        jwtTokenProvider.getExpirationSeconds(),
        isNewCoach,
        coach.getStatus(),
        coach.getId(),
        Boolean.TRUE.equals(coach.getProfileCompleted()));
  }

  private CoachLoginLog buildLog(
      Long coachId, String ip, String userAgent, int status, String reason) {
    CoachLoginLog logRecord = new CoachLoginLog();
    logRecord.setCoachId(coachId);
    logRecord.setIp(ip);
    logRecord.setUserAgent(userAgent);
    logRecord.setStatus(status);
    logRecord.setReason(reason);
    return logRecord;
  }

  private String encryptPhone(String phone) {
    try {
      return phoneEncryptor.encrypt(phone);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号加密失败");
    }
  }

  private String hashPhone(String phone) {
    try {
      return phoneEncryptor.hash(phone);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号哈希失败", e);
    }
  }

  private void recordLoginConsent(Long coachId, String termsVersion, String privacyVersion) {
    policyService.recordConsent(ActorType.coach, coachId, termsVersion, privacyVersion);
  }

  private void validateTerms(boolean termsAccepted, boolean privacyAccepted) {
    if (!termsAccepted || !privacyAccepted) {
      throw new BusinessException(ErrorCode.TERMS_NOT_ACCEPTED);
    }
  }
}
