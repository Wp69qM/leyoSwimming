package com.leyoswimming.service;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.exception.BusinessException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotencyHelper {

  private static final String PREFIX = "idempotency:";
  private static final Duration TTL = Duration.ofMinutes(5);

  private final StringRedisTemplate redisTemplate;

  public void checkAndLock(String actorType, Long actorId, String key) {
    String redisKey = buildKey(actorType, actorId, key);
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", TTL);
    if (Boolean.FALSE.equals(acquired)) {
      throw new BusinessException(ErrorCode.IDEMPOTENCY_DUPLICATE);
    }
  }

  public void unlock(String actorType, Long actorId, String key) {
    String redisKey = buildKey(actorType, actorId, key);
    redisTemplate.delete(redisKey);
  }

  private String buildKey(String actorType, Long actorId, String key) {
    return PREFIX + actorType + ":" + actorId + ":" + key;
  }
}
