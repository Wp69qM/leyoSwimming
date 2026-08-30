package com.leyoswimming.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserCoachControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private CoachMapper coachMapper;
  @Autowired private PackageTemplateMapper packageTemplateMapper;
  @Autowired private PackageTemplateCoachMapper packageTemplateCoachMapper;
  @Autowired private PhoneEncryptor phoneEncryptor;
  @Autowired private StringRedisTemplate redisTemplate;

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
  }

  @Test
  @DisplayName("POST /api/coach/list 无需登录返回公开教练列表")
  void list_anonymous_returnsPublicCoaches() throws Exception {
    createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("4.9"), "空闲中");
    createCoach("李教练", CoachStatus.RESIGNING.getValue(), new BigDecimal("4.5"), "空闲中");

    mockMvc
        .perform(
            post("/api/coach/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":1,\"pageSize\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.items.length()").value(2))
        .andExpect(jsonPath("$.data.items[0].name").value("王教练"))
        .andExpect(jsonPath("$.data.items[0].rating").value(4.9));
  }

  @Test
  @DisplayName("POST /api/coach/list 无公开教练返回空列表")
  void list_noPublicCoaches_returnsEmpty() throws Exception {
    mockMvc
        .perform(
            post("/api/coach/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":1,\"pageSize\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.items").isEmpty());
  }

  @Test
  @DisplayName("POST /api/coach/detail 无需登录返回已通过教练详情")
  void detail_anonymous_returnsActiveCoach() throws Exception {
    Coach coach =
        createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("4.9"), "空闲中");

    mockMvc
        .perform(
            post("/api/coach/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d}", coach.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.name").value("王教练"))
        .andExpect(jsonPath("$.data.realTimeStatus").value("空闲中"));
  }

  @Test
  @DisplayName("POST /api/coach/detail 申请离职中教练可查看")
  void detail_resigningCoach_returnsOk() throws Exception {
    Coach coach =
        createCoach("李教练", CoachStatus.RESIGNING.getValue(), new BigDecimal("4.7"), "空闲中");

    mockMvc
        .perform(
            post("/api/coach/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d}", coach.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value(CoachStatus.RESIGNING.getValue()));
  }

  @Test
  @DisplayName("POST /api/coach/detail 待审核教练返回 410001")
  void detail_pendingCoach_notFound() throws Exception {
    Coach coach =
        createCoach("张教练", CoachStatus.PENDING.getValue(), new BigDecimal("4.0"), "空闲中");

    mockMvc
        .perform(
            post("/api/coach/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d}", coach.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(410001));
  }

  @Test
  @DisplayName("POST /api/coach/detail 已离职教练返回 410001")
  void detail_resignedCoach_notFound() throws Exception {
    Coach coach =
        createCoach("刘教练", CoachStatus.RESIGNED.getValue(), new BigDecimal("4.0"), "空闲中");

    mockMvc
        .perform(
            post("/api/coach/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d}", coach.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(410001));
  }

  @Test
  @DisplayName("POST /api/coach/detail 返回已上架套餐预览")
  void detail_activePackages_returnsPreview() throws Exception {
    Coach coach =
        createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("4.9"), "空闲中");
    PackageTemplate template = createActiveTemplate("标准 6 节", "standard", 6);
    linkTemplateToCoach(template, coach);

    mockMvc
        .perform(
            post("/api/coach/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d}", coach.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.packages.length()").value(1))
        .andExpect(jsonPath("$.data.packages[0].id").value(template.getId()))
        .andExpect(jsonPath("$.data.packages[0].name").value("标准 6 节"));
  }

  private PackageTemplate createActiveTemplate(String name, String packageMode, int totalHours) {
    PackageTemplate template = new PackageTemplate();
    template.setName(name);
    template.setPackageMode(packageMode);
    template.setTeachingType("one_on_one");
    template.setTotalHours(totalHours);
    template.setDurationMinutes(60);
    template.setValidDays(30);
    template.setOriginalPrice(new BigDecimal("1200.00"));
    template.setPrice(new BigDecimal("1080.00"));
    template.setRefundEnabled(false);
    template.setStatus("active");
    packageTemplateMapper.insert(template);
    assertThat(template.getId()).isNotNull();
    return template;
  }

  private void linkTemplateToCoach(PackageTemplate template, Coach coach) {
    PackageTemplateCoach link = new PackageTemplateCoach();
    link.setPackageTemplateId(template.getId());
    link.setCoachId(coach.getId());
    link.setReferencePriceSnapshot(coach.getReferencePrice());
    packageTemplateCoachMapper.insert(link);
  }

  private Coach createCoach(
      String name, int status, BigDecimal rating, String realtimeStatus) throws Exception {
    Coach coach = new Coach();
    coach.setOpenid(name + System.nanoTime());
    coach.setPhone(phoneEncryptor.encrypt("1380000" + String.format("%04d", (int) (Math.random() * 10000))));
    coach.setPhoneHash(
        phoneEncryptor.hash("1380000" + String.format("%04d", (int) (Math.random() * 10000))));
    coach.setName(name);
    coach.setStatus(status);
    coach.setRating(rating);
    coach.setRealtimeStatus(realtimeStatus);
    coach.setTeachingStrokes("蛙泳,自由泳");
    coachMapper.insert(coach);
    assertThat(coach.getId()).isNotNull();
    return coach;
  }
}
