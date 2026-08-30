package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UpdateCoachProfileRequest;
import com.leyoswimming.dto.request.UpdateCoachReferencePriceRequest;
import com.leyoswimming.dto.response.CoachProfileResponse;
import com.leyoswimming.dto.response.CoachReferencePriceResponse;
import com.leyoswimming.dto.response.ConsentStatusResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachCertificate;
import com.leyoswimming.entity.CoachUpdateLog;
import com.leyoswimming.enums.ActorType;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.enums.CoachCertificateType;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachUpdateLogMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachProfileService {

  private static final int MIN_AGE = 18;
  private static final int MAX_AGE = 80;
  private static final int MIN_TEACHING_YEARS = 0;
  private static final int MAX_TEACHING_YEARS = 60;
  private static final int MIN_BIO_LENGTH = 10;
  private static final int MAX_BIO_LENGTH = 500;
  private static final BigDecimal MIN_REFERENCE_PRICE = new BigDecimal("50.00");
  private static final BigDecimal MAX_REFERENCE_PRICE = new BigDecimal("2000.00");
  private static final int MAX_PRICE_CHANGES_PER_DAY = 3;
  private static final Duration PHONE_CHANGE_LOCK_TTL = Duration.ofSeconds(60);

  private final CoachMapper coachMapper;
  private final CoachCertificateMapper coachCertificateMapper;
  private final CoachUpdateLogMapper coachUpdateLogMapper;
  private final PhoneEncryptor phoneEncryptor;
  private final IdCardEncryptor idCardEncryptor;
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
      validateStatusForUpdate(coach);
      validateConsent(coachId);
      validateProfileFields(request);

      logFieldChange(coachId, "name", coach.getName(), request.name());
      logFieldChange(coachId, "age", stringValue(coach.getAge()), stringValue(request.age()));
      logFieldChange(coachId, "gender", coach.getGender(), request.gender());
      logFieldChange(coachId, "email", coach.getEmail(), request.email());
      logFieldChange(coachId, "wechatQrUrl", coach.getWechatQrUrl(), request.wechatQrUrl());
      logFieldChange(
          coachId, "teachingYears", stringValue(coach.getTeachingYears()), stringValue(request.teachingYears()));
      logFieldChange(coachId, "teachingStrokes", coach.getTeachingStrokes(), formatStrokes(request.teachingStrokes()));
      logFieldChange(coachId, "bio", coach.getBio(), request.bio());

      applyName(coach, request.name());
      applyAvatar(coach, request.avatarUrl());
      applyBasicInfo(coach, request);
      applyPortrait(coachId, request.portraitUrl());
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

  @Transactional
  public CoachReferencePriceResponse updateReferencePrice(
      Long coachId, UpdateCoachReferencePriceRequest request) {
    idempotencyHelper.checkAndLock(ActorType.coach.name(), coachId, request.idempotencyKey());
    try {
      Coach coach = findActiveCoach(coachId);
      validateStatusForUpdate(coach);
      validateReferencePrice(request.referencePrice());

      LocalDate today = LocalDate.now();
      resetDailyCountIfNeeded(coach, today);
      if (coach.getPriceChangeCountToday() >= MAX_PRICE_CHANGES_PER_DAY) {
        throw new BusinessException(ErrorCode.PRICE_CHANGE_LIMIT_REACHED);
      }

      logFieldChange(
          coachId,
          "referencePrice",
          coach.getReferencePrice() == null ? null : coach.getReferencePrice().toPlainString(),
          request.referencePrice().toPlainString());

      coach.setReferencePrice(request.referencePrice());
      coach.setPriceChangedAt(LocalDateTime.now());
      coach.setPriceChangeCountToday(coach.getPriceChangeCountToday() + 1);
      coachMapper.updateById(coach);

      return new CoachReferencePriceResponse(
          coach.getReferencePrice(),
          coach.getPriceChangedAt(),
          MAX_PRICE_CHANGES_PER_DAY - coach.getPriceChangeCountToday());
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

  private void validateStatusForUpdate(Coach coach) {
    if (coach.getStatus() != CoachStatus.APPROVED.getValue()) {
      throw new BusinessException(ErrorCode.COACH_STATUS_NOT_APPROVED);
    }
  }

  private void validateConsent(Long coachId) {
    if (!policyService.hasAgreedCurrentPolicy(ActorType.coach, coachId)) {
      throw new BusinessException(ErrorCode.TERMS_NOT_ACCEPTED);
    }
  }

  private void validateProfileFields(UpdateCoachProfileRequest request) {
    if (request.age() == null || request.age() < MIN_AGE || request.age() > MAX_AGE) {
      throw new BusinessException(ErrorCode.INVALID_AGE);
    }
    if (request.teachingYears() != null
        && (request.teachingYears() < MIN_TEACHING_YEARS || request.teachingYears() > MAX_TEACHING_YEARS)) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "任教年限需在 0-60 年之间");
    }
    if (isNotBlank(request.bio())
        && (request.bio().length() < MIN_BIO_LENGTH || request.bio().length() > MAX_BIO_LENGTH)) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "个人简介需在 10-500 字符之间");
    }
    if (sensitiveWordFilter.containsSensitive(request.name())) {
      throw new BusinessException(ErrorCode.NICKNAME_SENSITIVE);
    }
    if (isNotBlank(request.bio()) && sensitiveWordFilter.containsSensitive(request.bio())) {
      throw new BusinessException(ErrorCode.NICKNAME_SENSITIVE);
    }
  }

  private void validateReferencePrice(BigDecimal price) {
    if (price == null
        || price.compareTo(MIN_REFERENCE_PRICE) < 0
        || price.compareTo(MAX_REFERENCE_PRICE) > 0) {
      throw new BusinessException(ErrorCode.INVALID_REFERENCE_PRICE);
    }
  }

  private void resetDailyCountIfNeeded(Coach coach, LocalDate today) {
    LocalDateTime lastChanged = coach.getPriceChangedAt();
    if (lastChanged == null || !today.equals(lastChanged.toLocalDate())) {
      coach.setPriceChangeCountToday(0);
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
    coach.setEmail(isBlank(request.email()) ? null : request.email().trim());
    coach.setWechatQrUrl(isBlank(request.wechatQrUrl()) ? null : request.wechatQrUrl().trim());
    coach.setPersonalDesc(isBlank(request.personalDesc()) ? null : request.personalDesc().trim());
    coach.setTeachingYears(request.teachingYears());
    coach.setTeachingStrokes(formatStrokes(request.teachingStrokes()));
    coach.setBio(isBlank(request.bio()) ? null : request.bio().trim());
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

  private void applyPortrait(Long coachId, String portraitUrl) {
    if (isBlank(portraitUrl)) {
      return;
    }
    LambdaQueryWrapper<CoachCertificate> wrapper =
        new LambdaQueryWrapper<CoachCertificate>()
            .eq(CoachCertificate::getCoachId, coachId)
            .eq(CoachCertificate::getCertType, CoachCertificateType.PORTRAIT.getValue());
    CoachCertificate existing = coachCertificateMapper.selectOne(wrapper);
    if (existing != null) {
      existing.setImageUrl(portraitUrl.trim());
      coachCertificateMapper.updateById(existing);
    } else {
      CoachCertificate portrait = new CoachCertificate();
      portrait.setCoachId(coachId);
      portrait.setCertType(CoachCertificateType.PORTRAIT.getValue());
      portrait.setImageUrl(portraitUrl.trim());
      portrait.setSortOrder(0);
      coachCertificateMapper.insert(portrait);
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

  private void logFieldChange(Long coachId, String fieldName, String oldValue, String newValue) {
    if (Objects.equals(oldValue, newValue)) {
      return;
    }
    CoachUpdateLog logEntry = new CoachUpdateLog();
    logEntry.setCoachId(coachId);
    logEntry.setFieldName(fieldName);
    logEntry.setOldValue(oldValue);
    logEntry.setNewValue(newValue);
    logEntry.setCreatedAt(LocalDateTime.now());
    coachUpdateLogMapper.insert(logEntry);
  }

  private String formatStrokes(List<String> strokes) {
    if (strokes == null || strokes.isEmpty()) {
      return null;
    }
    return strokes.stream().filter(Objects::nonNull).distinct().collect(Collectors.joining(","));
  }

  private String stringValue(Object value) {
    return value == null ? null : value.toString();
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
    if (isBlank(encryptedPhone)) {
      return null;
    }
    try {
      return phoneEncryptor.decrypt(encryptedPhone);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号解密失败", e);
    }
  }

  private String decryptIdCard(String encryptedIdCard) {
    if (isBlank(encryptedIdCard)) {
      return null;
    }
    try {
      return idCardEncryptor.decrypt(encryptedIdCard);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "身份证号解密失败", e);
    }
  }

  private String findPortraitUrl(Long coachId) {
    LambdaQueryWrapper<CoachCertificate> wrapper =
        new LambdaQueryWrapper<CoachCertificate>()
            .eq(CoachCertificate::getCoachId, coachId)
            .eq(CoachCertificate::getCertType, CoachCertificateType.PORTRAIT.getValue());
    CoachCertificate portrait = coachCertificateMapper.selectOne(wrapper);
    return portrait == null ? null : portrait.getImageUrl();
  }

  private List<Coach.CoachCertificate> embeddedCertificates(Coach coach) {
    return coach.getCertificates() == null ? List.of() : coach.getCertificates();
  }

  private Optional<String> findCertUrl(Coach coach, CoachCertificateType type) {
    return embeddedCertificates(coach).stream()
        .filter(cert -> type.getValue().equalsIgnoreCase(cert.getName()) && isNotBlank(cert.getUrl()))
        .map(Coach.CoachCertificate::getUrl)
        .findFirst();
  }

  private List<String> certUrlsByType(Coach coach, CoachCertificateType type) {
    return embeddedCertificates(coach).stream()
        .filter(cert -> type.getValue().equalsIgnoreCase(cert.getName()) && isNotBlank(cert.getUrl()))
        .map(Coach.CoachCertificate::getUrl)
        .toList();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static boolean isNotBlank(String value) {
    return !isBlank(value);
  }

  private CoachProfileResponse toResponse(Coach coach, ConsentStatusResponse consent) {
    String phone = decryptPhone(coach.getPhone());
    String idCardNo = decryptIdCard(coach.getIdCardNo());
    List<String> teachingStrokes =
        isBlank(coach.getTeachingStrokes())
            ? List.of()
            : Arrays.stream(coach.getTeachingStrokes().split(",")).filter(Objects::nonNull).toList();
    return new CoachProfileResponse(
        coach.getId(),
        coach.getName(),
        coach.getAvatarUrl(),
        findPortraitUrl(coach.getId()),
        phone,
        coach.getAge(),
        coach.getGender(),
        coach.getEmail(),
        coach.getWechatQrUrl(),
        IdCardEncryptor.mask(idCardNo),
        findCertUrl(coach, CoachCertificateType.ID_CARD_FRONT).orElse(null),
        findCertUrl(coach, CoachCertificateType.ID_CARD_BACK).orElse(null),
        certUrlsByType(coach, CoachCertificateType.COACH_CERT),
        findCertUrl(coach, CoachCertificateType.HEALTH_CERT).orElse(null),
        coach.getTotalStudents(),
        coach.getTotalHours(),
        coach.getPersonalDesc(),
        coach.getTeachingYears(),
        teachingStrokes,
        coach.getBio(),
        coach.getReferencePrice(),
        coach.getStatus(),
        embeddedCertificates(coach).stream()
            .map(Coach.CoachCertificate::getUrl)
            .filter(CoachProfileService::isNotBlank)
            .toList(),
        Boolean.TRUE.equals(coach.getProfileCompleted()),
        consent);
  }
}
