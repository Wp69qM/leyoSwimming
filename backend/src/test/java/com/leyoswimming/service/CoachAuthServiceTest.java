package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachPhoneLoginRequest;
import com.leyoswimming.dto.request.CoachWechatLoginRequest;
import com.leyoswimming.dto.response.CoachLoginResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachLoginLog;
import com.leyoswimming.entity.CoachSession;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachLoginLogMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachSessionMapper;
import com.leyoswimming.security.JwtTokenProvider;
import com.leyoswimming.service.wechat.WechatClient;
import com.leyoswimming.service.wechat.WechatSession;
import com.leyoswimming.util.PhoneEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoachAuthServiceTest {

  @Mock private CoachMapper coachMapper;
  @Mock private CoachSessionMapper coachSessionMapper;
  @Mock private CoachLoginLogMapper coachLoginLogMapper;
  @Mock private WechatClient wechatClient;
  @Mock private SmsCodeService smsCodeService;
  private JwtTokenProvider jwtTokenProvider;
  private PhoneEncryptor phoneEncryptor;
  private CoachAuthService coachAuthService;

  @BeforeEach
  void setUp() throws Exception {
    jwtTokenProvider =
        new JwtTokenProvider(
            "bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==",
            86400000L);
    phoneEncryptor = new PhoneEncryptor("local-test-phone-encryption-key-32bytes!");
    coachAuthService =
        new CoachAuthService(
            coachMapper,
            coachSessionMapper,
            coachLoginLogMapper,
            wechatClient,
            smsCodeService,
            jwtTokenProvider,
            phoneEncryptor);
  }

  @Test
  @DisplayName("教练微信登录：已存在教练登录成功")
  void wechatLogin_existingCoach_returnsToken() {
    Coach coach = activeCoach(1L, CoachStatus.NOT_SUBMITTED);
    when(wechatClient.code2session("coach_wx_code"))
        .thenReturn(new WechatSession("coach_openid_1", "coach_union_1", "session_key_1"));
    when(wechatClient.decryptPhone("session_key_1", "encrypted_data", "iv"))
        .thenReturn("13800138000");
    when(coachMapper.findActiveByUnionId("coach_union_1")).thenReturn(coach);

    CoachLoginResponse response =
        coachAuthService.wechatLogin(
            new CoachWechatLoginRequest(
                "coach_wx_code", "encrypted_data", "iv", true, true, "avatar.jpg", "Coach"),
            "127.0.0.1",
            "JUnit");

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.isNewCoach()).isFalse();
    assertThat(response.coachStatus()).isEqualTo(CoachStatus.NOT_SUBMITTED.getValue());
    assertThat(response.coachId()).isEqualTo(1L);
    verify(coachSessionMapper).insert(any(CoachSession.class));
    verify(coachLoginLogMapper).insert(any(CoachLoginLog.class));
  }

  @Test
  @DisplayName("教练微信登录：新教练自动注册并登录成功")
  void wechatLogin_newCoach_createsCoachAndReturnsToken() {
    when(wechatClient.code2session("coach_wx_code_new"))
        .thenReturn(new WechatSession("coach_openid_2", "coach_union_2", "session_key_2"));
    when(wechatClient.decryptPhone("session_key_2", "encrypted_data", "iv"))
        .thenReturn("13800138001");
    when(coachMapper.findActiveByUnionId("coach_union_2")).thenReturn(null);
    when(coachMapper.insert(any(Coach.class))).thenAnswer(invocation -> {
      Coach c = invocation.getArgument(0);
      c.setId(2L);
      return 1;
    });

    CoachLoginResponse response =
        coachAuthService.wechatLogin(
            new CoachWechatLoginRequest(
                "coach_wx_code_new", "encrypted_data", "iv", true, true, "avatar.jpg", "Coach"),
            "127.0.0.1",
            "JUnit");

    assertThat(response.isNewCoach()).isTrue();
    assertThat(response.coachId()).isEqualTo(2L);
    ArgumentCaptor<Coach> coachCaptor = ArgumentCaptor.forClass(Coach.class);
    verify(coachMapper).insert(coachCaptor.capture());
    assertThat(coachCaptor.getValue().getStatus())
        .isEqualTo(CoachStatus.NOT_SUBMITTED.getValue());
  }

  @Test
  @DisplayName("教练微信登录：未同意协议抛出 TERMS_NOT_ACCEPTED")
  void wechatLogin_termsNotAccepted_throwsTermsNotAccepted() {
    assertThatThrownBy(
            () ->
                coachAuthService.wechatLogin(
                    new CoachWechatLoginRequest(
                        "coach_wx_code", "encrypted_data", "iv", true, false, null, null),
                    "127.0.0.1",
                    "JUnit"))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TERMS_NOT_ACCEPTED));

    verify(wechatClient, never()).code2session(any());
  }

  @Test
  @DisplayName("教练微信登录：已离职教练抛出 COACH_NOT_FOUND")
  void wechatLogin_resignedCoach_throwsCoachNotFound() {
    Coach resigned = activeCoach(3L, CoachStatus.RESIGNED);
    when(wechatClient.code2session("coach_wx_code_resigned"))
        .thenReturn(new WechatSession("coach_openid_3", "coach_union_3", "session_key_3"));
    when(wechatClient.decryptPhone("session_key_3", "encrypted_data", "iv"))
        .thenReturn("13800138002");
    when(coachMapper.findActiveByUnionId("coach_union_3")).thenReturn(resigned);

    assertThatThrownBy(
            () ->
                coachAuthService.wechatLogin(
                    new CoachWechatLoginRequest(
                        "coach_wx_code_resigned", "encrypted_data", "iv", true, true, null, null),
                    "127.0.0.1",
                    "JUnit"))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_NOT_FOUND));
  }

  @Test
  @DisplayName("教练手机号登录：已存在教练登录成功")
  void phoneLogin_existingCoach_returnsToken() {
    Coach coach = activeCoach(4L, CoachStatus.APPROVED);
    when(coachMapper.findActiveByPhone(any())).thenReturn(coach);

    CoachLoginResponse response =
        coachAuthService.phoneLogin(
            new CoachPhoneLoginRequest("13800138003", "123456", true, true),
            "127.0.0.1",
            "JUnit");

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.isNewCoach()).isFalse();
    assertThat(response.coachStatus()).isEqualTo(CoachStatus.APPROVED.getValue());
    assertThat(response.coachId()).isEqualTo(4L);
    verify(smsCodeService)
        .verify("13800138003", "123456", "login", com.leyoswimming.enums.AppType.coach);
    verify(coachSessionMapper).insert(any(CoachSession.class));
  }

  @Test
  @DisplayName("教练手机号登录：新教练自动注册并登录成功")
  void phoneLogin_newCoach_createsCoachAndReturnsToken() {
    when(coachMapper.findActiveByPhone(any())).thenReturn(null);
    when(coachMapper.insert(any(Coach.class))).thenAnswer(invocation -> {
      Coach c = invocation.getArgument(0);
      c.setId(5L);
      return 1;
    });

    CoachLoginResponse response =
        coachAuthService.phoneLogin(
            new CoachPhoneLoginRequest("13800138004", "123456", true, true),
            "127.0.0.1",
            "JUnit");

    assertThat(response.isNewCoach()).isTrue();
    assertThat(response.coachId()).isEqualTo(5L);
    verify(coachMapper).insert(any(Coach.class));
  }

  private Coach activeCoach(Long id, CoachStatus status) {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setOpenid("coach_openid_" + id);
    coach.setPhone("encrypted_phone_" + id);
    coach.setStatus(status.getValue());
    return coach;
  }
}
