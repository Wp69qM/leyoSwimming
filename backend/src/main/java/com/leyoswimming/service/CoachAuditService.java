package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminCoachApplicationApproveRequest;
import com.leyoswimming.dto.request.AdminCoachApplicationListRequest;
import com.leyoswimming.dto.request.AdminCoachApplicationRejectRequest;
import com.leyoswimming.dto.response.AdminCoachApplicationDetailResponse;
import com.leyoswimming.dto.response.AdminCoachApplicationListItemResponse;
import com.leyoswimming.dto.response.AdminCoachApplicationListResponse;
import com.leyoswimming.dto.response.AdminCoachApplicationStatsResponse;
import com.leyoswimming.dto.response.CoachApplicationCertificateResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachApplication;
import com.leyoswimming.entity.CoachAuditLog;
import com.leyoswimming.entity.CoachCertificate;
import com.leyoswimming.entity.CoachCertificateApplication;
import com.leyoswimming.enums.CoachApplicationStatus;
import com.leyoswimming.enums.CoachAuditAction;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachApplicationMapper;
import com.leyoswimming.repository.CoachAuditLogMapper;
import com.leyoswimming.repository.CoachCertificateApplicationMapper;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachAuditService {

  private static final Duration AUDIT_LOCK_TTL = Duration.ofSeconds(30);
  private static final int MAX_HISTORY_RECORDS = 20;
  private static final int MAX_AUDIT_LOG_RECORDS = 50;

  private final CoachApplicationMapper applicationMapper;
  private final CoachCertificateApplicationMapper certificateApplicationMapper;
  private final CoachCertificateMapper certificateMapper;
  private final CoachAuditLogMapper auditLogMapper;
  private final CoachMapper coachMapper;
  private final PhoneEncryptor phoneEncryptor;
  private final IdCardEncryptor idCardEncryptor;
  private final DistributedLockHelper distributedLockHelper;
  private final AdminPermissionHelper adminPermissionHelper;

  @Transactional(readOnly = true)
  public AdminCoachApplicationListResponse list(AdminCoachApplicationListRequest request) {
    LambdaQueryWrapper<CoachApplication> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(request.status())) {
      wrapper.eq(CoachApplication::getStatus, request.status());
    }
    if (StringUtils.isNotBlank(request.keyword())) {
      String keyword = request.keyword().trim();
      if (keyword.matches("\\d{11}")) {
        try {
          String phoneHash = phoneEncryptor.hash(keyword);
          wrapper.eq(CoachApplication::getPhoneHash, phoneHash);
        } catch (Exception e) {
          log.error("Failed to hash phone keyword", e);
          wrapper.eq(CoachApplication::getPhoneHash, "");
        }
      } else {
        wrapper.and(w -> w.like(CoachApplication::getName, keyword));
      }
    }
    wrapper.orderByDesc(CoachApplication::getSubmittedAt, CoachApplication::getCreatedAt);

    Page<CoachApplication> page = new Page<>(request.page(), request.pageSize());
    Page<CoachApplication> result = applicationMapper.selectPage(page, wrapper);

    List<AdminCoachApplicationListItemResponse> list =
        result.getRecords().stream().map(this::toListItem).toList();
    return new AdminCoachApplicationListResponse(
        list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
  }

  @Transactional(readOnly = true)
  public AdminCoachApplicationStatsResponse stats() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
    LocalDateTime yesterdayStart = todayStart.minusDays(1);

    long pendingCount =
        applicationMapper.selectCount(
            new LambdaQueryWrapper<CoachApplication>()
                .eq(CoachApplication::getStatus, CoachApplicationStatus.PENDING.getValue()));

    long todayNewCount =
        applicationMapper.selectCount(
            new LambdaQueryWrapper<CoachApplication>()
                .ge(CoachApplication::getCreatedAt, todayStart)
                .ne(CoachApplication::getStatus, CoachApplicationStatus.DRAFT.getValue()));

    long overdue24hCount =
        applicationMapper.selectCount(
            new LambdaQueryWrapper<CoachApplication>()
                .eq(CoachApplication::getStatus, CoachApplicationStatus.PENDING.getValue())
                .lt(CoachApplication::getSubmittedAt, yesterdayStart));

    long todayApprovedCount =
        applicationMapper.selectCount(
            new LambdaQueryWrapper<CoachApplication>()
                .eq(CoachApplication::getStatus, CoachApplicationStatus.APPROVED.getValue())
                .ge(CoachApplication::getApprovedAt, todayStart));

    long todayRejectedCount =
        applicationMapper.selectCount(
            new LambdaQueryWrapper<CoachApplication>()
                .eq(CoachApplication::getStatus, CoachApplicationStatus.REJECTED.getValue())
                .ge(CoachApplication::getUpdatedAt, todayStart));

    return new AdminCoachApplicationStatsResponse(
        pendingCount, todayNewCount, overdue24hCount, todayApprovedCount, todayRejectedCount);
  }

  @Transactional(readOnly = true)
  public AdminCoachApplicationDetailResponse detail(Long applicationId) {
    CoachApplication application =
        applicationMapper.selectById(Objects.requireNonNull(applicationId));
    if (application == null) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "入驻申请不存在");
    }

    List<CoachCertificateApplication> certs =
        certificateApplicationMapper.findByApplicationId(applicationId);
    List<CoachApplication> history = findHistoryByCoachId(application.getCoachId());
    List<CoachAuditLog> logs = findAuditLogsByCoachId(application.getCoachId());

    return toDetailResponse(application, certs, history, logs);
  }

  @Transactional
  public void approve(Long adminId, AdminCoachApplicationApproveRequest request) {
    adminPermissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_COACH_WRITE);
    var lockToken =
        distributedLockHelper.lock(
            "coach_audit", String.valueOf(request.applicationId()), AUDIT_LOCK_TTL);
    distributedLockHelper.unlockAfterTransaction(lockToken);

    CoachApplication application = applicationMapper.selectById(request.applicationId());
    if (application == null) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "入驻申请不存在");
    }
    if (!CoachApplicationStatus.PENDING.getValue().equals(application.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该申请已审核，无需重复操作");
    }

    Coach coach = coachMapper.selectById(application.getCoachId());
    if (coach == null) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }

    copyApplicationToCoach(application, coach);
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coach.setApprovedAt(LocalDateTime.now());
    coachMapper.updateById(coach);

    application.setStatus(CoachApplicationStatus.APPROVED.getValue());
    application.setApprovedAt(LocalDateTime.now());
    application.setApprovedBy(adminId);
    applicationMapper.updateById(application);

    copyCertificatesToEffective(application.getId(), coach.getId());

    writeAuditLog(
        coach.getId(),
        application.getId(),
        adminId,
        CoachAuditAction.APPROVE,
        CoachStatus.PENDING.getValue(),
        CoachStatus.APPROVED.getValue(),
        request.remark());
  }

  @Transactional
  public void reject(Long adminId, AdminCoachApplicationRejectRequest request) {
    adminPermissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_COACH_WRITE);
    var lockToken =
        distributedLockHelper.lock(
            "coach_audit", String.valueOf(request.applicationId()), AUDIT_LOCK_TTL);
    distributedLockHelper.unlockAfterTransaction(lockToken);

    CoachApplication application = applicationMapper.selectById(request.applicationId());
    if (application == null) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "入驻申请不存在");
    }
    if (!CoachApplicationStatus.PENDING.getValue().equals(application.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该申请已审核，无需重复操作");
    }

    Coach coach = coachMapper.selectById(application.getCoachId());
    if (coach == null) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }

    Integer previousStatus = application.getPreviousCoachStatus();
    int targetStatus = resolveRejectedCoachStatus(previousStatus);

    coach.setStatus(targetStatus);
    coachMapper.updateById(coach);

    application.setStatus(CoachApplicationStatus.REJECTED.getValue());
    application.setRejectionReason(request.reason());
    applicationMapper.updateById(application);

    writeAuditLog(
        coach.getId(),
        application.getId(),
        adminId,
        CoachAuditAction.REJECT,
        CoachStatus.PENDING.getValue(),
        targetStatus,
        request.reason());
  }

  private int resolveRejectedCoachStatus(Integer previousStatus) {
    if (previousStatus == null) {
      return CoachStatus.REJECTED.getValue();
    }
    if (previousStatus == CoachStatus.RESIGNED.getValue()) {
      return CoachStatus.RESIGNED.getValue();
    }
    return CoachStatus.REJECTED.getValue();
  }

  private void copyApplicationToCoach(CoachApplication application, Coach coach) {
    coach.setName(application.getName());
    coach.setGender(application.getGender());
    coach.setAge(application.getAge());
    coach.setEmail(application.getEmail());
    coach.setWechatQrUrl(application.getWechatQrUrl());
    coach.setIdCardNo(application.getIdCardNo());
    coach.setTeachingYears(application.getTeachingYears());
    coach.setTotalStudents(application.getTotalStudents());
    coach.setTotalHours(application.getTotalHours());
    coach.setTeachingStrokes(application.getTeachingStrokes());
    coach.setBio(application.getBio());
    coach.setReferencePrice(application.getReferencePrice());
    coach.setPhone(application.getPhone());
  }

  private void copyCertificatesToEffective(Long applicationId, Long coachId) {
    LambdaQueryWrapper<CoachCertificate> deleteWrapper =
        new LambdaQueryWrapper<CoachCertificate>().eq(CoachCertificate::getCoachId, coachId);
    certificateMapper.delete(deleteWrapper);

    List<CoachCertificateApplication> certs =
        certificateApplicationMapper.findByApplicationId(applicationId);
    if (certs == null || certs.isEmpty()) {
      return;
    }
    List<CoachCertificate> effective =
        certs.stream()
            .map(
                c -> {
                  CoachCertificate cert = new CoachCertificate();
                  cert.setCoachId(coachId);
                  cert.setCertType(c.getCertType());
                  cert.setImageUrl(c.getImageUrl());
                  cert.setSortOrder(c.getSortOrder());
                  return cert;
                })
            .sorted(
                Comparator.comparing(CoachCertificate::getCertType)
                    .thenComparing(CoachCertificate::getSortOrder))
            .toList();
    for (int i = 0; i < effective.size(); i++) {
      effective.get(i).setSortOrder(i);
      certificateMapper.insert(effective.get(i));
    }

    updateCoachCertificatesJson(coachId, effective);
  }

  private void updateCoachCertificatesJson(Long coachId, List<CoachCertificate> certs) {
    List<Coach.CoachCertificate> jsonCerts =
        certs.stream()
            .map(
                c -> {
                  Coach.CoachCertificate cert = new Coach.CoachCertificate();
                  cert.setName(c.getCertType());
                  cert.setUrl(c.getImageUrl());
                  return cert;
                })
            .toList();
    Coach coach = coachMapper.selectById(coachId);
    coach.setCertificates(jsonCerts);
    coachMapper.updateById(coach);
  }

  private List<CoachApplication> findHistoryByCoachId(Long coachId) {
    LambdaQueryWrapper<CoachApplication> wrapper =
        new LambdaQueryWrapper<CoachApplication>()
            .eq(CoachApplication::getCoachId, coachId)
            .ne(CoachApplication::getStatus, CoachApplicationStatus.DRAFT.getValue())
            .orderByDesc(CoachApplication::getCreatedAt)
            .last("LIMIT " + MAX_HISTORY_RECORDS);
    return applicationMapper.selectList(wrapper);
  }

  private List<CoachAuditLog> findAuditLogsByCoachId(Long coachId) {
    LambdaQueryWrapper<CoachAuditLog> wrapper =
        new LambdaQueryWrapper<CoachAuditLog>()
            .eq(CoachAuditLog::getCoachId, coachId)
            .ne(CoachAuditLog::getAction, CoachAuditAction.DRAFT_SAVE.getValue())
            .orderByDesc(CoachAuditLog::getCreatedAt)
            .last("LIMIT " + MAX_AUDIT_LOG_RECORDS);
    return auditLogMapper.selectList(wrapper);
  }

  private void writeAuditLog(
      Long coachId,
      Long applicationId,
      Long adminId,
      CoachAuditAction action,
      Integer fromStatus,
      Integer toStatus,
      String reason) {
    CoachAuditLog log = new CoachAuditLog();
    log.setCoachId(coachId);
    log.setApplicationId(applicationId);
    log.setAdminId(adminId);
    log.setAction(action.getValue());
    log.setFromStatus(fromStatus);
    log.setToStatus(toStatus);
    log.setReason(reason);
    log.setCreatedAt(LocalDateTime.now());
    auditLogMapper.insert(log);
  }

  private AdminCoachApplicationListItemResponse toListItem(CoachApplication application) {
    return new AdminCoachApplicationListItemResponse(
        application.getCoachId(),
        application.getId(),
        application.getName(),
        application.getGender(),
        application.getAge(),
        application.getTeachingYears(),
        application.getTeachingStrokes(),
        application.getSubmittedAt(),
        application.getStatus(),
        application.getPreviousCoachStatus(),
        PhoneEncryptor.mask(decryptPhone(application.getPhone())));
  }

  private AdminCoachApplicationDetailResponse toDetailResponse(
      CoachApplication application,
      List<CoachCertificateApplication> certs,
      List<CoachApplication> history,
      List<CoachAuditLog> logs) {
    return new AdminCoachApplicationDetailResponse(
        application.getCoachId(),
        application.getId(),
        application.getStatus(),
        application.getPreviousCoachStatus(),
        application.getName(),
        PhoneEncryptor.mask(decryptPhone(application.getPhone())),
        application.getGender(),
        application.getAge(),
        application.getEmail(),
        toAbsoluteUrl(application.getWechatQrUrl()),
        application.getIdCardNo() != null
            ? IdCardEncryptor.mask(decryptIdCard(application.getIdCardNo()))
            : null,
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
        mapCertificates(certs),
        history.stream().map(this::toHistoryItem).toList(),
        logs.stream().map(this::toAuditLogItem).toList());
  }

  private AdminCoachApplicationDetailResponse.CoachApplicationHistoryItem toHistoryItem(
      CoachApplication application) {
    return new AdminCoachApplicationDetailResponse.CoachApplicationHistoryItem(
        application.getId(),
        application.getStatus(),
        application.getSubmittedAt(),
        application.getApprovedAt(),
        application.getApprovedBy(),
        application.getRejectionReason());
  }

  private AdminCoachApplicationDetailResponse.CoachAuditLogItem toAuditLogItem(CoachAuditLog log) {
    return new AdminCoachApplicationDetailResponse.CoachAuditLogItem(
        log.getId(),
        log.getAdminId(),
        log.getAction(),
        log.getFromStatus(),
        log.getToStatus(),
        log.getReason(),
        log.getCreatedAt());
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
                    c.getId(), c.getCertType(), toAbsoluteUrl(c.getImageUrl()), c.getSortOrder()))
        .toList();
  }

  private String toAbsoluteUrl(String path) {
    if (path == null || path.isBlank() || path.startsWith("http://") || path.startsWith("https://")) {
      return path;
    }
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return path;
    }
    String normalizedPath = path.startsWith("/") ? path : "/" + path;
    return ServletUriComponentsBuilder.fromRequestUri(attributes.getRequest())
        .replacePath(normalizedPath)
        .toUriString();
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
}
