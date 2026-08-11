package com.leyoswimming.config;

import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.User;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class BackfillPhoneHashInitializer {

  private final UserMapper userMapper;
  private final CoachMapper coachMapper;
  private final PhoneEncryptor phoneEncryptor;

  @EventListener(ApplicationReadyEvent.class)
  public void backfill() {
    backfillUsers();
    backfillCoaches();
  }

  private void backfillUsers() {
    List<User> users = userMapper.selectList(null);
    int updated = 0;
    for (User user : users) {
      if (hasText(user.getPhone()) && !hasText(user.getPhoneHash())) {
        try {
          String plain = phoneEncryptor.decrypt(user.getPhone());
          user.setPhoneHash(phoneEncryptor.hash(plain));
          userMapper.updateById(user);
          updated++;
        } catch (Exception e) {
          log.warn("Failed to backfill phone hash for user id={}", user.getId(), e);
        }
      }
      if (hasText(user.getGuardianPhone()) && !hasText(user.getGuardianPhoneHash())) {
        try {
          String plain = phoneEncryptor.decrypt(user.getGuardianPhone());
          user.setGuardianPhoneHash(phoneEncryptor.hash(plain));
          userMapper.updateById(user);
          updated++;
        } catch (Exception e) {
          log.warn("Failed to backfill guardian phone hash for user id={}", user.getId(), e);
        }
      }
    }
    if (updated > 0) {
      log.info("Backfilled phone hashes for {} user records", updated);
    }
  }

  private void backfillCoaches() {
    List<Coach> coaches = coachMapper.selectList(null);
    int updated = 0;
    for (Coach coach : coaches) {
      if (hasText(coach.getPhone()) && !hasText(coach.getPhoneHash())) {
        try {
          String plain = phoneEncryptor.decrypt(coach.getPhone());
          coach.setPhoneHash(phoneEncryptor.hash(plain));
          coachMapper.updateById(coach);
          updated++;
        } catch (Exception e) {
          log.warn("Failed to backfill phone hash for coach id={}", coach.getId(), e);
        }
      }
    }
    if (updated > 0) {
      log.info("Backfilled phone hashes for {} coach records", updated);
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
