package com.leyoswimming.service.sms;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "leyo.sms", name = "mock-enabled", havingValue = "false")
public class ProductionSmsSender implements SmsSender {

  @Override
  public void send(String phone, String code, String scene, AppType appType) {
    log.info("[PROD SMS] phone={}, code={}, scene={}, appType={}", mask(phone), code, scene, appType);
    throw new BusinessException(ErrorCode.SMS_SEND_FAILED, "请在生产环境配置真实短信服务商");
  }

  private String mask(String phone) {
    return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
  }
}
