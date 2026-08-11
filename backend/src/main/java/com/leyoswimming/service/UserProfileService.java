package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UpdateUserProfileRequest;
import com.leyoswimming.dto.response.ConsentStatusResponse;
import com.leyoswimming.dto.response.UserProfileResponse;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.ActorType;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.enums.UserStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

  private static final int MIN_AGE = 3;
  private static final int MAX_AGE = 99;

  private final UserMapper userMapper;
  private final PhoneEncryptor phoneEncryptor;
  private final SmsCodeService smsCodeService;
  private final SensitiveWordFilter sensitiveWordFilter;
  private final PolicyService policyService;
  private final IdempotencyHelper idempotencyHelper;

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(Long userId) {
    User user = findActiveUser(userId);
    ConsentStatusResponse consent = policyService.getConsentStatus(ActorType.user, userId);
    return toResponse(user, consent);
  }

  @Transactional
  public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
    idempotencyHelper.checkAndLock(ActorType.user.name(), userId, request.idempotencyKey());
    try {
      User user = findActiveUser(userId);
      validateConsent(userId);
      validateAge(request.age());
      validateSwimBasis(request);
      validateSensitiveWords(request);
      applyName(user, request.name());
      applyAvatar(user, request.avatarUrl());
      applyAgeAndGuardian(user, request);
      applySwimInfo(user, request);
      applyPersonalDesc(user, request.personalDesc());
      applyPhoneChange(user, request);
      user.setProfileCompleted(true);
      userMapper.updateById(user);
      ConsentStatusResponse consent = policyService.getConsentStatus(ActorType.user, userId);
      return toResponse(user, consent);
    } catch (Exception e) {
      idempotencyHelper.unlock(ActorType.user.name(), userId, request.idempotencyKey());
      throw e;
    }
  }

  private User findActiveUser(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null || user.getStatus() != UserStatus.ACTIVE.getValue()) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    return user;
  }

  private void validateConsent(Long userId) {
    if (!policyService.hasAgreedCurrentPolicy(ActorType.user, userId)) {
      throw new BusinessException(ErrorCode.TERMS_NOT_ACCEPTED);
    }
  }

  private void validateAge(Integer age) {
    if (age == null || age < MIN_AGE || age > MAX_AGE) {
      throw new BusinessException(ErrorCode.INVALID_AGE);
    }
  }

  private void validateSwimBasis(UpdateUserProfileRequest request) {
    if (Boolean.TRUE.equals(request.hasSwimBasis())) {
      if (request.swimStrokes() == null || request.swimStrokes().isEmpty()) {
        throw new BusinessException(ErrorCode.INVALID_SWIM_STROKE);
      }
    }
  }

  private void validateSensitiveWords(UpdateUserProfileRequest request) {
    if (sensitiveWordFilter.containsSensitive(request.name())) {
      throw new BusinessException(ErrorCode.NICKNAME_SENSITIVE);
    }
    if (isNotBlank(request.personalDesc())
        && sensitiveWordFilter.containsSensitive(request.personalDesc())) {
      throw new BusinessException(ErrorCode.NICKNAME_SENSITIVE);
    }
  }

  private void applyName(User user, String name) {
    user.setName(name.trim());
  }

  private void applyAvatar(User user, String avatarUrl) {
    if (isNotBlank(avatarUrl)) {
      user.setAvatarUrl(avatarUrl.trim());
    }
  }

  private void applyAgeAndGuardian(User user, UpdateUserProfileRequest request) {
    user.setAge(request.age());
    user.setGender(request.gender());
    if (request.age() < 18) {
      if (isBlank(request.guardianName()) || isBlank(request.guardianPhone())) {
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "未成年人需填写监护人信息");
      }
      user.setGuardianName(request.guardianName().trim());
      String guardianPhone = request.guardianPhone().trim();
      user.setGuardianPhone(encryptPhone(guardianPhone));
      user.setGuardianPhoneHash(hashPhone(guardianPhone));
    } else {
      user.setGuardianName(null);
      user.setGuardianPhone(null);
      user.setGuardianPhoneHash(null);
    }
  }

  private void applySwimInfo(User user, UpdateUserProfileRequest request) {
    user.setHasSwimBasis(request.hasSwimBasis());
    if (Boolean.TRUE.equals(request.hasSwimBasis())) {
      List<String> strokes = request.swimStrokes().stream().distinct().map(String::trim).toList();
      user.setSwimStrokes(strokes);
      user.setSwimYears(request.swimYears());
    } else {
      user.setSwimStrokes(null);
      user.setSwimYears(null);
    }
  }

  private void applyPersonalDesc(User user, String personalDesc) {
    user.setPersonalDesc(isBlank(personalDesc) ? null : personalDesc.trim());
  }

  private void applyPhoneChange(User user, UpdateUserProfileRequest request) {
    if (isBlank(request.newPhone())) {
      return;
    }
    if (isBlank(request.oldPhoneVerifyCode()) || isBlank(request.newPhoneVerifyCode())) {
      throw new BusinessException(ErrorCode.INVALID_SMS_CODE);
    }
    String currentPlainPhone = decryptPhone(user.getPhone());
    smsCodeService.verify(currentPlainPhone, request.oldPhoneVerifyCode(), "change_phone_old", AppType.user);
    smsCodeService.verify(request.newPhone(), request.newPhoneVerifyCode(), "change_phone_new", AppType.user);
    String encryptedNewPhone = encryptPhone(request.newPhone());
    String newPhoneHash = hashPhone(request.newPhone());
    if (phoneAlreadyUsedByOther(user.getId(), newPhoneHash)) {
      throw new BusinessException(ErrorCode.PHONE_ALREADY_BOUND);
    }
    user.setPhone(encryptedNewPhone);
    user.setPhoneHash(newPhoneHash);
  }

  private boolean phoneAlreadyUsedByOther(Long currentUserId, String phoneHash) {
    LambdaQueryWrapper<User> wrapper =
        new LambdaQueryWrapper<User>()
            .eq(User::getPhoneHash, phoneHash)
            .ne(User::getId, currentUserId)
            .eq(User::getStatus, UserStatus.ACTIVE.getValue());
    return userMapper.selectCount(wrapper) > 0;
  }

  private String hashPhone(String phone) {
    try {
      return phoneEncryptor.hash(phone);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号哈希失败", e);
    }
  }

  private String encryptPhone(String phone) {
    try {
      return phoneEncryptor.encrypt(phone);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号加密失败", e);
    }
  }

  private String decryptPhone(String encryptedPhone) {
    try {
      return phoneEncryptor.decrypt(encryptedPhone);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号解密失败", e);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static boolean isNotBlank(String value) {
    return !isBlank(value);
  }

  private UserProfileResponse toResponse(User user, ConsentStatusResponse consent) {
    String phone = user.getPhone() == null ? null : decryptPhone(user.getPhone());
    String guardianPhone =
        user.getGuardianPhone() == null ? null : decryptPhone(user.getGuardianPhone());
    return new UserProfileResponse(
        user.getId(),
        user.getName(),
        user.getAvatarUrl(),
        phone,
        user.getAge(),
        user.getGender(),
        user.getGuardianName(),
        guardianPhone,
        user.getHasSwimBasis(),
        user.getSwimStrokes(),
        user.getSwimYears(),
        user.getPersonalDesc(),
        Boolean.TRUE.equals(user.getProfileCompleted()),
        consent);
  }
}
