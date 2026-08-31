package com.leyoswimming.config;

import com.leyoswimming.enums.AppType;
import com.leyoswimming.service.sms.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback SmsSender，当 MockSmsSender/ProductionSmsSender 都未激活时兜底。
 * <p>生产环境若未配置真实短信服务商且 mock 开关也未开启，至少保证验证码流程可用，
 * 固定验证码由 SmsCodeService 控制。
 */
@Configuration
@Slf4j
public class FallbackSmsConfig {

  @Bean
  @ConditionalOnMissingBean(SmsSender.class)
  public SmsSender fallbackSmsSender() {
    return new SmsSender() {
      @Override
      public void send(String phone, String code, String scene, AppType appType) {
        log.info(
            "[FALLBACK SMS] phone={}, code={}, scene={}, appType={}",
            mask(phone), code, scene, appType);
      }

      private String mask(String phone) {
        if (phone == null || phone.length() <= 7) {
          return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
      }
    };
  }
}
