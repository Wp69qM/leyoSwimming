package com.leyoswimming.controller.coach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.repository.SmsCodeMapper;
import com.leyoswimming.util.PhoneEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CoachAuthControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SmsCodeMapper smsCodeMapper;
  @Autowired private PhoneEncryptor phoneEncryptor;

  @Test
  @DisplayName("POST /api/coach/auth/wechat-login 新教练注册并登录成功")
  void wechatLogin_newCoach_returnsToken() throws Exception {
    mockMvc
        .perform(
            post("/api/coach/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"coach_wx_new","phoneEncryptedData":"data","phoneIv":"iv","termsAccepted":true,"privacyAccepted":true,"termsVersion":"v1.0","privacyVersion":"v1.0","avatarUrl":"avatar.jpg","nickName":"Coach"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.refreshToken").isString())
        .andExpect(jsonPath("$.data.isNewCoach").value(true))
        .andExpect(jsonPath("$.data.coachStatus").value(-1))
        .andExpect(jsonPath("$.data.coachId").isNumber());
  }

  @Test
  @DisplayName("POST /api/coach/auth/wechat-login 未同意协议返回 440001")
  void wechatLogin_termsNotAccepted_returnsBusinessError() throws Exception {
    mockMvc
        .perform(
            post("/api/coach/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"coach_wx_terms","phoneEncryptedData":"data","phoneIv":"iv","termsAccepted":true,"privacyAccepted":false,"termsVersion":"v1.0","privacyVersion":"v1.0"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(440001))
        .andExpect(jsonPath("$.message").value("请阅读并同意《用户须知》和《隐私协议》"));
  }

  @Test
  @DisplayName("POST /api/coach/auth/phone-login 使用有效验证码登录成功")
  void phoneLogin_validSmsCode_returnsToken() throws Exception {
    seedSmsCode("13800138200", "123456", "coach");

    mockMvc
        .perform(
            post("/api/coach/auth/phone-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"phone":"13800138200","code":"123456","termsAccepted":true,"privacyAccepted":true,"termsVersion":"v1.0","privacyVersion":"v1.0"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.isNewCoach").value(true))
        .andExpect(jsonPath("$.data.coachId").isNumber());
  }

  @Test
  @DisplayName("POST /api/coach/auth/phone-login 错误验证码返回 420001")
  void phoneLogin_invalidSmsCode_returnsBusinessError() throws Exception {
    seedSmsCode("13800138201", "123456", "coach");

    mockMvc
        .perform(
            post("/api/coach/auth/phone-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"phone":"13800138201","code":"000000","termsAccepted":true,"privacyAccepted":true,"termsVersion":"v1.0","privacyVersion":"v1.0"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(420001))
        .andExpect(jsonPath("$.message").value("验证码错误或已过期"));
  }

  @Test
  @DisplayName("POST /api/coach/auth/refresh 有效 refresh token 返回新 access token")
  void refresh_validRefreshToken_returnsNewTokens() throws Exception {
    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/coach/auth/wechat-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"code":"coach_wx_refresh","phoneEncryptedData":"data","phoneIv":"iv","termsAccepted":true,"privacyAccepted":true,"termsVersion":"v1.0","privacyVersion":"v1.0"}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    JsonNode loginBody =
        objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data");
    String refreshToken = loginBody.path("refreshToken").asText();

    mockMvc
        .perform(
            post("/api/coach/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.expiresInSeconds").value(86400));
  }

  @Test
  @DisplayName("POST /api/coach/auth/logout 携带有效 access token 登出成功")
  void logout_withAccessToken_returnsOk() throws Exception {
    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/coach/auth/wechat-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"code":"coach_wx_logout","phoneEncryptedData":"data","phoneIv":"iv","termsAccepted":true,"privacyAccepted":true,"termsVersion":"v1.0","privacyVersion":"v1.0"}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    JsonNode loginBody =
        objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data");
    String accessToken = loginBody.path("accessToken").asText();
    String refreshToken = loginBody.path("refreshToken").asText();

    mockMvc
        .perform(
            post("/api/coach/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  @DisplayName("访问受保护的教练接口未携带 token 返回 401")
  void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/coach/protected/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  private void seedSmsCode(String phone, String code, String appType) {
    com.leyoswimming.entity.SmsCode smsCode = new com.leyoswimming.entity.SmsCode();
    smsCode.setPhoneHash(phoneHash(phone));
    smsCode.setCode(code);
    smsCode.setScene("login");
    smsCode.setAppType(appType);
    smsCode.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(5));
    smsCode.setUsed(false);
    smsCodeMapper.insert(smsCode);
  }

  private String phoneHash(String phone) {
    try {
      return phoneEncryptor.hash(phone);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash phone", e);
    }
  }
}
