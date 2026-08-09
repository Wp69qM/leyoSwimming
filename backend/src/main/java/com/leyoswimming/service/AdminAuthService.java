package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminLoginRequest;
import com.leyoswimming.dto.response.AdminInfo;
import com.leyoswimming.dto.response.AdminLoginResponse;
import com.leyoswimming.entity.AdminLoginLog;
import com.leyoswimming.entity.AdminSession;
import com.leyoswimming.entity.AdminUser;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AdminLoginLogMapper;
import com.leyoswimming.repository.AdminSessionMapper;
import com.leyoswimming.repository.AdminUserMapper;
import com.leyoswimming.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

  private static final int LOGIN_LOG_SUCCESS = 0;
  private static final int LOGIN_LOG_FAIL = 1;
  private static final int ADMIN_STATUS_ENABLED = 0;
  private static final int ADMIN_STATUS_DISABLED = 1;

  private final AdminUserMapper adminUserMapper;
  private final AdminSessionMapper adminSessionMapper;
  private final AdminLoginLogMapper adminLoginLogMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public AdminLoginResponse login(AdminLoginRequest request, HttpServletRequest httpRequest) {
    AdminUser admin =
        adminUserMapper.selectOne(
            new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, request.username()));

    if (admin == null || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
      saveLoginLog(null, request.username(), httpRequest, LOGIN_LOG_FAIL, "用户名或密码错误");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }

    if (admin.getStatus() == ADMIN_STATUS_DISABLED) {
      saveLoginLog(admin.getId(), admin.getUsername(), httpRequest, LOGIN_LOG_FAIL, "账号已被禁用");
      throw new BusinessException(ErrorCode.ADMIN_DISABLED);
    }

    String token = jwtTokenProvider.generateAdminToken(admin.getId(), admin.getUsername());
    saveSession(admin.getId(), token);
    updateLastLoginAt(admin);
    saveLoginLog(admin.getId(), admin.getUsername(), httpRequest, LOGIN_LOG_SUCCESS, null);

    AdminInfo adminInfo = new AdminInfo(admin.getId(), admin.getUsername(), admin.getName(), admin.getRole());
    return new AdminLoginResponse(token, jwtTokenProvider.getExpirationSeconds(), adminInfo);
  }

  @Transactional
  public void logout(String token) {
    if (token == null || token.isBlank()) {
      return;
    }
    String tokenHash = sha256(token);
    AdminSession session =
        adminSessionMapper.selectOne(
            new LambdaQueryWrapper<AdminSession>().eq(AdminSession::getTokenHash, tokenHash));
    if (session != null && session.getRevokedAt() == null) {
      session.setRevokedAt(LocalDateTime.now());
      adminSessionMapper.updateById(session);
    }
  }

  public boolean isTokenActive(String token) {
    if (!jwtTokenProvider.isTokenValid(token)) {
      return false;
    }
    String tokenHash = sha256(token);
    AdminSession session =
        adminSessionMapper.selectOne(
            new LambdaQueryWrapper<AdminSession>().eq(AdminSession::getTokenHash, tokenHash));
    if (session == null || session.getRevokedAt() != null) {
      return false;
    }
    return session.getExpiresAt().isAfter(LocalDateTime.now());
  }

  private void saveSession(Long adminUserId, String token) {
    AdminSession session = new AdminSession();
    session.setAdminUserId(adminUserId);
    session.setTokenHash(sha256(token));
    session.setExpiresAt(
        LocalDateTime.ofInstant(
            Instant.now().plusSeconds(jwtTokenProvider.getExpirationSeconds()), ZoneId.systemDefault()));
    adminSessionMapper.insert(session);
  }

  private void updateLastLoginAt(AdminUser admin) {
    admin.setLastLoginAt(LocalDateTime.now());
    adminUserMapper.updateById(admin);
  }

  private void saveLoginLog(
      Long adminUserId,
      String username,
      HttpServletRequest request,
      int status,
      String reason) {
    AdminLoginLog logRecord = new AdminLoginLog();
    logRecord.setAdminUserId(adminUserId);
    logRecord.setUsername(username);
    logRecord.setIp(getClientIp(request));
    logRecord.setUserAgent(request.getHeader("User-Agent"));
    logRecord.setStatus(status);
    logRecord.setReason(reason);
    adminLoginLogMapper.insert(logRecord);
  }

  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isBlank()) {
      ip = request.getRemoteAddr();
    } else {
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
