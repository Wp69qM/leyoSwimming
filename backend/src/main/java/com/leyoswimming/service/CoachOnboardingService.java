package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachApplicationSaveDraftRequest;
import com.leyoswimming.dto.request.CoachApplicationSubmitRequest;
import com.leyoswimming.dto.request.CoachCertificateItem;
import com.leyoswimming.dto.response.CoachApplicationCertificateResponse;
import com.leyoswimming.dto.response.CoachApplicationResponse;
import com.leyoswimming.dto.response.CoachApplicationSubmitResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachApplication;
import com.leyoswimming.entity.CoachCertificateApplication;
import com.leyoswimming.enums.ActorType;
import com.leyoswimming.enums.CoachApplicationStatus;
import com.leyoswimming.enums.CoachAuditAction;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachApplicationMapper;
import com.leyoswimming.repository.CoachAuditLogMapper;
import com.leyoswimming.repository.CoachCertificateApplicationMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachOnboardingService {

  private static final int MIN_AGE = 18;
  private static final int MAX_AGE = 80;
  private static final int MIN_TEACHING_YEARS = 0;
  private static final int MAX_TEACHING_YEARS = 60;
  private static final int MIN_STUDENTS = 0;
  private static final int MAX_STUDENTS = 99999;
  private static final int MIN_HOURS = 0;
  private static final int MAX_HOURS = 99999;
  private static final BigDecimal MIN_PRICE = new BigDecimal("50.00");
  private static final BigDecimal MAX_PRICE = new BigDecimal("2000.00");
  private static final Set<String> VALID_SWIM_STROKES =
      Set.of("蛙泳", "自由泳", "仰泳", "蝶泳");
  private static final Set<String> REQUIRED_CERT_TYPES =
      Set.of("ID_CARD_FRONT", "ID_CARD_BACK", "COACH_CERT", "HEALTH_CERT", "PORTRAIT");
  private static final Duration ONBOARDING_LOCK_TTL = Duration.ofSeconds(30);

  private final CoachMapper coachMapper;
  private final CoachApplicationMapper applicationMapper;
  private final CoachCertificateApplicationMapper certificateApplicationMapper;
  private final CoachAuditLogMapper auditLogMapper;
  private final IdCardEncryptor idCardEncryptor;
  private final PhoneEncryptor phoneEncryptor;
  private final IdempotencyHelper idempotencyHelper;
  private final DistributedLockHelper distributedLockHelper;

  @Transactional(readOnly = true)
  public CoachApplicationResponse getDetail(Long coachId) {
    Coach coach = findCoach(coachId);
    CoachApplication application = applicationMapper.findLatestByCoachId(coachId);

    if (application == null) {
      return buildResponseFromCoach(coach, null, null);
    }

    List<CoachCertificateApplication> certs =
        certificateApplicationMapper.findByApplicationId(application.getId());
    return buildResponse(coach, application, certs);
  }

  @Transactional
  public CoachApplicationSubmitResponse saveDraft(
      Long coachId, CoachApplicationSaveDraftRequest request) {
    var lockToken = distributedLockHelper.lock("coach_onboarding", String.valueOf(coachId), ONBOARDING_LOCK_TTL);
    distributedLockHelper.unlockAfterTransaction(lockToken);
    idempotencyHelper.checkAndLock(
        ActorType.coach.name(), coachId, draftKey(request.idempotencyKey()));
    try {
      Coach coach = findCoach(coachId);
      validateCoachStatusForEdit(coach);

      CoachApplication application = applicationMapper.findLatestByCoachId(coachId);
      if (application != null
          && CoachApplicationStatus.PENDING.getValue().equals(application.getStatus())) {
        throw new BusinessException(ErrorCode.COACH_APPLICATION_PENDING);
      }

      boolean createNew = shouldCreateNewDraft(application, coach);
      if (createNew) {
        application = new CoachApplication();
        application.setCoachId(coachId);
        application.setStatus(CoachApplicationStatus.DRAFT.getValue());
      }
      fillApplicationFromRequest(application, request, coach);
      application.setSubmittedAt(null);

      if (createNew) {
        applicationMapper.insert(application);
      } else {
        applicationMapper.updateById(application);
      }

      saveCertificates(application.getId(), request.certificates(), true);
      writeAuditLog(coachId, application.getId(), CoachAuditAction.DRAFT_SAVE, coach.getStatus(), coach.getStatus(), null);

      return new CoachApplicationSubmitResponse(
          coachId, application.getId(), coach.getStatus(), null);
    } catch (Exception e) {
      idempotencyHelper.unlock(
          ActorType.coach.name(), coachId, draftKey(request.idempotencyKey()));
      throw e;
    }
  }

  @Transactional
  public CoachApplicationSubmitResponse submit(
      Long coachId, CoachApplicationSubmitRequest request) {
    var lockToken = distributedLockHelper.lock("coach_onboarding", String.valueOf(coachId), ONBOARDING_LOCK_TTL);
    distributedLockHelper.unlockAfterTransaction(lockToken);
    idempotencyHelper.checkAndLock(
        ActorType.coach.name(), coachId, submitKey(request.idempotencyKey()));
    try {
      Coach coach = findCoach(coachId);
      validateCoachStatusForEdit(coach);

      CoachApplication pending = applicationMapper.findPendingByCoachId(coachId);
      if (pending != null) {
        throw new BusinessException(ErrorCode.COACH_APPLICATION_PENDING);
      }

      validateSubmit(request);

      CoachApplication application = new CoachApplication();
      application.setCoachId(coachId);
      application.setStatus(CoachApplicationStatus.PENDING.getValue());
      application.setPreviousCoachStatus(coach.getStatus());
      fillApplicationFromRequest(application, request, coach);
      application.setSubmittedAt(LocalDateTime.now());
      applicationMapper.insert(application);

      saveCertificates(application.getId(), request.certificates(), true);

      Integer previousStatus = coach.getStatus();
      coach.setStatus(CoachStatus.PENDING.getValue());
      coach.setSubmittedAt(LocalDateTime.now());
      coachMapper.updateById(coach);

      writeAuditLog(
          coachId,
          application.getId(),
          CoachAuditAction.SUBMIT,
          previousStatus,
          CoachStatus.PENDING.getValue(),
          null);

      return new CoachApplicationSubmitResponse(
          coachId, application.getId(), CoachStatus.PENDING.getValue(), application.getSubmittedAt());
    } catch (Exception e) {
      idempotencyHelper.unlock(
          ActorType.coach.name(), coachId, submitKey(request.idempotencyKey()));
      throw e;
    }
  }

  private Coach findCoach(Long coachId) {
    Coach coach = coachMapper.selectById(coachId);
    if (coach == null) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }
    return coach;
  }

  private void validateCoachStatusForEdit(Coach coach) {
    int status = coach.getStatus();
    if (status != CoachStatus.NOT_SUBMITTED.getValue()
        && status != CoachStatus.REJECTED.getValue()
        && status != CoachStatus.RESIGNED.getValue()) {
      throw new BusinessException(ErrorCode.COACH_STATUS_NOT_ALLOWED);
    }
  }

  private boolean shouldCreateNewDraft(CoachApplication application, Coach coach) {
    if (application == null) {
      return true;
    }
    String status = application.getStatus();
    return CoachApplicationStatus.APPROVED.getValue().equals(status)
        || CoachApplicationStatus.PENDING.getValue().equals(status);
  }

  private void fillApplicationFromRequest(
      CoachApplication application, CoachApplicationSaveDraftRequest request, Coach coach) {
    application.setName(trim(request.name()));
    application.setGender(trim(request.gender()));
    application.setAge(request.age());
    application.setEmail(trim(request.email()));
    application.setWechatQrUrl(trim(request.wechatQrUrl()));
    application.setIdCardNo(encryptIdCard(trim(request.idCardNo())));
    application.setTeachingYears(request.teachingYears());
    application.setTotalStudents(request.totalStudents());
    application.setTotalHours(request.totalHours());
    application.setTeachingStrokes(joinStrokes(request.teachingStrokes()));
    application.setBio(trim(request.bio()));
    application.setReferencePrice(request.referencePrice());
    application.setPhoneHash(coach.getPhoneHash());
  }

  private void fillApplicationFromRequest(
      CoachApplication application,
      CoachApplicationSubmitRequest request,
      Coach coach) {
    application.setName(trim(request.name()));
    application.setPhone(coach.getPhone());
    application.setPhoneHash(coach.getPhoneHash());
    application.setGender(trim(request.gender()));
    application.setAge(request.age());
    application.setEmail(trim(request.email()));
    application.setWechatQrUrl(trim(request.wechatQrUrl()));
    application.setIdCardNo(encryptIdCard(trim(request.idCardNo())));
    application.setTeachingYears(request.teachingYears());
    application.setTotalStudents(request.totalStudents());
    application.setTotalHours(request.totalHours());
    application.setTeachingStrokes(joinStrokes(request.teachingStrokes()));
    application.setBio(trim(request.bio()));
    application.setReferencePrice(request.referencePrice());
  }

  private void validateSubmit(CoachApplicationSubmitRequest request) {
    validateAge(request.age());
    validateIdCard(request.idCardNo());
    validateReferencePrice(request.referencePrice());
    validateCounts(request.totalStudents(), request.totalHours());
    validateTeachingYears(request.teachingYears());
    validateCertificates(request.certificates());
  }

  private void validateAge(Integer age) {
    if (age == null || age < MIN_AGE || age > MAX_AGE) {
      throw new BusinessException(ErrorCode.INVALID_AGE);
    }
  }

  private void validateIdCard(String idCardNo) {
    if (idCardNo == null || !idCardNo.matches("\\d{17}[\\dXx]")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入 18 位有效身份证号");
    }
  }

  private void validateReferencePrice(BigDecimal price) {
    if (price == null || price.compareTo(MIN_PRICE) < 0 || price.compareTo(MAX_PRICE) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "参考单价需在 50-2000 元之间");
    }
  }

  private void validateCounts(Integer totalStudents, Integer totalHours) {
    if (totalStudents == null
        || totalStudents < MIN_STUDENTS
        || totalStudents > MAX_STUDENTS) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "总学员数需在 0-99999 之间");
    }
    if (totalHours == null || totalHours < MIN_HOURS || totalHours > MAX_HOURS) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "总课时数需在 0-99999 之间");
    }
  }

  private void validateTeachingYears(Integer teachingYears) {
    if (teachingYears == null
        || teachingYears < MIN_TEACHING_YEARS
        || teachingYears > MAX_TEACHING_YEARS) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任教年限需在 0-60 之间");
    }
  }

  private void validateCertificates(List<CoachCertificateItem> certificates) {
    if (certificates == null || certificates.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传身份证正反面、教练资格证、健康证和个人形象照");
    }
    for (CoachCertificateItem item : certificates) {
      try {
        com.leyoswimming.enums.CoachCertificateType.valueOf(item.certType());
      } catch (IllegalArgumentException | NullPointerException e) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "证书类型无效: " + item.certType());
      }
    }
    Set<String> presentTypes =
        certificates.stream()
            .map(CoachCertificateItem::certType)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    for (String required : REQUIRED_CERT_TYPES) {
      if (!presentTypes.contains(required)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传身份证正反面、教练资格证、健康证和个人形象照");
      }
    }
  }

  private void saveCertificates(
      Long applicationId, List<CoachCertificateItem> certificates, boolean replaceAll) {
    if (replaceAll) {
      LambdaQueryWrapper<CoachCertificateApplication> wrapper =
          new LambdaQueryWrapper<CoachCertificateApplication>()
              .eq(CoachCertificateApplication::getApplicationId, applicationId);
      certificateApplicationMapper.delete(wrapper);
    }
    if (certificates == null || certificates.isEmpty()) {
      return;
    }

    Map<String, Integer> typeCounter =
        Arrays.stream(com.leyoswimming.enums.CoachCertificateType.values())
            .collect(Collectors.toMap(com.leyoswimming.enums.CoachCertificateType::getValue, e -> 0));
    List<CoachCertificateApplication> entities = new ArrayList<>();
    for (CoachCertificateItem item : certificates) {
      if (item.certType() == null || item.imageUrl() == null) {
        continue;
      }
      CoachCertificateApplication entity = new CoachCertificateApplication();
      entity.setApplicationId(applicationId);
      entity.setCertType(item.certType());
      entity.setImageUrl(item.imageUrl().trim());
      int order = typeCounter.getOrDefault(item.certType(), 0);
      entity.setSortOrder(order);
      typeCounter.put(item.certType(), order + 1);
      entities.add(entity);
    }
    if (!entities.isEmpty()) {
      entities.sort(Comparator.comparing(CoachCertificateApplication::getCertType)
          .thenComparing(CoachCertificateApplication::getSortOrder));
      for (int i = 0; i < entities.size(); i++) {
        entities.get(i).setSortOrder(i);
      }
      for (CoachCertificateApplication entity : entities) {
        certificateApplicationMapper.insert(entity);
      }
    }
  }

  private void writeAuditLog(
      Long coachId,
      Long applicationId,
      CoachAuditAction action,
      Integer fromStatus,
      Integer toStatus,
      String reason) {
    com.leyoswimming.entity.CoachAuditLog log = new com.leyoswimming.entity.CoachAuditLog();
    log.setCoachId(coachId);
    log.setApplicationId(applicationId);
    log.setAction(action.getValue());
    log.setFromStatus(fromStatus);
    log.setToStatus(toStatus);
    log.setReason(reason);
    log.setCreatedAt(LocalDateTime.now());
    auditLogMapper.insert(log);
  }

  private String encryptIdCard(String idCardNo) {
    if (idCardNo == null) {
      return null;
    }
    try {
      return idCardEncryptor.encrypt(idCardNo);
    } catch (Exception e) {
      log.error("Failed to encrypt id card", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "身份证号加密失败");
    }
  }

  private String decryptIdCard(String encrypted) {
    if (encrypted == null) {
      return null;
    }
    try {
      return idCardEncryptor.decrypt(encrypted);
    } catch (Exception e) {
      log.error("Failed to decrypt id card", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "身份证号解密失败");
    }
  }

  private String decryptPhone(String encrypted) {
    if (encrypted == null) {
      return null;
    }
    try {
      return phoneEncryptor.decrypt(encrypted);
    } catch (Exception e) {
      log.error("Failed to decrypt phone", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号解密失败");
    }
  }

  private static String trim(String value) {
    return StringUtils.trimToNull(value);
  }

  private static String joinStrokes(List<String> strokes) {
    if (strokes == null || strokes.isEmpty()) {
      return null;
    }
    return strokes.stream()
        .filter(s -> s != null && !s.isBlank())
        .filter(VALID_SWIM_STROKES::contains)
        .distinct()
        .collect(Collectors.joining(","));
  }

  private static List<String> splitStrokes(String strokes) {
    if (strokes == null || strokes.isBlank()) {
      return List.of();
    }
    return Arrays.stream(strokes.split(","))
        .filter(s -> !s.isBlank())
        .filter(VALID_SWIM_STROKES::contains)
        .distinct()
        .toList();
  }

  private String draftKey(String key) {
    return "coach_draft:" + (StringUtils.isBlank(key) ? "default" : key);
  }

  private String submitKey(String key) {
    return "coach_submit:" + (StringUtils.isBlank(key) ? "default" : key);
  }

  private CoachApplicationResponse buildResponseFromCoach(Coach coach, CoachApplication application, List<CoachCertificateApplication> certs) {
    String idCardPlain = null;
    if (coach.getIdCardNo() != null) {
      idCardPlain = IdCardEncryptor.mask(decryptIdCard(coach.getIdCardNo()));
    }
    return new CoachApplicationResponse(
        coach.getId(),
        application == null ? null : application.getId(),
        decryptPhone(coach.getPhone()),
        coach.getName(),
        coach.getGender(),
        coach.getAge(),
        coach.getEmail(),
        coach.getWechatQrUrl(),
        idCardPlain,
        coach.getTeachingYears(),
        coach.getTotalStudents(),
        coach.getTotalHours(),
        splitStrokes(coach.getTeachingStrokes()),
        coach.getBio(),
        coach.getReferencePrice(),
        coach.getStatus(),
        application == null ? null : application.getStatus(),
        application == null ? null : application.getSubmittedAt(),
        resolveEntryType(coach, application),
        resolvePromptMessage(coach, application),
        certs == null ? List.of() : mapCertificates(certs));
  }

  private CoachApplicationResponse buildResponse(
      Coach coach, CoachApplication application, List<CoachCertificateApplication> certs) {
    String idCardPlain = null;
    if (application.getIdCardNo() != null) {
      idCardPlain = IdCardEncryptor.mask(decryptIdCard(application.getIdCardNo()));
    }
    return new CoachApplicationResponse(
        coach.getId(),
        application.getId(),
        decryptPhone(coach.getPhone()),
        application.getName() != null ? application.getName() : coach.getName(),
        application.getGender() != null ? application.getGender() : coach.getGender(),
        application.getAge() != null ? application.getAge() : coach.getAge(),
        application.getEmail() != null ? application.getEmail() : coach.getEmail(),
        application.getWechatQrUrl() != null ? application.getWechatQrUrl() : coach.getWechatQrUrl(),
        idCardPlain,
        application.getTeachingYears() != null ? application.getTeachingYears() : coach.getTeachingYears(),
        application.getTotalStudents() != null ? application.getTotalStudents() : coach.getTotalStudents(),
        application.getTotalHours() != null ? application.getTotalHours() : coach.getTotalHours(),
        splitStrokes(application.getTeachingStrokes() != null ? application.getTeachingStrokes() : coach.getTeachingStrokes()),
        application.getBio() != null ? application.getBio() : coach.getBio(),
        application.getReferencePrice() != null ? application.getReferencePrice() : coach.getReferencePrice(),
        coach.getStatus(),
        application.getStatus(),
        application.getSubmittedAt(),
        resolveEntryType(coach, application),
        resolvePromptMessage(coach, application),
        mapCertificates(certs));
  }

  private List<CoachApplicationCertificateResponse> mapCertificates(
      List<CoachCertificateApplication> certs) {
    if (certs == null) {
      return List.of();
    }
    return certs.stream()
        .map(
            c ->
                new CoachApplicationCertificateResponse(
                    c.getId(), c.getCertType(), c.getImageUrl(), c.getSortOrder()))
        .toList();
  }

  private String resolveEntryType(Coach coach, CoachApplication application) {
    if (application == null) {
      return "first";
    }
    String status = application.getStatus();
    if (CoachApplicationStatus.DRAFT.getValue().equals(status)) {
      return "draft";
    }
    if (CoachApplicationStatus.PENDING.getValue().equals(status)) {
      return Integer.valueOf(CoachStatus.NOT_SUBMITTED.getValue()).equals(application.getPreviousCoachStatus())
          ? "first"
          : "reapply";
    }
    if (CoachApplicationStatus.REJECTED.getValue().equals(status)) {
      return "rejected";
    }
    if (coach.getStatus() == CoachStatus.RESIGNED.getValue()) {
      return "reapply";
    }
    return "first";
  }

  private String resolvePromptMessage(Coach coach, CoachApplication application) {
    if (coach.getStatus() == CoachStatus.REJECTED.getValue()
        && application != null
        && CoachApplicationStatus.REJECTED.getValue().equals(application.getStatus())) {
      return application.getRejectionReason();
    }
    if (coach.getStatus() == CoachStatus.RESIGNED.getValue()) {
      return "你的账号已离职，请重新提交入驻资料，审核通过后即可恢复接单。";
    }
    return null;
  }
}
