package com.leyoswimming.controller.coach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CoachProfileControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CoachMapper coachMapper;
  @Autowired private PhoneEncryptor phoneEncryptor;
  @Autowired private StringRedisTemplate redisTemplate;

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
  }

  @Test
  @DisplayName("POST /api/coach/profile/detail 获取当前教练主页成功")
  void detail_loggedInCoach_returnsProfile() throws Exception {
    CoachTokens tokens = loginAsNewCoach("coach_profile_detail");

    mockMvc
        .perform(
            post("/api/coach/profile/detail")
                .header("Authorization", "Bearer " + tokens.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").isNumber())
        .andExpect(jsonPath("$.data.consent").exists());
  }

  @Test
  @DisplayName("POST /api/coach/profile/update 审核通过教练更新资料成功")
  void update_approvedCoach_updatesProfile() throws Exception {
    CoachTokens tokens = loginAsNewCoach("coach_profile_update");
    approveCoach(tokens.coachId);

    mockMvc
        .perform(
            post("/api/coach/profile/update")
                .header("Authorization", "Bearer " + tokens.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"教练新名","age":28,"gender":"male","email":"coach@example.com","teachingYears":5,"teachingStrokes":["蛙泳","自由泳"],"bio":"我是一名专业游泳教练，擅长基础教学。","idempotencyKey":"update-1"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.name").value("教练新名"))
        .andExpect(jsonPath("$.data.email").value("coach@example.com"));
  }

  @Test
  @DisplayName("POST /api/coach/profile/update 未审核通过返回 410006")
  void update_notApprovedCoach_returnsStatusNotApproved() throws Exception {
    CoachTokens tokens = loginAsNewCoach("coach_profile_update_not_approved");

    mockMvc
        .perform(
            post("/api/coach/profile/update")
                .header("Authorization", "Bearer " + tokens.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"教练","age":28,"gender":"male","idempotencyKey":"update-2"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(410006));
  }

  @Test
  @DisplayName("POST /api/coach/reference-price/update 审核通过教练改价成功")
  void updateReferencePrice_approvedCoach_updatesPrice() throws Exception {
    CoachTokens tokens = loginAsNewCoach("coach_price_update");
    approveCoach(tokens.coachId);

    mockMvc
        .perform(
            post("/api/coach/profile/reference-price/update")
                .header("Authorization", "Bearer " + tokens.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"referencePrice":150.00,"idempotencyKey":"price-1"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.referencePrice").value(150.00))
        .andExpect(jsonPath("$.data.remainingChangesToday").value(2));
  }

  @Test
  @DisplayName("POST /api/coach/reference-price/update 价格超出范围返回 410008")
  void updateReferencePrice_invalidPrice_returnsInvalidReferencePrice() throws Exception {
    CoachTokens tokens = loginAsNewCoach("coach_price_invalid");
    approveCoach(tokens.coachId);

    mockMvc
        .perform(
            post("/api/coach/profile/reference-price/update")
                .header("Authorization", "Bearer " + tokens.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"referencePrice":3000.00,"idempotencyKey":"price-2"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(410008));
  }

  @Test
  @DisplayName("POST /api/coach/reference-price/update 超过每日次数限制返回 410009")
  void updateReferencePrice_dailyLimitReached_returnsLimitReached() throws Exception {
    CoachTokens tokens = loginAsNewCoach("coach_price_limit");
    Coach coach = approveCoach(tokens.coachId);
    coach.setPriceChangeCountToday(3);
    coach.setPriceChangedAt(java.time.LocalDateTime.now());
    coachMapper.updateById(coach);

    mockMvc
        .perform(
            post("/api/coach/profile/reference-price/update")
                .header("Authorization", "Bearer " + tokens.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"referencePrice":200.00,"idempotencyKey":"price-3"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(410009));
  }

  private CoachTokens loginAsNewCoach(String code) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/coach/auth/wechat-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            """
                            {"code":"%s","phoneEncryptedData":"data","phoneIv":"iv","termsAccepted":true,"privacyAccepted":true,"termsVersion":"v1.0","privacyVersion":"v1.0"}
                            """,
                            code)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return new CoachTokens(
        body.path("accessToken").asText(),
        body.path("refreshToken").asText(),
        body.path("coachId").asLong());
  }

  private Coach approveCoach(Long coachId) {
    Coach coach = coachMapper.selectById(coachId);
    assertThat(coach).isNotNull();
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coachMapper.updateById(coach);
    return coach;
  }

  private record CoachTokens(String accessToken, String refreshToken, Long coachId) {}
}
