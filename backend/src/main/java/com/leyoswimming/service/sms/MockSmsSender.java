package com.leyoswimming.service.sms;

import com.leyoswimming.enums.AppType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "leyo.sms", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

  @Override
  public void send(String phone, String code, String scene, AppType appType) {
    log.info("[MOCK SMS] phone={}, code={}, scene={}, appType={}", mask(phone), code, scene, appType);
  }

  private String mask(String phone) {
    return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
  }
}
