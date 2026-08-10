package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.entity.SmsCode;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.SmsCodeMapper;
import com.leyoswimming.service.sms.SmsSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

  private static final int CODE_LENGTH = 6;
  private static final int EXPIRE_MINUTES = 5;
  private static final int RATE_LIMIT_SECONDS = 60;

  private final SmsCodeMapper smsCodeMapper;
  private final SmsSender smsSender;

  public void send(String phone, String scene, AppType appType) {
    String phoneHash = hashPhone(phone);
    SmsCode latest = findLatest(phoneHash, scene, appType);
    if (latest != null
        && latest.getCreatedAt().plusSeconds(RATE_LIMIT_SECONDS).isAfter(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.SMS_RATE_LIMIT);
    }
    String code = generateCode();
    smsSender.send(phone, code, scene, appType);
    SmsCode smsCode = new SmsCode();
    smsCode.setPhoneHash(phoneHash);
    smsCode.setCode(code);
    smsCode.setScene(scene);
    smsCode.setAppType(appType.name());
    smsCode.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
    smsCode.setUsed(false);
    smsCodeMapper.insert(smsCode);
  }

  public void verify(String phone, String code, String scene, AppType appType) {
    String phoneHash = hashPhone(phone);
    SmsCode record = findLatest(phoneHash, scene, appType);
    if (record == null
        || record.getUsed()
        || record.getExpiresAt().isBefore(LocalDateTime.now())
        || !record.getCode().equals(code)) {
      throw new BusinessException(ErrorCode.INVALID_SMS_CODE);
    }
    record.setUsed(true);
    smsCodeMapper.updateById(record);
  }

  private String generateCode() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(ThreadLocalRandom.current().nextInt(10));
    }
    return sb.toString();
  }

  private String hashPhone(String phone) {
    try {
      return Base64.getEncoder()
          .encodeToString(
              MessageDigest.getInstance("SHA-256").digest(phone.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private SmsCode findLatest(String phoneHash, String scene, AppType appType) {
    return smsCodeMapper
        .selectList(
            new LambdaQueryWrapper<SmsCode>()
                .eq(SmsCode::getPhoneHash, phoneHash)
                .eq(SmsCode::getScene, scene)
                .eq(SmsCode::getAppType, appType.name())
                .orderByDesc(SmsCode::getCreatedAt)
                .last("LIMIT 1"))
        .stream()
        .findFirst()
        .orElse(null);
  }
}
