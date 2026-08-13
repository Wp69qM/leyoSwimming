package com.leyoswimming.service;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.entity.AdminUser;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AdminUserMapper;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminPermissionHelper {

  public static final String ROLE_SUPER_ADMIN = "super_admin";
  public static final String ROLE_ADMIN = "admin";
  public static final String ROLE_COACH_MANAGER = "COACH_MANAGER";

  public static final String PERM_USER_READ = "USER:READ";
  public static final String PERM_USER_WRITE = "USER:WRITE";
  public static final String PERM_USER_BAN = "USER:BAN";
  public static final String PERM_COACH_READ = "COACH:READ";
  public static final String PERM_COACH_WRITE = "COACH:WRITE";
  public static final String PERM_COACH_CANCEL_ENTRY = "COACH:CANCEL_ENTRY";
  public static final String PERM_ADMIN_WRITE = "ADMIN:WRITE";
  public static final String PERM_ORDER_READ = "ORDER:READ";
  public static final String PERM_ORDER_WRITE = "ORDER:WRITE";
  public static final String PERM_PACKAGE_READ = "PACKAGE:READ";
  public static final String PERM_PACKAGE_WRITE = "PACKAGE:WRITE";

  private static final Set<String> SUPER_ADMIN_PERMS =
      Set.of(
          PERM_USER_READ,
          PERM_USER_WRITE,
          PERM_USER_BAN,
          PERM_COACH_READ,
          PERM_COACH_WRITE,
          PERM_COACH_CANCEL_ENTRY,
          PERM_ADMIN_WRITE,
          PERM_ORDER_READ,
          PERM_ORDER_WRITE,
          PERM_PACKAGE_READ,
          PERM_PACKAGE_WRITE);

  private static final Set<String> ADMIN_PERMS =
      Set.of(
          PERM_USER_READ,
          PERM_USER_WRITE,
          PERM_USER_BAN,
          PERM_COACH_READ,
          PERM_COACH_WRITE,
          PERM_COACH_CANCEL_ENTRY,
          PERM_ORDER_READ,
          PERM_ORDER_WRITE,
          PERM_PACKAGE_READ,
          PERM_PACKAGE_WRITE);

  private static final Set<String> COACH_MANAGER_PERMS =
      Set.of(PERM_COACH_READ, PERM_COACH_WRITE);

  private final AdminUserMapper adminUserMapper;

  public void checkPermission(Long adminId, String permission) {
    if (!hasPermission(adminId, permission)) {
      throw new BusinessException(ErrorCode.ADMIN_PERMISSION_DENIED);
    }
  }

  public boolean hasPermission(Long adminId, String permission) {
    AdminUser admin = adminUserMapper.selectById(adminId);
    if (admin == null || admin.getStatus() != 0 || admin.getDeletedAt() != null) {
      return false;
    }
    return resolvePermissions(admin.getRole()).contains(permission);
  }

  public boolean isSuperAdmin(Long adminId) {
    AdminUser admin = adminUserMapper.selectById(adminId);
    return admin != null
        && admin.getStatus() == 0
        && admin.getDeletedAt() == null
        && ROLE_SUPER_ADMIN.equalsIgnoreCase(admin.getRole());
  }

  public AdminUser requireActiveAdmin(Long adminId) {
    AdminUser admin = adminUserMapper.selectById(adminId);
    if (admin == null || admin.getStatus() != 0 || admin.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.ADMIN_PERMISSION_DENIED);
    }
    return admin;
  }

  public void checkSuperAdmin(Long adminId) {
    if (!isSuperAdmin(adminId)) {
      throw new BusinessException(ErrorCode.ADMIN_PERMISSION_DENIED);
    }
  }

  private Set<String> resolvePermissions(String role) {
    return switch (Objects.requireNonNullElse(role, "").toLowerCase()) {
      case ROLE_SUPER_ADMIN -> SUPER_ADMIN_PERMS;
      case ROLE_ADMIN -> ADMIN_PERMS;
      case ROLE_COACH_MANAGER -> COACH_MANAGER_PERMS;
      default -> Set.of();
    };
  }
}
