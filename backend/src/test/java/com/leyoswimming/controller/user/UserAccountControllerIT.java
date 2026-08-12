package com.leyoswimming.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.UserMapper;
import java.math.BigDecimal;
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
class UserAccountControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserMapper userMapper;
  @Autowired private PackageMapper packageMapper;

  @Test
  @DisplayName("POST /api/user/account/cancel-check 无活跃套餐返回可以注销")
  void cancelCheck_noActivePackage_returnsCanCancel() throws Exception {
    String accessToken = obtainUserAccessToken("user_cancel_check_no_package");

    mockMvc
        .perform(
            post("/api/user/account/cancel-check")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.canCancel").value(true))
        .andExpect(jsonPath("$.data.checks.noActivePackage").value(true))
        .andExpect(jsonPath("$.data.checks.noPendingOrder").value(true))
        .andExpect(jsonPath("$.data.checks.noOngoingBooking").value(true));
  }

  @Test
  @DisplayName("POST /api/user/account/cancel-check 存在活跃套餐返回不可注销")
  void cancelCheck_withActivePackage_returnsCannotCancel() throws Exception {
    LoginInfo info = obtainLoginInfo("user_cancel_check_with_package");
    insertActivePackage(info.userId, 1L);

    mockMvc
        .perform(
            post("/api/user/account/cancel-check")
                .header("Authorization", "Bearer " + info.accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.canCancel").value(false))
        .andExpect(jsonPath("$.data.checks.noActivePackage").value(false));
  }

  @Test
  @DisplayName("POST /api/user/account/cancel 无活跃套餐成功注销")
  void cancel_noActivePackage_cancelsAccount() throws Exception {
    String accessToken = obtainUserAccessToken("user_cancel_success");

    mockMvc
        .perform(
            post("/api/user/account/cancel")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.cancelled").value(true))
        .andExpect(jsonPath("$.data.anonymousAfter").isString());
  }

  @Test
  @DisplayName("POST /api/user/account/cancel 已注销用户再次调用返回幂等成功")
  void cancel_alreadyCancelledUser_returnsIdempotentSuccess() throws Exception {
    String accessToken = obtainUserAccessToken("user_cancel_idempotent");

    mockMvc
        .perform(
            post("/api/user/account/cancel")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    String anonymousAfter =
        mockMvc
            .perform(
                post("/api/user/account/cancel")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode body = objectMapper.readTree(anonymousAfter).path("data");
    assertThat(body.path("cancelled").asBoolean()).isTrue();
    assertThat(body.path("anonymousAfter").asText()).isNotBlank();
  }

  @Test
  @DisplayName("POST /api/user/account/cancel 存在活跃套餐返回 400201")
  void cancel_withActivePackage_returnsActivePackageExists() throws Exception {
    LoginInfo info = obtainLoginInfo("user_cancel_with_package");
    insertActivePackage(info.userId, 2L);

    mockMvc
        .perform(
            post("/api/user/account/cancel")
                .header("Authorization", "Bearer " + info.accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400201))
        .andExpect(jsonPath("$.message").value("您还有未完成的套餐，无法注销"));
  }

  @Test
  @DisplayName("访问注销接口未携带 token 返回 401")
  void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/user/account/cancel-check")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  private String obtainUserAccessToken(String wxCode) throws Exception {
    return obtainLoginInfo(wxCode).accessToken;
  }

  private LoginInfo obtainLoginInfo(String wxCode) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/user/auth/wechat-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            "{\"code\":\"%s\",\"phoneEncryptedData\":\"data\",\"phoneIv\":\"iv\",\"termsAccepted\":true,\"privacyAccepted\":true,\"termsVersion\":\"v1.0\",\"privacyVersion\":\"v1.0\"}",
                            wxCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    JsonNode data =
        objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return new LoginInfo(data.path("userId").asLong(), data.path("accessToken").asText());
  }

  private record LoginInfo(long userId, String accessToken) {}

  private void insertActivePackage(long userId, long coachId) {
    CoursePackage pkg = new CoursePackage();
    pkg.setUserId(userId);
    pkg.setCoachId(coachId);
    pkg.setTotalHours(10);
    pkg.setConsumedCount(0);
    pkg.setReservedCount(0);
    pkg.setAvailableCount(10);
    pkg.setPricePerHour(BigDecimal.valueOf(200));
    pkg.setPaidAmount(BigDecimal.valueOf(2000));
    pkg.setStatus("active");
    packageMapper.insert(pkg);
  }
}
