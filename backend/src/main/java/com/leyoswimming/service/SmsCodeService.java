package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.entity.SmsCode;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.SmsCodeMapper;
import com.leyoswimming.service.sms.SmsSender;
import com.leyoswimming.util.PhoneEncryptor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class SmsCodeService {

  private static final int CODE_LENGTH = 6;
  private static final int EXPIRE_MINUTES = 5;
  private static final int RATE_LIMIT_SECONDS = 60;
  private static final int MAX_VERIFY_FAILURES = 5;
  private static final Duration FAILURE_LOCK_DURATION = Duration.ofMinutes(30);
  private static final Duration FAILURE_COUNTER_TTL = Duration.ofMinutes(30);
  private static final String SMS_FAIL_COUNT_PREFIX = "sms_fail_count:";
  private static final String SMS_LOCKED_PREFIX = "sms_locked:";
  private static final String RECORD_FAILURE_SCRIPT =
      "local countKey = KEYS[1] "
          + "local lockedKey = KEYS[2] "
          + "local maxFailures = tonumber(ARGV[1]) "
          + "local counterTtl = tonumber(ARGV[2]) "
          + "local lockTtl = tonumber(ARGV[3]) "
          + "local count = redis.call('incr', countKey) "
          + "if count == 1 then "
          + "  redis.call('expire', countKey, counterTtl) "
          + "end "
          + "if count >= maxFailures then "
          + "  redis.call('set', lockedKey, '1', 'EX', lockTtl) "
          + "end "
          + "return count";

  private final SmsCodeMapper smsCodeMapper;
  private final SmsSender smsSender;
  private final StringRedisTemplate redisTemplate;
  private final PhoneEncryptor phoneEncryptor;
  private final String mockFixedCode;

  public SmsCodeService(
      SmsCodeMapper smsCodeMapper,
      SmsSender smsSender,
      StringRedisTemplate redisTemplate,
      PhoneEncryptor phoneEncryptor,
      @Value("${leyo.sms.mock-fixed-code:}") String mockFixedCode) {
    this.smsCodeMapper = smsCodeMapper;
    this.smsSender = smsSender;
    this.redisTemplate = redisTemplate;
    this.phoneEncryptor = phoneEncryptor;
    this.mockFixedCode = mockFixedCode;
  }

  public void send(String phone, String scene, AppType appType) {
    String phoneHash = hashPhone(phone);
    throwIfLocked(phoneHash, scene, appType);
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
    clearFailureCounter(phoneHash, scene, appType);
  }

  public void verify(String phone, String code, String scene, AppType appType) {
    String phoneHash = hashPhone(phone);
    throwIfLocked(phoneHash, scene, appType);
    SmsCode record = findLatest(phoneHash, scene, appType);
    if (record == null) {
      throw new BusinessException(ErrorCode.INVALID_SMS_CODE);
    }
    if (record.getUsed()
        || record.getExpiresAt().isBefore(LocalDateTime.now())
        || !record.getCode().equals(code)) {
      recordVerifyFailure(phoneHash, scene, appType);
      throw new BusinessException(ErrorCode.INVALID_SMS_CODE);
    }
    record.setUsed(true);
    smsCodeMapper.updateById(record);
    clearFailureCounter(phoneHash, scene, appType);
  }

  private void throwIfLocked(String phoneHash, String scene, AppType appType) {
    Boolean locked = redisTemplate.hasKey(buildLockedKey(phoneHash, scene, appType));
    if (Boolean.TRUE.equals(locked)) {
      throw new BusinessException(ErrorCode.SMS_TOO_MANY_ATTEMPTS);
    }
  }

  private void recordVerifyFailure(String phoneHash, String scene, AppType appType) {
    try {
      String countKey = buildFailCountKey(phoneHash, scene, appType);
      String lockedKey = buildLockedKey(phoneHash, scene, appType);
      DefaultRedisScript<Long> script = new DefaultRedisScript<>(RECORD_FAILURE_SCRIPT, Long.class);
      redisTemplate.execute(
          script,
          List.of(countKey, lockedKey),
          String.valueOf(MAX_VERIFY_FAILURES),
          String.valueOf(FAILURE_COUNTER_TTL.getSeconds()),
          String.valueOf(FAILURE_LOCK_DURATION.getSeconds()));
    } catch (Exception e) {
      log.warn("Failed to record SMS verify failure counter", e);
    }
  }

  private void clearFailureCounter(String phoneHash, String scene, AppType appType) {
    try {
      redisTemplate.delete(buildFailCountKey(phoneHash, scene, appType));
      redisTemplate.delete(buildLockedKey(phoneHash, scene, appType));
    } catch (Exception e) {
      log.warn("Failed to clear SMS verify failure counter", e);
    }
  }

  private String buildFailCountKey(String phoneHash, String scene, AppType appType) {
    return SMS_FAIL_COUNT_PREFIX + phoneHash + ":" + scene + ":" + appType.name();
  }

  private String buildLockedKey(String phoneHash, String scene, AppType appType) {
    return SMS_LOCKED_PREFIX + phoneHash + ":" + scene + ":" + appType.name();
  }

  private String generateCode() {
    if (StringUtils.hasText(mockFixedCode)) {
      return mockFixedCode;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(ThreadLocalRandom.current().nextInt(10));
    }
    return sb.toString();
  }

  private String hashPhone(String phone) {
    try {
      return phoneEncryptor.hash(phone);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash phone", e);
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
