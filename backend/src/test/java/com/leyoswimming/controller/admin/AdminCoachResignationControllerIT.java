package com.leyoswimming.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.ScheduleSlot;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.ScheduleSlotMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class AdminCoachResignationControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CoachMapper coachMapper;
  @Autowired private PackageMapper packageMapper;
  @Autowired private ScheduleSlotMapper scheduleSlotMapper;

  @Test
  @DisplayName("POST /api/admin/coach/resignation-ticket/list 返回离职工单列表")
  void list_pendingTickets_returnsPagedItems() throws Exception {
    String adminToken = loginAdmin();
    CoachLogin coach = createApprovedCoachWithSubmittedTicket("coach_list_");

    mockMvc
        .perform(
            post("/api/admin/coach/resignation-ticket/list")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.total").isNumber())
        .andExpect(jsonPath("$.data.page").value(1));
  }

  @Test
  @DisplayName("POST /api/admin/coach/resignation-ticket/list 按状态过滤")
  void list_withStatusFilter_returnsMatchingTickets() throws Exception {
    String adminToken = loginAdmin();
    CoachLogin coach = createApprovedCoachWithSubmittedTicket("coach_filter_");

    mockMvc
        .perform(
            post("/api/admin/coach/resignation-ticket/list")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"pending_audit\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.items[0].status").value("pending_audit"))
        .andExpect(jsonPath("$.data.items[0].coachId").value(coach.coachId));
  }

  @Test
  @DisplayName("POST /api/admin/coach/resignation-ticket/detail 返回工单详情")
  void detail_existingTicket_returnsDetail() throws Exception {
    String adminToken = loginAdmin();
    CoachLogin coach = createApprovedCoachWithSubmittedTicket("coach_detail_");

    mockMvc
        .perform(
            post("/api/admin/coach/resignation-ticket/detail")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ticketId\":%d}", coach.ticketId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.ticketId").value(coach.ticketId))
        .andExpect(jsonPath("$.data.status").value("pending_audit"))
        .andExpect(jsonPath("$.data.coach.coachId").value(coach.coachId))
        .andExpect(jsonPath("$.data.checklist.allActionsRegistered").value(true));
  }

  @Test
  @DisplayName("POST /api/admin/coach/resignation-ticket/approve 审批通过并冻结套餐")
  void approve_allChecksPassed_approvesTicketAndFreezesPackage() throws Exception {
    String adminToken = loginAdmin();
    CoachLogin coach = createApprovedCoachWithSubmittedTicket("coach_approve_");

    mockMvc
        .perform(
            post("/api/admin/coach/resignation-ticket/approve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ticketId\":%d}", coach.ticketId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    Coach updatedCoach = coachMapper.selectById(coach.coachId);
    assertThat(updatedCoach.getStatus()).isEqualTo(CoachStatus.RESIGNED.getValue());

    CoursePackage updatedPackage = packageMapper.selectById(coach.packageId);
    assertThat(updatedPackage.getStatus()).isEqualTo("frozen");
    assertThat(updatedPackage.getFrozenReason()).isEqualTo("coach_resigned");
  }

  @Test
  @DisplayName("POST /api/admin/coach/resignation-ticket/approve 未来排班未清空返回 510002")
  void approve_scheduleNotCleared_returnsScheduleNotCleared() throws Exception {
    String adminToken = loginAdmin();
    CoachLogin coach = createApprovedCoachWithSubmittedTicket("coach_approve_schedule_");
    insertFutureScheduleSlot(coach.coachId);

    mockMvc
        .perform(
            post("/api/admin/coach/resignation-ticket/approve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ticketId\":%d}", coach.ticketId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(510002))
        .andExpect(jsonPath("$.message").value("未来排班未清空"));
  }

  @Test
  @DisplayName("POST /api/admin/coach/resignation-ticket/reject 审批拒绝并恢复教练状态")
  void reject_validTicket_rejectsAndRestoresCoach() throws Exception {
    String adminToken = loginAdmin();
    CoachLogin coach = createApprovedCoachWithSubmittedTicket("coach_reject_");

    mockMvc
        .perform(
            post("/api/admin/coach/resignation-ticket/reject")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ticketId\":%d,\"reason\":\"资料不全\"}", coach.ticketId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    Coach updatedCoach = coachMapper.selectById(coach.coachId);
    assertThat(updatedCoach.getStatus()).isEqualTo(CoachStatus.APPROVED.getValue());
  }

  @Test
  @DisplayName("访问管理员离职接口未携带 token 返回 401")
  void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/coach/resignation-ticket/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  private String loginAdmin() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .path("data")
        .path("token")
        .asText();
  }

  private CoachLogin createApprovedCoachWithSubmittedTicket(String prefix) throws Exception {
    String wxCode = prefix + System.nanoTime();
    String coachToken = obtainCoachAccessToken(wxCode);
    long coachId = obtainCoachId(wxCode);

    Coach coach = coachMapper.selectById(coachId);
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coachMapper.updateById(coach);

    CoursePackage pkg = insertActivePackage(coachId, 200L);

    MvcResult applyResult =
        mockMvc
            .perform(
                post("/api/coach/resignation/apply")
                    .header("Authorization", "Bearer " + coachToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            "{\"reason\":\"个人原因\",\"idempotencyKey\":\"%s\"}", wxCode)))
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
                .header("Authorization", "Bearer " + coachToken)
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
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ticketId\":%d}", ticketId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    return new CoachLogin(coachId, coachToken, ticketId, pkg.getId());
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

  private void insertFutureScheduleSlot(long coachId) {
    ScheduleSlot slot = new ScheduleSlot();
    slot.setCoachId(coachId);
    slot.setStartTime(LocalDateTime.now().plusDays(1));
    slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
    slot.setStatus("available");
    scheduleSlotMapper.insert(slot);
  }

  private record CoachLogin(long coachId, String accessToken, long ticketId, long packageId) {}
}
