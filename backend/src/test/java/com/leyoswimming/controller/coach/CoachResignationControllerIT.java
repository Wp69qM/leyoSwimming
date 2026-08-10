package com.leyoswimming.controller.coach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.PackageMapper;
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
class CoachResignationControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CoachMapper coachMapper;
  @Autowired private PackageMapper packageMapper;

  @Test
  @DisplayName("POST /api/coach/resignation/apply 已认证教练成功提交离职申请")
  void apply_approvedCoach_createsTicket() throws Exception {
    CoachLogin login = loginApprovedCoach("coach_resign_apply");

    mockMvc
        .perform(
            post("/api/coach/resignation/apply")
                .header("Authorization", "Bearer " + login.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"个人原因\",\"idempotencyKey\":\"key1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.ticketId").isNumber())
        .andExpect(jsonPath("$.data.ticketNo").isString())
        .andExpect(jsonPath("$.data.totalPackages").value(0))
        .andExpect(jsonPath("$.data.message").value("离职申请已提交，请处理学员套餐"));
  }

  @Test
  @DisplayName("POST /api/coach/resignation/apply 非已认证教练返回 410002")
  void apply_notApprovedCoach_returnsCoachStatusNotAllowed() throws Exception {
    String accessToken = obtainCoachAccessToken("coach_resign_not_approved");

    mockMvc
        .perform(
            post("/api/coach/resignation/apply")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"个人原因\",\"idempotencyKey\":\"key2\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(410002))
        .andExpect(jsonPath("$.message").value("当前状态不可申请离职"));
  }

  @Test
  @DisplayName("POST /api/coach/resignation/detail 返回当前离职工单详情")
  void detail_withTicket_returnsDetail() throws Exception {
    CoachLogin login = loginApprovedCoach("coach_resign_detail");
    insertActivePackage(login.coachId, 200L);

    mockMvc
        .perform(
            post("/api/coach/resignation/apply")
                .header("Authorization", "Bearer " + login.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"个人原因\",\"idempotencyKey\":\"key3\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc
        .perform(
            post("/api/coach/resignation/detail")
                .header("Authorization", "Bearer " + login.accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("processing"))
        .andExpect(jsonPath("$.data.totalPackages").value(1))
        .andExpect(jsonPath("$.data.packages[0].userId").value(200));
  }

  @Test
  @DisplayName("POST /api/coach/resignation/package/action 登记退款处理")
  void registerAction_refund_registersAction() throws Exception {
    CoachLogin login = loginApprovedCoach("coach_resign_action");
    CoursePackage pkg = insertActivePackage(login.coachId, 201L);

    MvcResult applyResult =
        mockMvc
            .perform(
                post("/api/coach/resignation/apply")
                    .header("Authorization", "Bearer " + login.accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"个人原因\",\"idempotencyKey\":\"key4\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    Long ticketId =
        objectMapper
            .readTree(applyResult.getResponse().getContentAsString())
            .path("data")
            .path("ticketId")
            .asLong();

    mockMvc
        .perform(
            post("/api/coach/resignation/package/action")
                .header("Authorization", "Bearer " + login.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        "{\"ticketId\":%d,\"packageId\":%d,\"action\":\"refund\"}",
                        ticketId, pkg.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.action").value("refund"))
        .andExpect(jsonPath("$.data.refundAmount").value(2000.00));
  }

  @Test
  @DisplayName("POST /api/coach/resignation/submit 提交至管理员审批")
  void submit_withActions_submitsToAudit() throws Exception {
    CoachLogin login = loginApprovedCoach("coach_resign_submit");
    CoursePackage pkg = insertActivePackage(login.coachId, 202L);

    MvcResult applyResult =
        mockMvc
            .perform(
                post("/api/coach/resignation/apply")
                    .header("Authorization", "Bearer " + login.accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"个人原因\",\"idempotencyKey\":\"key5\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    Long ticketId =
        objectMapper
            .readTree(applyResult.getResponse().getContentAsString())
            .path("data")
            .path("ticketId")
            .asLong();

    mockMvc
        .perform(
            post("/api/coach/resignation/package/action")
                .header("Authorization", "Bearer " + login.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        "{\"ticketId\":%d,\"packageId\":%d,\"action\":\"refund\"}",
                        ticketId, pkg.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc
        .perform(
            post("/api/coach/resignation/submit")
                .header("Authorization", "Bearer " + login.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ticketId\":%d}", ticketId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc
        .perform(
            post("/api/coach/resignation/detail")
                .header("Authorization", "Bearer " + login.accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("pending_audit"))
        .andExpect(jsonPath("$.data.handledPackages").value(1));
  }

  @Test
  @DisplayName("访问离职接口未携带 token 返回 401")
  void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/coach/resignation/detail")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  private CoachLogin loginApprovedCoach(String wxCode) throws Exception {
    String accessToken = obtainCoachAccessToken(wxCode);
    long coachId = obtainCoachId(wxCode);

    Coach coach = coachMapper.selectById(coachId);
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coachMapper.updateById(coach);

    return new CoachLogin(coachId, accessToken);
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
                            "{\"code\":\"%s\",\"phoneEncryptedData\":\"data\",\"phoneIv\":\"iv\",\"termsAccepted\":true,\"privacyAccepted\":true}",
                            wxCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
  }

  private CoursePackage insertActivePackage(long coachId, long userId) {
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
    return pkg;
  }

  private record CoachLogin(long coachId, String accessToken) {}
}
