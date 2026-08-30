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
class CoachApplicationControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CoachMapper coachMapper;

  private static final String SUBMIT_BODY = """
      {"name":"张三","gender":"male","age":25,"email":"test@example.com",\
      "wechatQrUrl":"http://qr","idCardNo":"110101199001011234","teachingYears":5,\
      "totalStudents":100,"totalHours":1000,"teachingStrokes":["freestyle","breaststroke"],\
      "bio":"专业游泳教练",\
      "referencePrice":200,\
      "certificates":[\
      {"certType":"ID_CARD_FRONT","imageUrl":"http://id-front"},\
      {"certType":"ID_CARD_BACK","imageUrl":"http://id-back"},\
      {"certType":"COACH_CERT","imageUrl":"http://coach-cert"},\
      {"certType":"HEALTH_CERT","imageUrl":"http://health-cert"},\
      {"certType":"PORTRAIT","imageUrl":"http://portrait"}],\
      "idempotencyKey":"submit-key"}
      """;

  @Test
  @DisplayName("POST /api/coach/application/detail 未提交教练返回 first 入口")
  void detail_notSubmitted_returnsFirstEntry() throws Exception {
    String accessToken = obtainCoachAccessToken("coach_app_detail_first");

    mockMvc
        .perform(
            post("/api/coach/application/detail")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value(-1))
        .andExpect(jsonPath("$.data.entryType").value("first"));
  }

  @Test
  @DisplayName("POST /api/coach/application/save-draft 首次保存草稿成功")
  void saveDraft_firstTime_returnsSuccess() throws Exception {
    String accessToken = obtainCoachAccessToken("coach_app_draft");
    String idempotencyKey = "draft-key-" + System.currentTimeMillis();

    mockMvc
        .perform(
            post("/api/coach/application/save-draft")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        """
                    {"name":"张三","age":25,"idempotencyKey":"%s"}
                    """,
                        idempotencyKey)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.applicationId").isNumber());
  }

  @Test
  @DisplayName("POST /api/coach/application/submit 完整资料提交后进入 pending")
  void submit_completeApplication_returnsPending() throws Exception {
    String accessToken = obtainCoachAccessToken("coach_app_submit");
    String body = SUBMIT_BODY.replace("\"idempotencyKey\":\"submit-key\"",
        "\"idempotencyKey\":\"submit-key-" + System.currentTimeMillis() + "\"");

    mockMvc
        .perform(
            post("/api/coach/application/submit")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value(0))
        .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());

    long coachId = obtainCoachId("coach_app_submit");
    Coach coach = coachMapper.selectById(coachId);
    assertThat(coach.getStatus()).isEqualTo(CoachStatus.PENDING.getValue());
  }

  @Test
  @DisplayName("POST /api/coach/application/submit 缺少证书返回 BAD_REQUEST")
  void submit_missingCertificates_returnsBadRequest() throws Exception {
    String accessToken = obtainCoachAccessToken("coach_app_submit_invalid");

    mockMvc
        .perform(
            post("/api/coach/application/submit")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"张三","gender":"male","age":25,"email":"test@example.com",\
                    "wechatQrUrl":"http://qr","idCardNo":"110101199001011234",\
                    "teachingYears":5,"totalStudents":100,"totalHours":1000,\
                    "teachingStrokes":["freestyle"],\
                    "bio":"专业游泳教练","referencePrice":200,\
                    "certificates":[{"certType":"PORTRAIT","imageUrl":"http://portrait"}],\
                    "idempotencyKey":"submit-invalid"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(100001));
  }

  @Test
  @DisplayName("访问入驻接口未携带 token 返回 401")
  void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/coach/application/detail")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  private String obtainCoachAccessToken(String wxCode) throws Exception {
    return obtainLoginData(wxCode).path("accessToken").asText();
  }

  private long obtainCoachId(String wxCode) throws Exception {
    return obtainLoginData(wxCode).path("coachId").asLong();
  }

  private JsonNode obtainLoginData(String wxCode) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/coach/auth/wechat-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            """
                            {"code":"%s","phoneEncryptedData":"data","phoneIv":"iv",\
                            "termsAccepted":true,"privacyAccepted":true,\
                            "termsVersion":"v1.0","privacyVersion":"v1.0"}
                            """,
                            wxCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
  }
}
