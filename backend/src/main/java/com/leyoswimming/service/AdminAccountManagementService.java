package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminAccountAddRequest;
import com.leyoswimming.dto.request.AdminAccountDeleteRequest;
import com.leyoswimming.dto.request.AdminAccountListRequest;
import com.leyoswimming.dto.request.AdminAccountResetPasswordRequest;
import com.leyoswimming.dto.request.AdminAccountToggleStatusRequest;
import com.leyoswimming.dto.request.AdminAccountUpdateRequest;
import com.leyoswimming.dto.response.AdminAccountAuditLogResponse;
import com.leyoswimming.dto.response.AdminAccountDetailResponse;
import com.leyoswimming.dto.response.AdminAccountListItemResponse;
import com.leyoswimming.dto.response.AdminAccountListResponse;
import com.leyoswimming.dto.response.AdminAccountResetPasswordResponse;
import com.leyoswimming.entity.AdminAuditLog;
import com.leyoswimming.entity.AdminSession;
import com.leyoswimming.entity.AdminUser;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AdminAuditLogMapper;
import com.leyoswimming.repository.AdminSessionMapper;
import com.leyoswimming.repository.AdminUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccountManagementService {

  private static final int RANDOM_PASSWORD_LENGTH = 12;
  private static final String ACTION_CREATE = "CREATE";
  private static final String ACTION_UPDATE = "UPDATE";
  private static final String ACTION_DISABLE = "DISABLE";
  private static final String ACTION_ENABLE = "ENABLE";
  private static final String ACTION_DELETE = "DELETE";
  private static final String ACTION_RESET_PASSWORD = "RESET_PASSWORD";
  private static final String ROLE_SUPER_ADMIN = "super_admin";
  private static final String ROLE_ADMIN = "admin";

  private final AdminUserMapper adminUserMapper;
  private final AdminSessionMapper adminSessionMapper;
  private final AdminAuditLogMapper adminAuditLogMapper;
  private final PasswordEncoder passwordEncoder;
  private final AdminPermissionHelper permissionHelper;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public AdminAccountListResponse list(Long currentAdminId, AdminAccountListRequest request) {
    boolean isSuperAdmin = permissionHelper.isSuperAdmin(currentAdminId);

    LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
    wrapper.isNull(AdminUser::getDeletedAt);
    if (StringUtils.isNotBlank(request.role())) {
      wrapper.eq(AdminUser::getRole, request.role().trim().toLowerCase());
    }
    if (request.status() != null) {
      wrapper.eq(AdminUser::getStatus, request.status());
    }
    if (StringUtils.isNotBlank(request.keyword())) {
      String keyword = request.keyword().trim();
      wrapper.and(
          w ->
              w.like(AdminUser::getUsername, keyword).or().like(AdminUser::getName, keyword));
    }
    wrapper.orderByDesc(AdminUser::getCreatedAt);

    Page<AdminUser> page = new Page<>(request.page(), request.pageSize());
    Page<AdminUser> result = adminUserMapper.selectPage(page, wrapper);

    List<AdminAccountListItemResponse> list =
        result.getRecords().stream()
            .map(admin -> toListItem(admin, currentAdminId, isSuperAdmin))
            .toList();
    return new AdminAccountListResponse(
        list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
  }

  @Transactional(readOnly = true)
  public AdminAccountDetailResponse detail(Long currentAdminId, Long targetAdminId) {
    boolean isSuperAdmin = permissionHelper.isSuperAdmin(currentAdminId);
    if (!isSuperAdmin && !Objects.equals(currentAdminId, targetAdminId)) {
      throw new BusinessException(ErrorCode.ADMIN_PERMISSION_DENIED);
    }
    AdminUser admin = findActiveAdmin(targetAdminId);
    List<AdminAccountAuditLogResponse> auditLogs = fetchRecentAuditLogs(targetAdminId);
    return toDetailResponse(admin, auditLogs);
  }

  @Transactional
  public AdminAccountDetailResponse add(
      Long currentAdminId, AdminAccountAddRequest request, HttpServletRequest httpRequest) {
    permissionHelper.checkSuperAdmin(currentAdminId);

    String username = request.username().trim().toLowerCase();
    if (usernameExists(username, null)) {
      throw new BusinessException(ErrorCode.ADMIN_USERNAME_ALREADY_EXISTS);
    }

    AdminUser admin = new AdminUser();
    admin.setUsername(username);
    admin.setPasswordHash(passwordEncoder.encode(request.password()));
    admin.setName(request.name().trim());
    admin.setRole(request.role().trim().toLowerCase());
    admin.setStatus(0);

    adminUserMapper.insert(admin);

    writeAuditLog(
        currentAdminId,
        admin.getId(),
        ACTION_CREATE,
        null,
        toSnapshot(admin),
        null,
        getClientIp(httpRequest));

    return toDetailResponse(admin, List.of());
  }

  @Transactional
  public AdminAccountDetailResponse update(
      Long currentAdminId, AdminAccountUpdateRequest request, HttpServletRequest httpRequest) {
    permissionHelper.checkSuperAdmin(currentAdminId);

    AdminUser admin = findActiveAdmin(request.adminId());
    if (!Objects.equals(admin.getVersion(), request.version())) {
      throw new BusinessException(ErrorCode.ADMIN_CONCURRENTLY_UPDATED);
    }
    String beforeSnapshot = toSnapshot(admin);

    if (StringUtils.isNotBlank(request.name())) {
      admin.setName(request.name().trim());
    }
    if (StringUtils.isNotBlank(request.role())) {
      admin.setRole(request.role().trim().toLowerCase());
    }

    int affected = adminUserMapper.updateById(admin);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.ADMIN_CONCURRENTLY_UPDATED);
    }

    writeAuditLog(
        currentAdminId,
        admin.getId(),
        ACTION_UPDATE,
        beforeSnapshot,
        toSnapshot(admin),
        null,
        getClientIp(httpRequest));

    return toDetailResponse(admin, List.of());
  }

  @Transactional
  public AdminAccountDetailResponse toggleStatus(
      Long currentAdminId,
      AdminAccountToggleStatusRequest request,
      HttpServletRequest httpRequest) {
    permissionHelper.checkSuperAdmin(currentAdminId);

    if (Objects.equals(currentAdminId, request.adminId())) {
      throw new BusinessException(ErrorCode.ADMIN_CANNOT_DISABLE_SELF);
    }

    AdminUser admin = findActiveAdmin(request.adminId());
    int targetStatus = request.status();
    if (admin.getStatus() == targetStatus) {
      return toDetailResponse(admin, List.of());
    }

    if (targetStatus == 1 && ROLE_SUPER_ADMIN.equalsIgnoreCase(admin.getRole())) {
      if (countEnabledSuperAdmins() <= 1) {
        throw new BusinessException(ErrorCode.ADMIN_LAST_SUPER_ADMIN_PROTECTED);
      }
    }

    String beforeSnapshot = toSnapshot(admin);
    admin.setStatus(targetStatus);
    int affected = adminUserMapper.updateById(admin);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.ADMIN_CONCURRENTLY_UPDATED);
    }

    if (targetStatus == 1 && ROLE_SUPER_ADMIN.equalsIgnoreCase(admin.getRole())) {
      if (countEnabledSuperAdmins() == 0) {
        throw new BusinessException(ErrorCode.ADMIN_LAST_SUPER_ADMIN_PROTECTED);
      }
    }

    if (targetStatus == 1) {
      revokeSessions(admin.getId());
    }

    String action = targetStatus == 0 ? ACTION_ENABLE : ACTION_DISABLE;
    writeAuditLog(
        currentAdminId,
        admin.getId(),
        action,
        beforeSnapshot,
        toSnapshot(admin),
        request.reason(),
        getClientIp(httpRequest));

    return toDetailResponse(admin, List.of());
  }

  @Transactional
  public void delete(
      Long currentAdminId, AdminAccountDeleteRequest request, HttpServletRequest httpRequest) {
    permissionHelper.checkSuperAdmin(currentAdminId);

    if (Objects.equals(currentAdminId, request.adminId())) {
      throw new BusinessException(ErrorCode.ADMIN_CANNOT_DELETE_SELF);
    }

    AdminUser admin = findActiveAdmin(request.adminId());
    if (ROLE_SUPER_ADMIN.equalsIgnoreCase(admin.getRole()) && countEnabledSuperAdmins() <= 1) {
      throw new BusinessException(ErrorCode.ADMIN_LAST_SUPER_ADMIN_PROTECTED);
    }

    String beforeSnapshot = toSnapshot(admin);

    revokeSessions(admin.getId());

    admin.setStatus(1);
    admin.setDeletedAt(LocalDateTime.now());
    int affected = adminUserMapper.updateById(admin);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.ADMIN_CONCURRENTLY_UPDATED);
    }

    writeAuditLog(
        currentAdminId,
        admin.getId(),
        ACTION_DELETE,
        beforeSnapshot,
        null,
        request.reason(),
        getClientIp(httpRequest));
  }

  @Transactional
  public AdminAccountResetPasswordResponse resetPassword(
      Long currentAdminId,
      AdminAccountResetPasswordRequest request,
      HttpServletRequest httpRequest) {
    permissionHelper.checkSuperAdmin(currentAdminId);

    AdminUser admin = findActiveAdmin(request.adminId());
    String tempPassword = generateRandomPassword();
    admin.setPasswordHash(passwordEncoder.encode(tempPassword));
    int affected = adminUserMapper.updateById(admin);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.ADMIN_CONCURRENTLY_UPDATED);
    }

    revokeSessions(admin.getId());

    writeAuditLog(
        currentAdminId,
        admin.getId(),
        ACTION_RESET_PASSWORD,
        null,
        null,
        null,
        getClientIp(httpRequest));

    return new AdminAccountResetPasswordResponse(tempPassword);
  }

  private AdminUser findActiveAdmin(Long adminId) {
    AdminUser admin = adminUserMapper.selectById(adminId);
    if (admin == null || admin.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
    }
    return admin;
  }

  private boolean usernameExists(String username, Long excludeAdminId) {
    LambdaQueryWrapper<AdminUser> wrapper =
        new LambdaQueryWrapper<AdminUser>()
            .eq(AdminUser::getUsername, username)
            .isNull(AdminUser::getDeletedAt);
    if (excludeAdminId != null) {
      wrapper.ne(AdminUser::getId, excludeAdminId);
    }
    return adminUserMapper.selectCount(wrapper) > 0;
  }

  private long countEnabledSuperAdmins() {
    LambdaQueryWrapper<AdminUser> wrapper =
        new LambdaQueryWrapper<AdminUser>()
            .eq(AdminUser::getRole, ROLE_SUPER_ADMIN)
            .eq(AdminUser::getStatus, 0)
            .isNull(AdminUser::getDeletedAt);
    return adminUserMapper.selectCount(wrapper);
  }

  private void revokeSessions(Long adminUserId) {
    List<AdminSession> sessions =
        adminSessionMapper.selectList(
            new LambdaQueryWrapper<AdminSession>()
                .eq(AdminSession::getAdminUserId, adminUserId)
                .isNull(AdminSession::getRevokedAt));
    LocalDateTime now = LocalDateTime.now();
    for (AdminSession session : sessions) {
      session.setRevokedAt(now);
      adminSessionMapper.updateById(session);
    }
  }

  private AdminAccountListItemResponse toListItem(
      AdminUser admin, Long currentAdminId, boolean currentIsSuperAdmin) {
    return new AdminAccountListItemResponse(
        admin.getId(),
        admin.getUsername(),
        admin.getName(),
        admin.getRole(),
        admin.getStatus(),
        admin.getLastLoginAt(),
        admin.getCreatedAt(),
        resolveAllowedActions(admin, currentAdminId, currentIsSuperAdmin));
  }

  private AdminAccountDetailResponse toDetailResponse(
      AdminUser admin, List<AdminAccountAuditLogResponse> auditLogs) {
    return new AdminAccountDetailResponse(
        admin.getId(),
        admin.getUsername(),
        admin.getName(),
        admin.getRole(),
        admin.getStatus(),
        admin.getLastLoginAt(),
        admin.getCreatedAt(),
        admin.getUpdatedAt(),
        admin.getVersion(),
        auditLogs);
  }

  private List<AdminAccountAuditLogResponse> fetchRecentAuditLogs(Long targetAdminId) {
    LambdaQueryWrapper<AdminAuditLog> wrapper =
        new LambdaQueryWrapper<AdminAuditLog>()
            .eq(AdminAuditLog::getTargetAdminUserId, targetAdminId)
            .orderByDesc(AdminAuditLog::getCreatedAt)
            .last("LIMIT 5");
    List<AdminAuditLog> logs = adminAuditLogMapper.selectList(wrapper);
    return logs.stream().map(this::toAuditLogResponse).toList();
  }

  private AdminAccountAuditLogResponse toAuditLogResponse(AdminAuditLog log) {
    String operatorName = null;
    if (log.getAdminUserId() != null) {
      AdminUser operator = adminUserMapper.selectById(log.getAdminUserId());
      operatorName = operator != null ? operator.getName() : "系统";
    }
    return new AdminAccountAuditLogResponse(
        log.getId(),
        log.getAdminUserId(),
        operatorName,
        log.getAction(),
        log.getReason(),
        log.getIp(),
        log.getCreatedAt());
  }

  private List<String> resolveAllowedActions(
      AdminUser admin, Long currentAdminId, boolean currentIsSuperAdmin) {
    boolean isSelf = Objects.equals(admin.getId(), currentAdminId);
    if (!currentIsSuperAdmin) {
      return isSelf ? List.of("VIEW", "EDIT") : List.of("VIEW");
    }
    if (isSelf) {
      return List.of("VIEW", "EDIT");
    }
    return List.of("VIEW", "EDIT", "DISABLE", "DELETE", "RESET_PASSWORD");
  }

  private String generateRandomPassword() {
    String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String lower = "abcdefghijklmnopqrstuvwxyz";
    String digits = "0123456789";
    String special = "@$!%*?&_#^";
    String all = upper + lower + digits + special;
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder(RANDOM_PASSWORD_LENGTH);
    sb.append(upper.charAt(random.nextInt(upper.length())));
    sb.append(lower.charAt(random.nextInt(lower.length())));
    sb.append(digits.charAt(random.nextInt(digits.length())));
    sb.append(special.charAt(random.nextInt(special.length())));
    for (int i = 4; i < RANDOM_PASSWORD_LENGTH; i++) {
      sb.append(all.charAt(random.nextInt(all.length())));
    }
    char[] chars = sb.toString().toCharArray();
    for (int i = chars.length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      char tmp = chars[i];
      chars[i] = chars[j];
      chars[j] = tmp;
    }
    return new String(chars);
  }

  @SneakyThrows
  private String toSnapshot(AdminUser admin) {
    return objectMapper.writeValueAsString(toDetailResponse(admin, List.of()));
  }

  private void writeAuditLog(
      Long operatorId,
      Long targetAdminId,
      String action,
      String beforeSnapshot,
      String afterSnapshot,
      String reason,
      String ip) {
    AdminAuditLog log = new AdminAuditLog();
    log.setAdminUserId(operatorId);
    log.setTargetAdminUserId(targetAdminId);
    log.setAction(action);
    log.setBeforeSnapshot(beforeSnapshot);
    log.setAfterSnapshot(afterSnapshot);
    log.setReason(reason);
    log.setIp(ip);
    log.setCreatedAt(LocalDateTime.now());
    adminAuditLogMapper.insert(log);
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
