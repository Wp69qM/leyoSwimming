package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.entity.SmsCode;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.SmsCodeMapper;
import com.leyoswimming.service.sms.SmsSender;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmsCodeServiceTest {

  @Mock private SmsCodeMapper smsCodeMapper;
  @Mock private SmsSender smsSender;
  private SmsCodeService smsCodeService;

  @BeforeEach
  void setUp() {
    smsCodeService = new SmsCodeService(smsCodeMapper, smsSender);
  }

  @Test
  @DisplayName("首次发送验证码成功")
  void send_noRecentCode_insertsNewCode() {
    when(smsCodeMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(Collections.emptyList());

    smsCodeService.send("13800138000", "login", AppType.user);

    ArgumentCaptor<SmsCode> captor = ArgumentCaptor.forClass(SmsCode.class);
    verify(smsCodeMapper).insert(captor.capture());
    SmsCode saved = captor.getValue();
    assertThat(saved.getPhoneHash()).isNotBlank();
    assertThat(saved.getCode()).hasSize(6);
    assertThat(saved.getScene()).isEqualTo("login");
    assertThat(saved.getAppType()).isEqualTo("user");
    assertThat(saved.getUsed()).isFalse();
    assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    verify(smsSender).send("13800138000", saved.getCode(), "login", AppType.user);
  }

  @Test
  @DisplayName("60 秒内重复发送触发限流")
  void send_recentCodeExists_throwsRateLimit() {
    SmsCode recent = new SmsCode();
    recent.setCreatedAt(LocalDateTime.now().minusSeconds(30));
    when(smsCodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(recent));

    assertThatThrownBy(() -> smsCodeService.send("13800138000", "login", AppType.user))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SMS_RATE_LIMIT));

    verify(smsCodeMapper, never()).insert(any(SmsCode.class));
    verify(smsSender, never()).send(any(), any(), any(), any());
  }

  @Test
  @DisplayName("验证码校验成功")
  void verify_validCode_marksUsed() {
    SmsCode record = validSmsCode("123456");
    when(smsCodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

    smsCodeService.verify("13800138000", "123456", "login", AppType.user);

    assertThat(record.getUsed()).isTrue();
    verify(smsCodeMapper).updateById(record);
  }

  @Test
  @DisplayName("验证码不匹配抛出 INVALID_SMS_CODE")
  void verify_wrongCode_throwsInvalidSmsCode() {
    SmsCode record = validSmsCode("123456");
    when(smsCodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

    assertThatThrownBy(
            () -> smsCodeService.verify("13800138000", "000000", "login", AppType.user))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_SMS_CODE));

    verify(smsCodeMapper, never()).updateById(any(SmsCode.class));
  }

  @Test
  @DisplayName("验证码已过期抛出 INVALID_SMS_CODE")
  void verify_expiredCode_throwsInvalidSmsCode() {
    SmsCode record = validSmsCode("123456");
    record.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    when(smsCodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

    assertThatThrownBy(
            () -> smsCodeService.verify("13800138000", "123456", "login", AppType.user))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_SMS_CODE));
  }

  @Test
  @DisplayName("验证码已使用抛出 INVALID_SMS_CODE")
  void verify_usedCode_throwsInvalidSmsCode() {
    SmsCode record = validSmsCode("123456");
    record.setUsed(true);
    when(smsCodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

    assertThatThrownBy(
            () -> smsCodeService.verify("13800138000", "123456", "login", AppType.user))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_SMS_CODE));
  }

  @Test
  @DisplayName("不存在验证码记录抛出 INVALID_SMS_CODE")
  void verify_noRecord_throwsInvalidSmsCode() {
    when(smsCodeMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(Collections.emptyList());

    assertThatThrownBy(
            () -> smsCodeService.verify("13800138000", "123456", "login", AppType.user))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_SMS_CODE));
  }

  private SmsCode validSmsCode(String code) {
    SmsCode smsCode = new SmsCode();
    smsCode.setId(1L);
    smsCode.setPhoneHash("phone_hash");
    smsCode.setCode(code);
    smsCode.setScene("login");
    smsCode.setAppType("user");
    smsCode.setUsed(false);
    smsCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
    smsCode.setCreatedAt(LocalDateTime.now());
    return smsCode;
  }
}
