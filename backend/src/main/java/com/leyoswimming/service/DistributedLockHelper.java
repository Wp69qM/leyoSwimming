package com.leyoswimming.service;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.exception.BusinessException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockHelper {

  private static final String PREFIX = "lock:";
  private static final Duration DEFAULT_TTL = Duration.ofSeconds(30);
  private static final String RELEASE_SCRIPT =
      "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

  private final StringRedisTemplate redisTemplate;

  public LockToken tryLock(String resource, String identifier, Duration ttl) {
    String key = buildKey(resource, identifier);
    String token = UUID.randomUUID().toString();
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
    if (Boolean.TRUE.equals(acquired)) {
      return new LockToken(key, token);
    }
    return null;
  }

  public LockToken lock(String resource, String identifier, Duration ttl) {
    LockToken token = tryLock(resource, identifier, ttl);
    if (token == null) {
      throw new BusinessException(ErrorCode.OPERATION_IN_PROGRESS);
    }
    return token;
  }

  public void unlock(LockToken token) {
    if (token == null) {
      return;
    }
    try {
      DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
      redisTemplate.execute(script, java.util.Collections.singletonList(token.key()), token.token());
    } catch (Exception e) {
      log.warn("Failed to release distributed lock: key={}", token.key(), e);
    }
  }

  public void unlockAfterTransaction(LockToken token) {
    if (token == null) {
      return;
    }
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              unlock(token);
            }
          });
    } else {
      unlock(token);
    }
  }

  public boolean extend(LockToken token, Duration ttl) {
    if (token == null) {
      return false;
    }
    Boolean extended =
        redisTemplate.expire(token.key(), ttl.toMillis(), TimeUnit.MILLISECONDS);
    return Boolean.TRUE.equals(extended);
  }

  private String buildKey(String resource, String identifier) {
    return PREFIX + resource + ":" + identifier;
  }

  public record LockToken(String key, String token) {}
}
