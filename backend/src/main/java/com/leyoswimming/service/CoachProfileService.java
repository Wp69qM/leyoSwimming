package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UpdateCoachProfileRequest;
import com.leyoswimming.dto.response.ConsentStatusResponse;
import com.leyoswimming.dto.response.CoachProfileResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.enums.ActorType;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import com.leyoswimming.util.PhoneEncryptor;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachProfileService {

  private static final int MIN_AGE = 3;
  private static final int MAX_AGE = 99;
  private static final Duration PHONE_CHANGE_LOCK_TTL = Duration.ofSeconds(60);

  private final CoachMapper coachMapper;
  private final PhoneEncryptor phoneEncryptor;
  private final SmsCodeService smsCodeService;
  private final SensitiveWordFilter sensitiveWordFilter;
  private final PolicyService policyService;
  private final IdempotencyHelper idempotencyHelper;
  private final DistributedLockHelper lockHelper;

  @Transactional(readOnly = true)
  public CoachProfileResponse getProfile(Long coachId) {
    Coach coach = findActiveCoach(coachId);
    ConsentStatusResponse consent = policyService.getConsentStatus(ActorType.coach, coachId);
    return toResponse(coach, consent);
  }

  @Transactional
  public CoachProfileResponse updateProfile(Long coachId, UpdateCoachProfileRequest request) {
    idempotencyHelper.checkAndLock(ActorType.coach.name(), coachId, request.idempotencyKey());
    try {
      Coach coach = findActiveCoach(coachId);
      validateConsent(coachId);
      validateAge(request.age());
      validateSensitiveWords(request);
      applyName(coach, request.name());
      applyAvatar(coach, request.avatarUrl());
      applyBasicInfo(coach, request);
      applyPhoneChange(coach, request);
      coach.setProfileCompleted(true);
      coachMapper.updateById(coach);
      ConsentStatusResponse consent = policyService.getConsentStatus(ActorType.coach, coachId);
      return toResponse(coach, consent);
    } catch (Exception e) {
      idempotencyHelper.unlock(ActorType.coach.name(), coachId, request.idempotencyKey());
      throw e;
    }
  }

  private Coach findActiveCoach(Long coachId) {
    Coach coach = coachMapper.selectById(coachId);
    if (coach == null || coach.getStatus() == CoachStatus.RESIGNED.getValue()) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }
    return coach;
  }

  private void validateConsent(Long coachId) {
    if (!policyService.hasAgreedCurrentPolicy(ActorType.coach, coachId)) {
      throw new BusinessException(ErrorCode.TERMS_NOT_ACCEPTED);
    }
  }

  private void validateAge(Integer age) {
    if (age == null || age < MIN_AGE || age > MAX_AGE) {
      throw new BusinessException(ErrorCode.INVALID_AGE);
    }
  }

  private void validateSensitiveWords(UpdateCoachProfileRequest request) {
    if (sensitiveWordFilter.containsSensitive(request.name())) {
      throw new BusinessException(ErrorCode.NICKNAME_SENSITIVE);
    }
    if (isNotBlank(request.personalDesc())
        && sensitiveWordFilter.containsSensitive(request.personalDesc())) {
      throw new BusinessException(ErrorCode.NICKNAME_SENSITIVE);
    }
  }

  private void applyName(Coach coach, String name) {
    coach.setName(name.trim());
  }

  private void applyAvatar(Coach coach, String avatarUrl) {
    if (isNotBlank(avatarUrl)) {
      coach.setAvatarUrl(avatarUrl.trim());
    }
  }

  private void applyBasicInfo(Coach coach, UpdateCoachProfileRequest request) {
    coach.setAge(request.age());
    coach.setGender(request.gender());
    coach.setPersonalDesc(isBlank(request.personalDesc()) ? null : request.personalDesc().trim());
    coach.setTeachingYears(request.teachingYears());
    if (request.certificates() == null || request.certificates().isEmpty()) {
      coach.setCertificates(null);
    } else {
      List<Coach.CoachCertificate> certificates =
          request.certificates().stream()
              .map(
                  url -> {
                    Coach.CoachCertificate certificate = new Coach.CoachCertificate();
                    certificate.setName("资质证书");
                    certificate.setUrl(url);
                    return certificate;
                  })
              .toList();
      coach.setCertificates(certificates);
    }
  }

  private void applyPhoneChange(Coach coach, UpdateCoachProfileRequest request) {
    if (isBlank(request.newPhone())) {
      return;
    }
    if (isBlank(request.oldPhoneVerifyCode()) || isBlank(request.newPhoneVerifyCode())) {
      throw new BusinessException(ErrorCode.INVALID_SMS_CODE);
    }
    String newPhoneHash = hashPhone(request.newPhone());
    LockToken lock = lockHelper.lock("phone_change", newPhoneHash, PHONE_CHANGE_LOCK_TTL);
    try {
      String currentPlainPhone = decryptPhone(coach.getPhone());
      smsCodeService.verify(
          currentPlainPhone, request.oldPhoneVerifyCode(), "change_phone_old", AppType.coach);
      smsCodeService.verify(
          request.newPhone(), request.newPhoneVerifyCode(), "change_phone_new", AppType.coach);
      if (phoneAlreadyUsedByOther(coach.getId(), newPhoneHash)) {
        throw new BusinessException(ErrorCode.PHONE_ALREADY_BOUND);
      }
      coach.setPhone(encryptPhone(request.newPhone()));
      coach.setPhoneHash(newPhoneHash);
    } finally {
      lockHelper.unlockAfterTransaction(lock);
    }
  }

  private boolean phoneAlreadyUsedByOther(Long currentCoachId, String phoneHash) {
    LambdaQueryWrapper<Coach> wrapper =
        new LambdaQueryWrapper<Coach>()
            .eq(Coach::getPhoneHash, phoneHash)
            .ne(Coach::getId, currentCoachId)
            .ne(Coach::getStatus, CoachStatus.RESIGNED.getValue());
    return coachMapper.selectCount(wrapper) > 0;
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

  private CoachProfileResponse toResponse(Coach coach, ConsentStatusResponse consent) {
    String phone = coach.getPhone() == null ? null : decryptPhone(coach.getPhone());
    List<String> certificateUrls =
        coach.getCertificates() == null
            ? null
            : coach.getCertificates().stream()
                .map(Coach.CoachCertificate::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
    return new CoachProfileResponse(
        coach.getId(),
        coach.getName(),
        coach.getAvatarUrl(),
        phone,
        coach.getAge(),
        coach.getGender(),
        coach.getPersonalDesc(),
        coach.getTeachingYears(),
        certificateUrls,
        Boolean.TRUE.equals(coach.getProfileCompleted()),
        consent);
  }
}
