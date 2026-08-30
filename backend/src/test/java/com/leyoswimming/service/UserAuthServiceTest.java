package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UserPhoneLoginRequest;
import com.leyoswimming.dto.request.UserWechatLoginRequest;
import com.leyoswimming.dto.response.UserLoginResponse;
import com.leyoswimming.entity.User;
import com.leyoswimming.entity.UserLoginLog;
import com.leyoswimming.entity.UserSession;
import com.leyoswimming.enums.UserStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.UserLoginLogMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.repository.UserSessionMapper;
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
class UserAuthServiceTest {

  @Mock private UserMapper userMapper;
  @Mock private UserSessionMapper userSessionMapper;
  @Mock private UserLoginLogMapper userLoginLogMapper;
  @Mock private WechatClient wechatClient;
  @Mock private SmsCodeService smsCodeService;
  @Mock private PolicyService policyService;
  private JwtTokenProvider jwtTokenProvider;
  private PhoneEncryptor phoneEncryptor;
  private UserAuthService userAuthService;

  @BeforeEach
  void setUp() throws Exception {
    jwtTokenProvider =
        new JwtTokenProvider(
            "bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==",
            86400000L);
    phoneEncryptor = new PhoneEncryptor("local-test-phone-encryption-key-32bytes!");
    userAuthService =
        new UserAuthService(
            userMapper,
            userSessionMapper,
            userLoginLogMapper,
            wechatClient,
            smsCodeService,
            jwtTokenProvider,
            phoneEncryptor,
            policyService);
  }

  @Test
  @DisplayName("微信登录：已存在用户登录成功")
  void wechatLogin_existingUser_returnsToken() {
    User user = activeUser(1L);
    when(wechatClient.code2session("wx_code"))
        .thenReturn(new WechatSession("openid_1", "union_1", "session_key_1"));
    when(wechatClient.decryptPhone("session_key_1", "encrypted_data", "iv"))
        .thenReturn("13800138000");
    when(userMapper.findActiveByUnionId("union_1")).thenReturn(user);

    UserLoginResponse response =
        userAuthService.wechatLogin(
            new UserWechatLoginRequest(
                "wx_code", "encrypted_data", "iv", true, true, "v1.0", "v1.0", "avatar.jpg", "Nick"),
            "127.0.0.1",
            "JUnit");

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.isNewUser()).isFalse();
    assertThat(response.profileCompleted()).isFalse();
    assertThat(response.userId()).isEqualTo(1L);
    verify(userSessionMapper).insert(any(UserSession.class));
    verify(userLoginLogMapper).insert(any(UserLoginLog.class));
  }

  @Test
  @DisplayName("微信登录：新用户自动注册并登录成功")
  void wechatLogin_newUser_createsUserAndReturnsToken() {
    when(wechatClient.code2session("wx_code_new"))
        .thenReturn(new WechatSession("openid_2", "union_2", "session_key_2"));
    when(wechatClient.decryptPhone("session_key_2", "encrypted_data", "iv"))
        .thenReturn("13800138001");
    when(userMapper.findActiveByUnionId("union_2")).thenReturn(null);
    when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
      User u = invocation.getArgument(0);
      u.setId(2L);
      return 1;
    });

    UserLoginResponse response =
        userAuthService.wechatLogin(
            new UserWechatLoginRequest(
                "wx_code_new", "encrypted_data", "iv", true, true, "v1.0", "v1.0", "avatar.jpg", "Nick"),
            "127.0.0.1",
            "JUnit");

    assertThat(response.isNewUser()).isTrue();
    assertThat(response.userId()).isEqualTo(2L);
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userMapper).insert(userCaptor.capture());
    assertThat(userCaptor.getValue().getUnionId()).isEqualTo("union_2");
  }

  @Test
  @DisplayName("微信登录：未同意协议抛出 TERMS_NOT_ACCEPTED")
  void wechatLogin_termsNotAccepted_throwsTermsNotAccepted() {
    assertThatThrownBy(
            () ->
                userAuthService.wechatLogin(
                    new UserWechatLoginRequest(
                        "wx_code", "encrypted_data", "iv", false, true, "v1.0", "v1.0", null, null),
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
  @DisplayName("微信登录：已封禁用户抛出 USER_DISABLED")
  void wechatLogin_bannedUser_throwsUserDisabled() {
    User banned = activeUser(3L);
    banned.setStatus(UserStatus.BANNED.getValue());
    when(wechatClient.code2session("wx_code_banned"))
        .thenReturn(new WechatSession("openid_3", "union_3", "session_key_3"));
    when(wechatClient.decryptPhone("session_key_3", "encrypted_data", "iv"))
        .thenReturn("13800138002");
    when(userMapper.findActiveByUnionId("union_3")).thenReturn(banned);

    assertThatThrownBy(
            () ->
                userAuthService.wechatLogin(
                    new UserWechatLoginRequest(
                        "wx_code_banned", "encrypted_data", "iv", true, true, "v1.0", "v1.0", null, null),
                    "127.0.0.1",
                    "JUnit"))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_DISABLED));
  }

  @Test
  @DisplayName("手机号登录：已存在用户登录成功")
  void phoneLogin_existingUser_returnsToken() {
    User user = activeUser(4L);
    when(userMapper.findActiveByPhone(any())).thenReturn(user);

    UserLoginResponse response =
        userAuthService.phoneLogin(
            new UserPhoneLoginRequest("13800138003", "123456", true, true, "v1.0", "v1.0"),
            "127.0.0.1",
            "JUnit");

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.isNewUser()).isFalse();
    assertThat(response.userId()).isEqualTo(4L);
    verify(smsCodeService).verify("13800138003", "123456", "login", com.leyoswimming.enums.AppType.user);
    verify(userSessionMapper).insert(any(UserSession.class));
  }

  @Test
  @DisplayName("手机号登录：新用户自动注册并登录成功")
  void phoneLogin_newUser_createsUserAndReturnsToken() {
    when(userMapper.findActiveByPhone(any())).thenReturn(null);
    when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
      User u = invocation.getArgument(0);
      u.setId(5L);
      return 1;
    });

    UserLoginResponse response =
        userAuthService.phoneLogin(
            new UserPhoneLoginRequest("13800138004", "123456", true, true, "v1.0", "v1.0"),
            "127.0.0.1",
            "JUnit");

    assertThat(response.isNewUser()).isTrue();
    assertThat(response.userId()).isEqualTo(5L);
    verify(userMapper).insert(any(User.class));
  }

  private User activeUser(Long id) {
    User user = new User();
    user.setId(id);
    user.setOpenid("openid_" + id);
    user.setPhone("encrypted_phone_" + id);
    user.setIdentityStatus("注册用户");
    user.setProfileCompleted(false);
    user.setStatus(UserStatus.ACTIVE.getValue());
    return user;
  }
}
