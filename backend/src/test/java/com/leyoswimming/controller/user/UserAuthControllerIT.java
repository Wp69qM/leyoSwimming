package com.leyoswimming.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.repository.SmsCodeMapper;
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
class UserAuthControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SmsCodeMapper smsCodeMapper;

  @Test
  @DisplayName("POST /api/user/auth/wechat-login 新用户注册并登录成功")
  void wechatLogin_newUser_returnsToken() throws Exception {
    mockMvc
        .perform(
            post("/api/user/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"user_wx_new","phoneEncryptedData":"data","phoneIv":"iv","termsAccepted":true,"privacyAccepted":true,"avatarUrl":"avatar.jpg","nickName":"User"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.refreshToken").isString())
        .andExpect(jsonPath("$.data.isNewUser").value(true))
        .andExpect(jsonPath("$.data.profileCompleted").value(false))
        .andExpect(jsonPath("$.data.userId").isNumber());
  }

  @Test
  @DisplayName("POST /api/user/auth/wechat-login 未同意协议返回 440001")
  void wechatLogin_termsNotAccepted_returnsBusinessError() throws Exception {
    mockMvc
        .perform(
            post("/api/user/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"user_wx_terms","phoneEncryptedData":"data","phoneIv":"iv","termsAccepted":false,"privacyAccepted":true}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(440001))
        .andExpect(jsonPath("$.message").value("请阅读并同意《用户须知》和《隐私协议》"));
  }

  @Test
  @DisplayName("POST /api/user/auth/phone-login 使用有效验证码登录成功")
  void phoneLogin_validSmsCode_returnsToken() throws Exception {
    seedSmsCode("13800138100", "123456", "user");

    mockMvc
        .perform(
            post("/api/user/auth/phone-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"phone":"13800138100","code":"123456","termsAccepted":true,"privacyAccepted":true}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.isNewUser").value(true))
        .andExpect(jsonPath("$.data.userId").isNumber());
  }

  @Test
  @DisplayName("POST /api/user/auth/phone-login 错误验证码返回 420001")
  void phoneLogin_invalidSmsCode_returnsBusinessError() throws Exception {
    seedSmsCode("13800138101", "123456", "user");

    mockMvc
        .perform(
            post("/api/user/auth/phone-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"phone":"13800138101","code":"000000","termsAccepted":true,"privacyAccepted":true}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(420001))
        .andExpect(jsonPath("$.message").value("验证码错误或已过期"));
  }

  @Test
  @DisplayName("POST /api/user/auth/refresh 有效 refresh token 返回新 access token")
  void refresh_validRefreshToken_returnsNewTokens() throws Exception {
    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/user/auth/wechat-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"code":"user_wx_refresh","phoneEncryptedData":"data","phoneIv":"iv","termsAccepted":true,"privacyAccepted":true}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    JsonNode loginBody =
        objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data");
    String refreshToken = loginBody.path("refreshToken").asText();

    mockMvc
        .perform(
            post("/api/user/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.refreshToken").isString())
        .andExpect(jsonPath("$.data.expiresInSeconds").value(86400));
  }

  @Test
  @DisplayName("POST /api/user/auth/logout 携带有效 access token 登出成功")
  void logout_withAccessToken_returnsOk() throws Exception {
    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/user/auth/wechat-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"code":"user_wx_logout","phoneEncryptedData":"data","phoneIv":"iv","termsAccepted":true,"privacyAccepted":true}
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
            post("/api/user/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  @DisplayName("访问受保护的用户接口未携带 token 返回 401")
  void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/user/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"dummy\"}"))
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
      java.security.MessageDigest digest =
          java.security.MessageDigest.getInstance("SHA-256");
      return java.util.Base64.getEncoder().encodeToString(digest.digest(phone.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
