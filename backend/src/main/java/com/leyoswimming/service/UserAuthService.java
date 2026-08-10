package com.leyoswimming.service;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UserPhoneLoginRequest;
import com.leyoswimming.dto.request.UserWechatLoginRequest;
import com.leyoswimming.dto.response.UserLoginResponse;
import com.leyoswimming.entity.User;
import com.leyoswimming.entity.UserLoginLog;
import com.leyoswimming.entity.UserSession;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.enums.UserStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.UserLoginLogMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.repository.UserSessionMapper;
import com.leyoswimming.security.JwtTokenProvider;
import com.leyoswimming.service.wechat.WechatClient;
import com.leyoswimming.service.wechat.WechatSession;
import com.leyoswimming.util.PhoneEncryptor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthService {

  private static final int LOGIN_LOG_SUCCESS = 0;
  private static final int LOGIN_LOG_FAIL = 1;

  private final UserMapper userMapper;
  private final UserSessionMapper userSessionMapper;
  private final UserLoginLogMapper userLoginLogMapper;
  private final WechatClient wechatClient;
  private final SmsCodeService smsCodeService;
  private final JwtTokenProvider jwtTokenProvider;
  private final PhoneEncryptor phoneEncryptor;

  @Transactional
  public UserLoginResponse wechatLogin(
      UserWechatLoginRequest request, String ip, String userAgent) {
    validateTerms(request.termsAccepted(), request.privacyAccepted());
    WechatSession session = wechatClient.code2session(request.code());
    String phone =
        wechatClient.decryptPhone(
            session.sessionKey(), request.phoneEncryptedData(), request.phoneIv());
    String encryptedPhone = encryptPhone(phone);
    User user = userMapper.findActiveByUnionId(session.unionId());
    boolean isNewUser = false;
    if (user == null) {
      user =
          createUser(
              session.openid(),
              session.unionId(),
              encryptedPhone,
              request.avatarUrl(),
              request.nickName());
      isNewUser = true;
    }
    return buildLoginResponse(user, isNewUser, session.sessionKey(), ip, userAgent);
  }

  @Transactional
  public UserLoginResponse phoneLogin(
      UserPhoneLoginRequest request, String ip, String userAgent) {
    validateTerms(request.termsAccepted(), request.privacyAccepted());
    smsCodeService.verify(request.phone(), request.code(), "login", AppType.user);
    String encryptedPhone = encryptPhone(request.phone());
    User user = userMapper.findActiveByPhone(encryptedPhone);
    boolean isNewUser = false;
    if (user == null) {
      user = createUserFromPhone(request.phone(), encryptedPhone);
      isNewUser = true;
    }
    return buildLoginResponse(user, isNewUser, "phone_session_" + request.phone(), ip, userAgent);
  }

  private User createUser(
      String openid, String unionId, String encryptedPhone, String avatarUrl, String nickName) {
    User user = new User();
    user.setOpenid(openid);
    user.setUnionId(unionId);
    user.setPhone(encryptedPhone);
    user.setAvatarUrl(avatarUrl);
    user.setName(nickName);
    user.setIdentityStatus("注册用户");
    user.setProfileCompleted(false);
    user.setStatus(UserStatus.ACTIVE.getValue());
    userMapper.insert(user);
    return user;
  }

  private User createUserFromPhone(String phone, String encryptedPhone) {
    User user = new User();
    user.setOpenid("phone_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    user.setPhone(encryptedPhone);
    user.setIdentityStatus("注册用户");
    user.setProfileCompleted(false);
    user.setStatus(UserStatus.ACTIVE.getValue());
    userMapper.insert(user);
    return user;
  }

  private UserLoginResponse buildLoginResponse(
      User user, boolean isNewUser, String sessionKey, String ip, String userAgent) {
    if (user.getStatus() != UserStatus.ACTIVE.getValue()) {
      throw new BusinessException(ErrorCode.USER_DISABLED);
    }
    String accessToken =
        jwtTokenProvider.generateUserAccessToken(
            user.getId(), Boolean.TRUE.equals(user.getProfileCompleted()));
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String refreshTokenHash = jwtTokenProvider.hashRefreshToken(refreshToken);
    UserSession session = new UserSession();
    session.setUserId(user.getId());
    session.setSessionKeyEncrypted(encryptPhone(sessionKey));
    session.setRefreshTokenHash(refreshTokenHash);
    session.setExpiresAt(
        LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationSeconds()));
    session.setLastActiveAt(LocalDateTime.now());
    userSessionMapper.insert(session);
    user.setLastLoginAt(LocalDateTime.now());
    user.setLoginIp(ip);
    userMapper.updateById(user);
    userLoginLogMapper.insert(buildLog(user.getId(), ip, userAgent, LOGIN_LOG_SUCCESS, null));
    return new UserLoginResponse(
        accessToken,
        refreshToken,
        jwtTokenProvider.getExpirationSeconds(),
        isNewUser,
        Boolean.TRUE.equals(user.getProfileCompleted()),
        user.getId());
  }

  private UserLoginLog buildLog(
      Long userId, String ip, String userAgent, int status, String reason) {
    UserLoginLog logRecord = new UserLoginLog();
    logRecord.setUserId(userId);
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
      return Base64.getEncoder()
          .encodeToString(
              MessageDigest.getInstance("SHA-256").digest(phone.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private void validateTerms(boolean termsAccepted, boolean privacyAccepted) {
    if (!termsAccepted || !privacyAccepted) {
      throw new BusinessException(ErrorCode.TERMS_NOT_ACCEPTED);
    }
  }
}
