package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminCoachAddRequest;
import com.leyoswimming.dto.request.AdminCoachCancelEntryRequest;
import com.leyoswimming.dto.request.AdminCoachListRequest;
import com.leyoswimming.dto.request.AdminCoachUpdateRequest;
import com.leyoswimming.dto.request.CoachCertificateItem;
import com.leyoswimming.dto.response.AdminCoachApplicationHistoryResponse;
import com.leyoswimming.dto.response.AdminCoachAuditLogResponse;
import com.leyoswimming.dto.response.AdminCoachCertificateResponse;
import com.leyoswimming.dto.response.AdminCoachDetailResponse;
import com.leyoswimming.dto.response.AdminCoachListItemResponse;
import com.leyoswimming.dto.response.AdminCoachListResponse;
import com.leyoswimming.entity.AuditLog;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachApplication;
import com.leyoswimming.entity.CoachAuditLog;
import com.leyoswimming.entity.CoachCertificate;
import com.leyoswimming.enums.CoachCertificateType;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AuditLogMapper;
import com.leyoswimming.repository.CoachApplicationMapper;
import com.leyoswimming.repository.CoachAuditLogMapper;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.util.PhoneEncryptor;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCoachManagementService {

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

  private static final String ACTION_CREATE = "ADMIN_CREATE_COACH";
  private static final String ACTION_UPDATE = "ADMIN_UPDATE_COACH_PROFILE";
  private static final String ACTION_CANCEL_ENTRY = "ADMIN_CANCEL_COACH_ENTRY";
  private static final String TARGET_TYPE = "coach";
  private static final String ACTOR_TYPE = "admin";

  private final CoachMapper coachMapper;
  private final CoachCertificateMapper certificateMapper;
  private final CoachApplicationMapper applicationMapper;
  private final CoachAuditLogMapper coachAuditLogMapper;
  private final AuditLogMapper auditLogMapper;
  private final PackageMapper packageMapper;
  private final PhoneEncryptor phoneEncryptor;
  private final IdCardEncryptor idCardEncryptor;
  private final AdminPermissionHelper permissionHelper;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public AdminCoachListResponse list(Long adminId, AdminCoachListRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_COACH_READ);

    LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(request.status())) {
      try {
        int status = Integer.parseInt(request.status().trim());
        wrapper.eq(Coach::getStatus, status);
      } catch (NumberFormatException e) {
        log.warn("Invalid status filter: {}", request.status());
      }
    }
    if (StringUtils.isNotBlank(request.keyword())) {
      String keyword = request.keyword().trim();
      if (keyword.matches("\\d{11}")) {
        try {
          wrapper.eq(Coach::getPhoneHash, phoneEncryptor.hash(keyword));
        } catch (Exception e) {
          log.error("Failed to hash phone keyword", e);
          wrapper.eq(Coach::getPhoneHash, "");
        }
      } else {
        wrapper.like(Coach::getName, keyword);
      }
    }
    wrapper.orderByDesc(Coach::getCreatedAt);

    Page<Coach> page = new Page<>(request.page(), request.pageSize());
    Page<Coach> result = coachMapper.selectPage(page, wrapper);

    List<AdminCoachListItemResponse> list =
        result.getRecords().stream().map(this::toListItem).toList();
    return new AdminCoachListResponse(
        list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
  }

  @Transactional(readOnly = true)
  public AdminCoachDetailResponse detail(Long adminId, Long coachId) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_COACH_READ);
    Coach coach = findCoach(coachId);
    return toDetailResponse(coach);
  }

  @Transactional
  public AdminCoachDetailResponse add(
      Long adminId, AdminCoachAddRequest request, HttpServletRequest httpRequest) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_COACH_WRITE);

    validateCoachFields(request.age(), request.idCardNo(), request.referencePrice(),
        request.totalStudents(), request.totalHours(), request.teachingYears(),
        request.certificates());

    String phone = request.phone().trim();
    String phoneHash = hashPhone(phone);
    if (phoneExists(phoneHash, null)) {
      throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
    }

    String idCardNo = request.idCardNo().trim();
    String idCardHash = hashIdCard(idCardNo);
    if (idCardExists(idCardHash, null)) {
      throw new BusinessException(ErrorCode.ID_CARD_ALREADY_EXISTS);
    }

    Coach coach = new Coach();
    coach.setOpenid(null);
    coach.setUnionId(null);
    coach.setPhone(encryptPhone(phone));
    coach.setPhoneHash(phoneHash);
    coach.setAvatarUrl(StringUtils.trimToNull(request.avatarUrl()));
    coach.setName(request.name().trim());
    coach.setGender(trim(request.gender()));
    coach.setAge(request.age());
    coach.setEmail(StringUtils.trimToNull(request.email()));
    coach.setWechatQrUrl(StringUtils.trimToNull(request.wechatQrUrl()));
    coach.setIdCardNo(encryptIdCard(idCardNo));
    coach.setIdCardHash(idCardHash);
    coach.setTeachingYears(request.teachingYears());
    coach.setTotalStudents(request.totalStudents());
    coach.setTotalHours(request.totalHours());
    coach.setTeachingStrokes(joinStrokes(request.teachingStrokes()));
    coach.setBio(StringUtils.trimToNull(request.bio()));
    coach.setReferencePrice(request.referencePrice());
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coach.setProfileCompleted(true);
    coach.setApprovedAt(LocalDateTime.now());

    coachMapper.insert(coach);

    saveCertificates(coach.getId(), request.certificates(), true);

    writeAuditLog(
        adminId,
        coach.getId(),
        ACTION_CREATE,
        null,
        toAuditSnapshot(coach),
        null,
        getClientIp(httpRequest));

    writeCoachAuditLog(
        adminId, coach.getId(), ACTION_CREATE, null, coach.getStatus(), null);

    return toDetailResponse(coach);
  }

  @Transactional
  public AdminCoachDetailResponse update(
      Long adminId, AdminCoachUpdateRequest request, HttpServletRequest httpRequest) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_COACH_WRITE);

    Coach coach = findCoach(request.coachId());
    if (!Objects.equals(coach.getVersion(), request.version())) {
      throw new BusinessException(ErrorCode.USER_CONCURRENTLY_UPDATED);
    }

    validateCoachFields(request.age(), request.idCardNo(), request.referencePrice(),
        request.totalStudents(), request.totalHours(), request.teachingYears(),
        request.certificates());

    String phone = request.phone().trim();
    String phoneHash = hashPhone(phone);
    if (phoneExists(phoneHash, coach.getId())) {
      throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
    }

    String idCardNo = request.idCardNo().trim();
    String idCardHash = hashIdCard(idCardNo);
    if (idCardExists(idCardHash, coach.getId())) {
      throw new BusinessException(ErrorCode.ID_CARD_ALREADY_EXISTS);
    }

    String beforeSnapshot = toAuditSnapshot(coach);
    Integer previousStatus = coach.getStatus();

    coach.setPhone(encryptPhone(phone));
    coach.setPhoneHash(phoneHash);
    coach.setAvatarUrl(StringUtils.trimToNull(request.avatarUrl()));
    coach.setName(request.name().trim());
    coach.setGender(trim(request.gender()));
    coach.setAge(request.age());
    coach.setEmail(StringUtils.trimToNull(request.email()));
    coach.setWechatQrUrl(StringUtils.trimToNull(request.wechatQrUrl()));
    coach.setIdCardNo(encryptIdCard(idCardNo));
    coach.setIdCardHash(idCardHash);
    coach.setTeachingYears(request.teachingYears());
    coach.setTotalStudents(request.totalStudents());
    coach.setTotalHours(request.totalHours());
    coach.setTeachingStrokes(joinStrokes(request.teachingStrokes()));
    coach.setBio(StringUtils.trimToNull(request.bio()));
    coach.setReferencePrice(request.referencePrice());

    int affected = coachMapper.updateById(coach);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.USER_CONCURRENTLY_UPDATED);
    }

    saveCertificates(coach.getId(), request.certificates(), true);

    writeAuditLog(
        adminId,
        coach.getId(),
        ACTION_UPDATE,
        beforeSnapshot,
        toAuditSnapshot(coach),
        null,
        getClientIp(httpRequest));

    writeCoachAuditLog(
        adminId, coach.getId(), ACTION_UPDATE, previousStatus, coach.getStatus(), null);

    return toDetailResponse(coach);
  }

  @Transactional
  public AdminCoachDetailResponse cancelEntry(
      Long adminId, AdminCoachCancelEntryRequest request, HttpServletRequest httpRequest) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_COACH_CANCEL_ENTRY);

    Coach coach = findCoach(request.coachId());
    if (coach.getStatus() != CoachStatus.APPROVED.getValue()) {
      throw new BusinessException(ErrorCode.COACH_STATUS_NOT_APPROVED);
    }

    String beforeSnapshot = toAuditSnapshot(coach);
    Integer previousStatus = coach.getStatus();
    coach.setStatus(CoachStatus.RESIGNED.getValue());

    int affected = coachMapper.updateById(coach);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.USER_CONCURRENTLY_UPDATED);
    }

    writeAuditLog(
        adminId,
        coach.getId(),
        ACTION_CANCEL_ENTRY,
        beforeSnapshot,
        toAuditSnapshot(coach),
        request.reason(),
        getClientIp(httpRequest));

    writeCoachAuditLog(
        adminId, coach.getId(), ACTION_CANCEL_ENTRY, previousStatus, coach.getStatus(),
        request.reason());

    // TODO: 触发 US-041 教练离职后续处理（清空可约时段、解绑学员等）

    return toDetailResponse(coach);
  }

  private Coach findCoach(Long coachId) {
    Coach coach = coachMapper.selectById(coachId);
    if (coach == null) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }
    return coach;
  }

  private void validateCoachFields(
      Integer age,
      String idCardNo,
      BigDecimal referencePrice,
      Integer totalStudents,
      Integer totalHours,
      Integer teachingYears,
      List<CoachCertificateItem> certificates) {
    if (age == null || age < MIN_AGE || age > MAX_AGE) {
      throw new BusinessException(ErrorCode.INVALID_AGE);
    }
    if (idCardNo == null || !idCardNo.trim().matches("\\d{17}[\\dXx]")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入 18 位有效身份证号");
    }
    if (referencePrice == null
        || referencePrice.compareTo(MIN_PRICE) < 0
        || referencePrice.compareTo(MAX_PRICE) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "参考单价需在 50-2000 元之间");
    }
    if (totalStudents == null || totalStudents < MIN_STUDENTS || totalStudents > MAX_STUDENTS) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "总学员数需在 0-99999 之间");
    }
    if (totalHours == null || totalHours < MIN_HOURS || totalHours > MAX_HOURS) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "总课时数需在 0-99999 之间");
    }
    if (teachingYears == null
        || teachingYears < MIN_TEACHING_YEARS
        || teachingYears > MAX_TEACHING_YEARS) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任教年限需在 0-60 之间");
    }
    validateCertificates(certificates);
  }

  private void validateCertificates(List<CoachCertificateItem> certificates) {
    if (certificates == null || certificates.isEmpty()) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST, "请上传身份证正反面、教练资格证、健康证和个人形象照");
    }
    for (CoachCertificateItem item : certificates) {
      if (!CoachCertificateType.isValid(item.certType())) {
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
        throw new BusinessException(
            ErrorCode.BAD_REQUEST, "请上传身份证正反面、教练资格证、健康证和个人形象照");
      }
    }
  }

  private void saveCertificates(
      Long coachId, List<CoachCertificateItem> certificates, boolean replaceAll) {
    if (replaceAll) {
      certificateMapper.deleteByCoachId(coachId);
    }
    if (certificates == null || certificates.isEmpty()) {
      return;
    }

    Map<String, Integer> typeCounter =
        CoachCertificateType.valuesSet().stream()
            .collect(Collectors.toMap(t -> t, t -> 0));
    List<CoachCertificate> entities = new ArrayList<>();
    int globalOrder = 0;
    for (CoachCertificateItem item : certificates) {
      if (item.certType() == null || item.imageUrl() == null) {
        continue;
      }
      CoachCertificate entity = new CoachCertificate();
      entity.setCoachId(coachId);
      entity.setCertType(item.certType());
      entity.setImageUrl(item.imageUrl().trim());
      entity.setSortOrder(globalOrder++);
      typeCounter.put(item.certType(), typeCounter.get(item.certType()) + 1);
      entities.add(entity);
    }
    if (!entities.isEmpty()) {
      entities.sort(
          Comparator.comparing(CoachCertificate::getCertType)
              .thenComparing(CoachCertificate::getSortOrder));
      for (int i = 0; i < entities.size(); i++) {
        entities.get(i).setSortOrder(i);
      }
      for (CoachCertificate entity : entities) {
        certificateMapper.insert(entity);
      }
    }
  }

  private boolean phoneExists(String phoneHash, Long excludeCoachId) {
    LambdaQueryWrapper<Coach> wrapper =
        new LambdaQueryWrapper<Coach>().eq(Coach::getPhoneHash, phoneHash);
    if (excludeCoachId != null) {
      wrapper.ne(Coach::getId, excludeCoachId);
    }
    return coachMapper.selectCount(wrapper) > 0;
  }

  private boolean idCardExists(String idCardHash, Long excludeCoachId) {
    LambdaQueryWrapper<Coach> wrapper =
        new LambdaQueryWrapper<Coach>().eq(Coach::getIdCardHash, idCardHash);
    if (excludeCoachId != null) {
      wrapper.ne(Coach::getId, excludeCoachId);
    }
    return coachMapper.selectCount(wrapper) > 0;
  }

  private AdminCoachListItemResponse toListItem(Coach coach) {
    Long studentCount = packageMapper.countActiveStudentsByCoachId(coach.getId());
    return new AdminCoachListItemResponse(
        coach.getId(),
        coach.getName(),
        coach.getGender(),
        coach.getAge(),
        coach.getTeachingYears(),
        coach.getTeachingStrokes(),
        coach.getApprovedAt(),
        computeTenure(coach.getApprovedAt()),
        coach.getStatus(),
        studentCount == null ? 0L : studentCount,
        resolveRealtimeStatus(coach.getStatus()));
  }

  private AdminCoachDetailResponse toDetailResponse(Coach coach) {
    List<CoachCertificate> certs = certificateMapper.findByCoachId(coach.getId());
    List<CoachApplication> applications =
        applicationMapper.selectList(
            new LambdaQueryWrapper<CoachApplication>()
                .eq(CoachApplication::getCoachId, coach.getId())
                .orderByDesc(CoachApplication::getCreatedAt));
    List<CoachAuditLog> auditLogs = coachAuditLogMapper.findByCoachId(coach.getId());

    return new AdminCoachDetailResponse(
        coach.getId(),
        coach.getAvatarUrl(),
        decryptPhone(coach.getPhone()),
        coach.getName(),
        coach.getGender(),
        coach.getAge(),
        coach.getEmail(),
        coach.getWechatQrUrl(),
        IdCardEncryptor.mask(decryptIdCard(coach.getIdCardNo())),
        coach.getTeachingYears(),
        coach.getTotalStudents(),
        coach.getTotalHours(),
        coach.getTeachingStrokes(),
        coach.getBio(),
        coach.getReferencePrice(),
        coach.getStatus(),
        coach.getApprovedAt(),
        coach.getCreatedAt(),
        coach.getUpdatedAt(),
        coach.getVersion(),
        certs.stream().map(this::toCertificateResponse).toList(),
        applications.stream().map(this::toApplicationHistoryResponse).toList(),
        auditLogs.stream().map(this::toAuditLogResponse).toList());
  }

  private AdminCoachCertificateResponse toCertificateResponse(CoachCertificate cert) {
    return new AdminCoachCertificateResponse(
        cert.getId(), cert.getCertType(), cert.getImageUrl(), cert.getSortOrder());
  }

  private AdminCoachApplicationHistoryResponse toApplicationHistoryResponse(
      CoachApplication application) {
    return new AdminCoachApplicationHistoryResponse(
        application.getId(),
        application.getStatus(),
        String.valueOf(application.getPreviousCoachStatus()),
        application.getName(),
        application.getPhone(),
        application.getGender(),
        application.getAge(),
        application.getEmail(),
        IdCardEncryptor.mask(decryptIdCard(application.getIdCardNo())),
        application.getTeachingYears(),
        application.getTotalStudents(),
        application.getTotalHours(),
        application.getTeachingStrokes(),
        application.getBio(),
        application.getReferencePrice(),
        application.getSubmittedAt(),
        application.getApprovedAt(),
        application.getApprovedBy(),
        application.getRejectionReason(),
        application.getCreatedAt());
  }

  private AdminCoachAuditLogResponse toAuditLogResponse(CoachAuditLog log) {
    return new AdminCoachAuditLogResponse(
        log.getId(),
        log.getAdminId(),
        log.getAction(),
        log.getFromStatus(),
        log.getToStatus(),
        log.getReason(),
        log.getCreatedAt());
  }

  private String computeTenure(LocalDateTime approvedAt) {
    if (approvedAt == null) {
      return "-";
    }
    LocalDate start = approvedAt.toLocalDate();
    LocalDate now = LocalDate.now();
    if (start.isAfter(now)) {
      return "不足 1 个月";
    }
    Period period = Period.between(start, now);
    int years = period.getYears();
    int months = period.getMonths();
    if (years == 0 && months == 0) {
      long days = ChronoUnit.DAYS.between(start, now);
      return days == 0 ? "不足 1 个月" : days + " 天";
    }
    if (years == 0) {
      return months + " 个月";
    }
    return years + " 年 " + months + " 个月";
  }

  private String resolveRealtimeStatus(Integer status) {
    if (status == null) {
      return "未知";
    }
    return switch (status) {
      case 1 -> "空闲中";
      case 4 -> "离职申请中";
      case 3 -> "已离职";
      case 0 -> "待审核";
      case 2 -> "已驳回";
      default -> "未知";
    };
  }

  private String encryptPhone(String phone) {
    try {
      return phoneEncryptor.encrypt(phone);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号加密失败", e);
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
      return null;
    }
  }

  private String hashPhone(String phone) {
    try {
      return phoneEncryptor.hash(phone);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号哈希失败", e);
    }
  }

  private String encryptIdCard(String idCardNo) {
    if (idCardNo == null) {
      return null;
    }
    try {
      return idCardEncryptor.encrypt(idCardNo);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "身份证号加密失败", e);
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
      return null;
    }
  }

  private String hashIdCard(String idCardNo) {
    if (idCardNo == null) {
      return null;
    }
    try {
      return phoneEncryptor.hash(idCardNo);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "身份证号哈希失败", e);
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

  @SneakyThrows
  private String toSnapshot(Coach coach) {
    return objectMapper.writeValueAsString(toDetailResponse(coach));
  }

  @SneakyThrows
  private String toAuditSnapshot(Coach coach) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("id", coach.getId());
    snapshot.put("name", coach.getName());
    snapshot.put("phone", PhoneEncryptor.mask(decryptPhone(coach.getPhone())));
    snapshot.put("phoneHash", coach.getPhoneHash());
    snapshot.put("avatarUrl", coach.getAvatarUrl());
    snapshot.put("gender", coach.getGender());
    snapshot.put("age", coach.getAge());
    snapshot.put("email", coach.getEmail());
    snapshot.put("wechatQrUrl", coach.getWechatQrUrl());
    snapshot.put("idCardNo", IdCardEncryptor.mask(decryptIdCard(coach.getIdCardNo())));
    snapshot.put("idCardHash", coach.getIdCardHash());
    snapshot.put("teachingYears", coach.getTeachingYears());
    snapshot.put("totalStudents", coach.getTotalStudents());
    snapshot.put("totalHours", coach.getTotalHours());
    snapshot.put("teachingStrokes", coach.getTeachingStrokes());
    snapshot.put("bio", coach.getBio());
    snapshot.put("referencePrice", coach.getReferencePrice());
    snapshot.put("status", coach.getStatus());
    snapshot.put("approvedAt", coach.getApprovedAt());
    snapshot.put("version", coach.getVersion());
    return objectMapper.writeValueAsString(snapshot);
  }

  private void writeAuditLog(
      Long adminId,
      Long coachId,
      String action,
      String beforeSnapshot,
      String afterSnapshot,
      String reason,
      String ip) {
    AuditLog auditLog = new AuditLog();
    auditLog.setActorType(ACTOR_TYPE);
    auditLog.setActorId(adminId);
    auditLog.setTargetType(TARGET_TYPE);
    auditLog.setTargetId(coachId);
    auditLog.setAction(action);
    auditLog.setBeforeSnapshot(beforeSnapshot);
    auditLog.setAfterSnapshot(afterSnapshot);
    auditLog.setReason(reason);
    auditLog.setIp(ip);
    auditLog.setCreatedAt(LocalDateTime.now());
    auditLogMapper.insert(auditLog);
  }

  private void writeCoachAuditLog(
      Long adminId,
      Long coachId,
      String action,
      Integer fromStatus,
      Integer toStatus,
      String reason) {
    CoachAuditLog log = new CoachAuditLog();
    log.setCoachId(coachId);
    log.setApplicationId(null);
    log.setAdminId(adminId);
    log.setAction(action);
    log.setFromStatus(fromStatus);
    log.setToStatus(toStatus);
    log.setReason(reason);
    log.setCreatedAt(LocalDateTime.now());
    coachAuditLogMapper.insert(log);
  }

  private String getClientIp(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isBlank()) {
      ip = request.getRemoteAddr();
    } else {
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }
}
