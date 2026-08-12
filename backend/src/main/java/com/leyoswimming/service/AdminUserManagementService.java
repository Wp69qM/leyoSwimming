package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminUserAddRequest;
import com.leyoswimming.dto.request.AdminUserListRequest;
import com.leyoswimming.dto.request.AdminUserUpdateRequest;
import com.leyoswimming.dto.response.AdminUserDetailResponse;
import com.leyoswimming.dto.response.AdminUserListItemResponse;
import com.leyoswimming.dto.response.AdminUserListResponse;
import com.leyoswimming.entity.AuditLog;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.SwimStroke;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AuditLogMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.PhoneEncryptor;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

  private static final int MIN_AGE = 3;
  private static final int MAX_AGE = 99;
  private static final String SOURCE_ADMIN_CREATED = "ADMIN_CREATED";
  private static final String ACTION_CREATE = "ADMIN_CREATE_USER";
  private static final String ACTION_UPDATE = "ADMIN_UPDATE_PROFILE";
  private static final String ACTION_BAN = "ADMIN_BAN_USER";
  private static final String ACTION_UNBAN = "ADMIN_UNBAN_USER";
  private static final String TARGET_TYPE = "user";
  private static final String ACTOR_TYPE = "admin";

  private final UserMapper userMapper;
  private final AuditLogMapper auditLogMapper;
  private final PhoneEncryptor phoneEncryptor;
  private final AdminPermissionHelper permissionHelper;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public AdminUserListResponse list(Long adminId, AdminUserListRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_USER_READ);

    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    if (request.identity() != null) {
      wrapper.eq(User::getIdentity, request.identity());
    }
    if (request.status() != null) {
      wrapper.eq(User::getStatus, request.status());
    }
    if (request.profileCompleted() != null) {
      wrapper.eq(User::getProfileCompleted, request.profileCompleted());
    }
    if (StringUtils.isNotBlank(request.startDate())) {
      try {
        LocalDateTime start = LocalDate.parse(request.startDate()).atStartOfDay();
        wrapper.ge(User::getCreatedAt, start);
      } catch (DateTimeParseException e) {
        log.warn("Invalid startDate: {}", request.startDate());
      }
    }
    if (StringUtils.isNotBlank(request.endDate())) {
      try {
        LocalDateTime end = LocalDate.parse(request.endDate()).atTime(23, 59, 59);
        wrapper.le(User::getCreatedAt, end);
      } catch (DateTimeParseException e) {
        log.warn("Invalid endDate: {}", request.endDate());
      }
    }
    if (StringUtils.isNotBlank(request.keyword())) {
      String keyword = request.keyword().trim();
      if (keyword.matches("\\d{11}")) {
        try {
          wrapper.eq(User::getPhoneHash, phoneEncryptor.hash(keyword));
        } catch (Exception e) {
          log.error("Failed to hash phone keyword", e);
          wrapper.eq(User::getPhoneHash, "");
        }
      } else {
        wrapper.like(User::getName, keyword);
      }
    }
    wrapper.orderByDesc(User::getCreatedAt);

    Page<User> page = new Page<>(request.page(), request.pageSize());
    Page<User> result = userMapper.selectPage(page, wrapper);

    List<AdminUserListItemResponse> list = result.getRecords().stream().map(this::toListItem).toList();
    return new AdminUserListResponse(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
  }

  @Transactional(readOnly = true)
  public AdminUserDetailResponse detail(Long adminId, Long userId) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_USER_READ);
    User user = findUser(userId);
    return toDetailResponse(user);
  }

  @Transactional
  public AdminUserDetailResponse add(Long adminId, AdminUserAddRequest request, HttpServletRequest httpRequest) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_USER_WRITE);

    validateAge(request.age());
    validateGuardian(request.age(), request.guardianName(), request.guardianPhone());
    validateSwimBasis(request.hasSwimBasis(), request.swimStrokes());

    String phoneHash = hashPhone(request.phone());
    if (phoneExists(phoneHash)) {
      throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
    }

    User user = new User();
    user.setOpenid(null);
    user.setUnionId(null);
    user.setPhone(encryptPhone(request.phone()));
    user.setPhoneHash(phoneHash);
    user.setAvatarUrl(StringUtils.trimToNull(request.avatarUrl()));
    user.setName(request.name().trim());
    user.setIdentityStatus("注册用户");
    user.setIdentity(1);
    user.setSource(SOURCE_ADMIN_CREATED);
    user.setProfileCompleted(true);
    user.setStatus(0);
    user.setAge(request.age());
    user.setGender(toGenderCode(request.gender()));
    applySwimInfo(user, request.hasSwimBasis(), request.swimStrokes(), request.swimYears());
    user.setPersonalDesc(StringUtils.trimToNull(request.personalDesc()));
    applyGuardian(user, request.age(), request.guardianName(), request.guardianPhone());

    userMapper.insert(user);

    writeAuditLog(
        adminId,
        user.getId(),
        ACTION_CREATE,
        null,
        toAuditSnapshot(user),
        null,
        getClientIp(httpRequest));

    return toDetailResponse(user);
  }

  @Transactional
  public AdminUserDetailResponse update(Long adminId, AdminUserUpdateRequest request, HttpServletRequest httpRequest) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_USER_WRITE);

    User user = findUser(request.userId());
    if (!Objects.equals(user.getVersion(), request.version())) {
      throw new BusinessException(ErrorCode.USER_CONCURRENTLY_UPDATED);
    }

    validateAge(request.age());
    validateGuardian(request.age(), request.guardianName(), request.guardianPhone());
    validateSwimBasis(request.hasSwimBasis(), request.swimStrokes());

    String beforeSnapshot = toAuditSnapshot(user);

    user.setAvatarUrl(StringUtils.trimToNull(request.avatarUrl()));
    user.setName(request.name().trim());
    user.setAge(request.age());
    user.setGender(toGenderCode(request.gender()));
    applySwimInfo(user, request.hasSwimBasis(), request.swimStrokes(), request.swimYears());
    user.setPersonalDesc(StringUtils.trimToNull(request.personalDesc()));
    applyGuardian(user, request.age(), request.guardianName(), request.guardianPhone());

    int affected = userMapper.updateById(user);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.USER_CONCURRENTLY_UPDATED);
    }

    writeAuditLog(
        adminId,
        user.getId(),
        ACTION_UPDATE,
        beforeSnapshot,
        toAuditSnapshot(user),
        null,
        getClientIp(httpRequest));

    return toDetailResponse(user);
  }

  @Transactional
  public AdminUserDetailResponse ban(Long adminId, Long userId, String reason, HttpServletRequest httpRequest) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_USER_BAN);
    return toggleStatus(adminId, userId, 2, reason, httpRequest, ACTION_BAN);
  }

  @Transactional
  public AdminUserDetailResponse unban(Long adminId, Long userId, String reason, HttpServletRequest httpRequest) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_USER_BAN);
    return toggleStatus(adminId, userId, 0, reason, httpRequest, ACTION_UNBAN);
  }

  private AdminUserDetailResponse toggleStatus(
      Long adminId,
      Long userId,
      int targetStatus,
      String reason,
      HttpServletRequest httpRequest,
      String action) {
    User user = findUser(userId);
    String beforeSnapshot = toAuditSnapshot(user);
    user.setStatus(targetStatus);
    int affected = userMapper.updateById(user);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.USER_CONCURRENTLY_UPDATED);
    }

    writeAuditLog(
        adminId,
        user.getId(),
        action,
        beforeSnapshot,
        toAuditSnapshot(user),
        reason,
        getClientIp(httpRequest));

    return toDetailResponse(user);
  }

  private User findUser(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    return user;
  }

  private boolean phoneExists(String phoneHash) {
    LambdaQueryWrapper<User> wrapper =
        new LambdaQueryWrapper<User>().eq(User::getPhoneHash, phoneHash).eq(User::getStatus, 0);
    return userMapper.selectCount(wrapper) > 0;
  }

  private void validateAge(Integer age) {
    if (age == null || age < MIN_AGE || age > MAX_AGE) {
      throw new BusinessException(ErrorCode.INVALID_AGE);
    }
  }

  private void validateGuardian(Integer age, String guardianName, String guardianPhone) {
    if (age < 18) {
      if (StringUtils.isBlank(guardianName) || StringUtils.isBlank(guardianPhone)) {
        throw new BusinessException(ErrorCode.INVALID_GUARDIAN_INFO);
      }
      if (!guardianPhone.matches("^1[3-9]\\d{9}$")) {
        throw new BusinessException(ErrorCode.INVALID_GUARDIAN_INFO);
      }
    }
  }

  private void validateSwimBasis(Boolean hasSwimBasis, List<String> swimStrokes) {
    if (Boolean.TRUE.equals(hasSwimBasis) && (swimStrokes == null || swimStrokes.isEmpty())) {
      throw new BusinessException(ErrorCode.INVALID_SWIM_STROKE);
    }
  }

  private void applySwimInfo(User user, Boolean hasSwimBasis, List<String> swimStrokes, Integer swimYears) {
    user.setHasSwimBasis(hasSwimBasis);
    if (Boolean.TRUE.equals(hasSwimBasis)) {
      List<String> codes = swimStrokes.stream().map(SwimStroke::toCode).distinct().toList();
      user.setSwimStrokes(codes);
      user.setSwimYears(swimYears);
    } else {
      user.setSwimStrokes(null);
      user.setSwimYears(null);
    }
  }

  private void applyGuardian(User user, Integer age, String guardianName, String guardianPhone) {
    if (age < 18) {
      user.setGuardianName(guardianName.trim());
      String phone = guardianPhone.trim();
      user.setGuardianPhone(encryptPhone(phone));
      user.setGuardianPhoneHash(hashPhone(phone));
    } else {
      user.setGuardianName(null);
      user.setGuardianPhone(null);
      user.setGuardianPhoneHash(null);
    }
  }

  private String toGenderCode(Integer gender) {
    return gender == null || gender == 2 ? "female" : "male";
  }

  private Integer toGenderValue(String gender) {
    return "female".equalsIgnoreCase(gender) ? 2 : 1;
  }

  private List<String> toStrokeLabels(List<String> codes) {
    if (codes == null) {
      return List.of();
    }
    return codes.stream().map(SwimStroke::toLabel).toList();
  }

  private AdminUserListItemResponse toListItem(User user) {
    return new AdminUserListItemResponse(
        user.getId(),
        user.getAvatarUrl(),
        user.getName(),
        maskPhone(decryptPhone(user.getPhone())),
        toGenderValue(user.getGender()),
        user.getAge(),
        user.getIdentity(),
        Boolean.TRUE.equals(user.getProfileCompleted()),
        user.getStatus(),
        user.getCreatedAt());
  }

  private AdminUserDetailResponse toDetailResponse(User user) {
    return new AdminUserDetailResponse(
        user.getId(),
        user.getAvatarUrl(),
        user.getName(),
        decryptPhone(user.getPhone()),
        toGenderValue(user.getGender()),
        user.getAge(),
        user.getIdentity(),
        user.getStatus(),
        Boolean.TRUE.equals(user.getProfileCompleted()),
        user.getSource(),
        user.getHasSwimBasis(),
        toStrokeLabels(user.getSwimStrokes()),
        user.getSwimYears(),
        user.getPersonalDesc(),
        user.getGuardianName(),
        decryptPhone(user.getGuardianPhone()),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        user.getVersion());
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

  private String maskPhone(String phone) {
    return PhoneEncryptor.mask(phone);
  }

  @SneakyThrows
  private String toSnapshot(User user) {
    return objectMapper.writeValueAsString(toDetailResponse(user));
  }

  @SneakyThrows
  private String toAuditSnapshot(User user) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("id", user.getId());
    snapshot.put("name", user.getName());
    snapshot.put("phone", maskPhone(decryptPhone(user.getPhone())));
    snapshot.put("phoneHash", user.getPhoneHash());
    snapshot.put("avatarUrl", user.getAvatarUrl());
    snapshot.put("identity", user.getIdentity());
    snapshot.put("status", user.getStatus());
    snapshot.put("age", user.getAge());
    snapshot.put("gender", user.getGender());
    snapshot.put("hasSwimBasis", user.getHasSwimBasis());
    snapshot.put("swimStrokes", user.getSwimStrokes());
    snapshot.put("swimYears", user.getSwimYears());
    snapshot.put("personalDesc", user.getPersonalDesc());
    snapshot.put("guardianName", user.getGuardianName());
    snapshot.put("guardianPhone", maskPhone(decryptPhone(user.getGuardianPhone())));
    snapshot.put("guardianPhoneHash", user.getGuardianPhoneHash());
    snapshot.put("version", user.getVersion());
    return objectMapper.writeValueAsString(snapshot);
  }

  private void writeAuditLog(
      Long adminId,
      Long userId,
      String action,
      String beforeSnapshot,
      String afterSnapshot,
      String reason,
      String ip) {
    AuditLog auditLog = new AuditLog();
    auditLog.setActorType(ACTOR_TYPE);
    auditLog.setActorId(adminId);
    auditLog.setTargetType(TARGET_TYPE);
    auditLog.setTargetId(userId);
    auditLog.setAction(action);
    auditLog.setBeforeSnapshot(beforeSnapshot);
    auditLog.setAfterSnapshot(afterSnapshot);
    auditLog.setReason(reason);
    auditLog.setIp(ip);
    auditLog.setCreatedAt(LocalDateTime.now());
    auditLogMapper.insert(auditLog);
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
