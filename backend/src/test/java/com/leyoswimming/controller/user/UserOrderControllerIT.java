package com.leyoswimming.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.CustomPackageConfig;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.enums.PackageMode;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CustomPackageConfigMapper;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.security.JwtTokenProvider;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class UserOrderControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private UserMapper userMapper;
  @Autowired private CoachMapper coachMapper;
  @Autowired private PackageTemplateMapper packageTemplateMapper;
  @Autowired private PackageTemplateCoachMapper packageTemplateCoachMapper;
  @Autowired private CustomPackageConfigMapper customPackageConfigMapper;
  @Autowired private OrderMapper orderMapper;
  @Autowired private PackageMapper packageMapper;

  private String token;
  private Long userId;

  @BeforeEach
  void setUp() {
    User user = createUser("学员 A");
    userId = user.getId();
    token = jwtTokenProvider.generateUserAccessToken(userId, true);
  }

  @Test
  @DisplayName("POST /api/order/trial 未登录返回 401")
  void trial_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/order/trial")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coachId\":1}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST /api/order/trial 创建体验课订单成功")
  void trial_validCoach_returnsOrder() throws Exception {
    Coach coach = createCoach("教练 A", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    PackageTemplate template = createActiveTemplate(
        "体验课", PackageMode.EXPERIENCE.getValue(), 1, new BigDecimal("59.00"));
    linkTemplateToCoach(template, coach);

    mockMvc
        .perform(
            post("/api/order/trial")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d,\"packageId\":%d}", coach.getId(), template.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.orderId").isNumber())
        .andExpect(jsonPath("$.data.amount").value(59.00));
  }

  @Test
  @DisplayName("POST /api/order/trial 重复购买体验课返回 420101")
  void trial_duplicateExperience_returnsError() throws Exception {
    Coach coach = createCoach("教练 B", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    createExperiencePackage(userId, coach.getId());
    PackageTemplate template = createActiveTemplate(
        "体验课", PackageMode.EXPERIENCE.getValue(), 1, new BigDecimal("59.00"));
    linkTemplateToCoach(template, coach);

    mockMvc
        .perform(
            post("/api/order/trial")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d,\"packageId\":%d}", coach.getId(), template.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(420101));
  }

  @Test
  @DisplayName("POST /api/order/trial 教练不适用该体验课返回 420001")
  void trial_coachNotLinked_returnsError() throws Exception {
    Coach coach = createCoach("教练 F", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    PackageTemplate template = createActiveTemplate(
        "体验课", PackageMode.EXPERIENCE.getValue(), 1, new BigDecimal("59.00"));

    mockMvc
        .perform(
            post("/api/order/trial")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d,\"packageId\":%d}", coach.getId(), template.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(420001));
  }

  @Test
  @DisplayName("POST /api/order/formal 创建标准套餐订单成功")
  void formal_standardPackage_returnsOrder() throws Exception {
    Coach coach = createCoach("教练 C", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    PackageTemplate template = createActiveTemplate("标准 6 节", PackageMode.STANDARD.getValue(), 6, new BigDecimal("1200.00"));
    linkTemplateToCoach(template, coach);

    mockMvc
        .perform(
            post("/api/order/formal")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"coachId\":%d,\"packageId\":%d,\"strokeIds\":[1,2]}",
                    coach.getId(), template.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.orderId").isNumber())
        .andExpect(jsonPath("$.data.amount").value(1200.00));
  }

  @Test
  @DisplayName("POST /api/order/formal 自定义套餐按参考单价计价")
  void formal_customPackage_calculatesByReferencePrice() throws Exception {
    Coach coach = createCoach("教练 D", CoachStatus.APPROVED.getValue(), new BigDecimal("300.00"));
    PackageTemplate template = createActiveTemplate("自定义套餐", PackageMode.CUSTOM.getValue(), 1, BigDecimal.ZERO);
    createCustomConfig(1, 50);

    mockMvc
        .perform(
            post("/api/order/formal")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"coachId\":%d,\"packageId\":%d,\"hours\":10,\"validDays\":60}",
                    coach.getId(), template.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.amount").value(3000.00));
  }

  @Test
  @DisplayName("POST /api/order/formal 用户已持有其他教练 active 套餐返回 420101")
  void formal_activePackageWithOtherCoach_returnsError() throws Exception {
    Coach otherCoach = createCoach("教练 E-其他", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    createActivePackage(userId, otherCoach.getId());

    Coach coach = createCoach("教练 E-新", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    PackageTemplate template = createActiveTemplate("标准 6 节", PackageMode.STANDARD.getValue(), 6, new BigDecimal("1200.00"));
    linkTemplateToCoach(template, coach);

    mockMvc
        .perform(
            post("/api/order/formal")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"coachId\":%d,\"packageId\":%d,\"strokeIds\":[1,2]}",
                    coach.getId(), template.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(420101));
  }

  private User createUser(String name) {
    User user = new User();
    user.setOpenid("openid-" + name);
    user.setPhone("1380000" + (int) (Math.random() * 10000));
    user.setName(name);
    user.setStatus(0);
    userMapper.insert(user);
    return user;
  }

  private Coach createCoach(String name, Integer status, BigDecimal referencePrice) {
    Coach coach = new Coach();
    coach.setOpenid("openid-" + name);
    coach.setPhone("1390000" + (int) (Math.random() * 10000));
    coach.setName(name);
    coach.setStatus(status);
    coach.setReferencePrice(referencePrice);
    coachMapper.insert(coach);
    return coach;
  }

  private PackageTemplate createActiveTemplate(
      String name, String packageMode, int totalHours, BigDecimal price) {
    PackageTemplate template = new PackageTemplate();
    template.setName(name);
    template.setPackageMode(packageMode);
    template.setTeachingType("one_on_one");
    template.setTotalHours(totalHours);
    template.setDurationMinutes(60);
    template.setValidDays(30);
    template.setOriginalPrice(price);
    template.setPrice(price);
    template.setRefundEnabled(false);
    template.setRefundRatio(BigDecimal.ZERO);
    template.setRefundValidDays(0);
    template.setStatus(PackageTemplateStatus.ACTIVE.getValue());
    packageTemplateMapper.insert(template);
    return template;
  }

  private void linkTemplateToCoach(PackageTemplate template, Coach coach) {
    PackageTemplateCoach link = new PackageTemplateCoach();
    link.setPackageTemplateId(template.getId());
    link.setCoachId(coach.getId());
    link.setReferencePriceSnapshot(coach.getReferencePrice());
    packageTemplateCoachMapper.insert(link);
  }

  private void createCustomConfig(int minHours, int maxHours) {
    CustomPackageConfig config = new CustomPackageConfig();
    config.setConfigKey("global");
    config.setMinHours(minHours);
    config.setMaxHours(maxHours);
    config.setDefaultValidDays(30);
    customPackageConfigMapper.insert(config);
  }

  private void createExperiencePackage(Long userId, Long coachId) {
    CoursePackage pkg = new CoursePackage();
    pkg.setUserId(userId);
    pkg.setCoachId(coachId);
    pkg.setPackageMode(PackageMode.EXPERIENCE.getValue());
    pkg.setTotalHours(1);
    pkg.setAvailableCount(1);
    pkg.setConsumedCount(0);
    pkg.setReservedCount(0);
    pkg.setPricePerHour(BigDecimal.ZERO);
    pkg.setPaidAmount(BigDecimal.ZERO);
    pkg.setStatus("active");
    packageMapper.insert(pkg);
  }

  private void createActivePackage(Long userId, Long coachId) {
    CoursePackage pkg = new CoursePackage();
    pkg.setUserId(userId);
    pkg.setCoachId(coachId);
    pkg.setPackageMode(PackageMode.STANDARD.getValue());
    pkg.setTotalHours(6);
    pkg.setAvailableCount(6);
    pkg.setConsumedCount(0);
    pkg.setReservedCount(0);
    pkg.setPricePerHour(new BigDecimal("200.00"));
    pkg.setPaidAmount(new BigDecimal("1200.00"));
    pkg.setStatus("active");
    packageMapper.insert(pkg);
  }
}
