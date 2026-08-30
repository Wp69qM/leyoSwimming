package com.leyoswimming.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CustomPackageConfig;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CustomPackageConfigMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
import java.util.List;
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
class UserPackageTemplateControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private CoachMapper coachMapper;
  @Autowired private PackageTemplateMapper packageTemplateMapper;
  @Autowired private PackageTemplateCoachMapper packageTemplateCoachMapper;
  @Autowired private CustomPackageConfigMapper customPackageConfigMapper;
  @Autowired private PhoneEncryptor phoneEncryptor;
  @Autowired private StringRedisTemplate redisTemplate;

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
  }

  @Test
  @DisplayName("POST /api/package/list 无需登录返回已上架套餐列表")
  void list_anonymous_returnsActivePackages() throws Exception {
    createActiveTemplate("标准 6 节", "standard", 6);
    createActiveTemplate("体验课", "experience", 1);

    mockMvc
        .perform(
            post("/api/package/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":1,\"pageSize\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.items.length()").value(2))
        .andExpect(jsonPath("$.data.total").value(2));
  }

  @Test
  @DisplayName("POST /api/package/list 无已上架套餐返回空列表")
  void list_noActivePackages_returnsEmpty() throws Exception {
    mockMvc
        .perform(
            post("/api/package/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":1,\"pageSize\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.items").isEmpty());
  }

  @Test
  @DisplayName("POST /api/coach/package/list 返回教练支持套餐和自定义入口")
  void coachPackageList_validCoach_returnsPackages() throws Exception {
    Coach coach = createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    PackageTemplate template = createActiveTemplate("标准 6 节", "standard", 6);
    linkTemplateToCoach(template, coach);
    createCustomConfig(1, 50);

    mockMvc
        .perform(
            post("/api/coach/package/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d}", coach.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.coachId").value(coach.getId()))
        .andExpect(jsonPath("$.data.standardPackages.length()").value(1))
        .andExpect(jsonPath("$.data.customPackageEnabled").value(true))
        .andExpect(jsonPath("$.data.customHoursMax").value(50));
  }

  @Test
  @DisplayName("POST /api/coach/package/list 教练未设参考单价时自定义入口禁用")
  void coachPackageList_noReferencePrice_customDisabled() throws Exception {
    Coach coach = createCoach("李教练", CoachStatus.APPROVED.getValue(), null);
    PackageTemplate template = createActiveTemplate("标准 6 节", "standard", 6);
    linkTemplateToCoach(template, coach);

    mockMvc
        .perform(
            post("/api/coach/package/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d}", coach.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.customPackageEnabled").value(false));
  }

  @Test
  @DisplayName("POST /api/coach/package/list 不可约教练返回 410001")
  void coachPackageList_notPublicCoach_notFound() throws Exception {
    Coach coach = createCoach("张教练", CoachStatus.PENDING.getValue(), new BigDecimal("200.00"));

    mockMvc
        .perform(
            post("/api/coach/package/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"coachId\":%d}", coach.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(410001));
  }

  @Test
  @DisplayName("POST /api/package/detail 返回模板详情和适用教练")
  void detail_anonymous_returnsTemplateDetail() throws Exception {
    Coach coach = createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    PackageTemplate template = createActiveTemplate("标准 6 节", "standard", 6);
    linkTemplateToCoach(template, coach);

    mockMvc
        .perform(
            post("/api/package/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"packageId\":%d}", template.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").value(template.getId()))
        .andExpect(jsonPath("$.data.applicableCoaches.length()").value(1))
        .andExpect(jsonPath("$.data.images.length()").value(1))
        .andExpect(jsonPath("$.data.images[0]").value("https://example.com/img.png"))
        .andExpect(jsonPath("$.data.tags[0]").value("popular"));
  }

  @Test
  @DisplayName("POST /api/package/detail 传入 coachId 仅返回选中教练")
  void detail_withCoachId_returnsSelectedCoach() throws Exception {
    Coach c1 = createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    Coach c2 = createCoach("李教练", CoachStatus.APPROVED.getValue(), new BigDecimal("180.00"));
    PackageTemplate template = createActiveTemplate("标准 6 节", "standard", 6);
    linkTemplateToCoach(template, c1);
    linkTemplateToCoach(template, c2);

    mockMvc
        .perform(
            post("/api/package/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"packageId\":%d,\"coachId\":%d}", template.getId(), c1.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.applicableCoaches.length()").value(1))
        .andExpect(jsonPath("$.data.applicableCoaches[0].coachId").value(c1.getId()));
  }

  @Test
  @DisplayName("POST /api/package/detail 未传 coachId 时过滤非公开教练")
  void detail_withoutCoachId_filtersNonPublicCoaches() throws Exception {
    Coach approved = createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    Coach resigning = createCoach("李教练", CoachStatus.RESIGNING.getValue(), new BigDecimal("180.00"));
    Coach pending = createCoach("张教练", CoachStatus.PENDING.getValue(), new BigDecimal("180.00"));
    PackageTemplate template = createActiveTemplate("标准 6 节", "standard", 6);
    linkTemplateToCoach(template, approved);
    linkTemplateToCoach(template, resigning);
    linkTemplateToCoach(template, pending);

    mockMvc
        .perform(
            post("/api/package/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"packageId\":%d}", template.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.applicableCoaches.length()").value(2));
  }

  @Test
  @DisplayName("POST /api/package/detail 已下架套餐返回 420001")
  void detail_inactiveTemplate_notFound() throws Exception {
    PackageTemplate template = createActiveTemplate("标准 6 节", "standard", 6);
    template.setStatus("inactive");
    packageTemplateMapper.updateById(template);

    mockMvc
        .perform(
            post("/api/package/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"packageId\":%d}", template.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(420001));
  }

  @Test
  @DisplayName("POST /api/package/list 存在全局配置和公开教练时返回自定义套餐")
  void list_withCustomConfig_includesCustomPackage() throws Exception {
    createActiveTemplate("标准 6 节", "standard", 6);
    Coach coach = createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    createCustomConfig(1, 50);

    mockMvc
        .perform(
            post("/api/package/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":1,\"pageSize\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.items.length()").value(2))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.items[1].id").value(-1))
        .andExpect(jsonPath("$.data.items[1].packageMode").value("custom"))
        .andExpect(jsonPath("$.data.items[1].name").value("自定义课时"));
  }

  @Test
  @DisplayName("POST /api/package/detail 自定义套餐 ID 返回合成详情")
  void detail_customPackageId_returnsSyntheticDetail() throws Exception {
    Coach c1 = createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    Coach c2 = createCoach("李教练", CoachStatus.APPROVED.getValue(), new BigDecimal("180.00"));
    createCustomConfig(1, 50);

    mockMvc
        .perform(
            post("/api/package/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"packageId\":-1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").value(-1))
        .andExpect(jsonPath("$.data.packageMode").value("custom"))
        .andExpect(jsonPath("$.data.applicableCoaches.length()").value(2));
  }

  @Test
  @DisplayName("POST /api/package/detail 自定义套餐传入 coachId 仅返回选中教练")
  void detail_customPackageWithCoachId_returnsSelectedCoach() throws Exception {
    Coach c1 = createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    createCoach("李教练", CoachStatus.APPROVED.getValue(), new BigDecimal("180.00"));
    createCustomConfig(1, 50);

    mockMvc
        .perform(
            post("/api/package/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"packageId\":-1,\"coachId\":%d}", c1.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.applicableCoaches.length()").value(1))
        .andExpect(jsonPath("$.data.applicableCoaches[0].coachId").value(c1.getId().intValue()));
  }

  @Test
  @DisplayName("POST /api/package/detail synthetic id 与标准模板 id 不冲突")
  void detail_customSyntheticId_doesNotConflictWithTemplateId() throws Exception {
    Coach c1 = createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("200.00"));
    PackageTemplate template = createActiveTemplate("标准 6 节", "standard", 6);
    createCustomConfig(1, 50);
    linkTemplateToCoach(template, c1);

    mockMvc
        .perform(
            post("/api/package/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"packageId\":%d}", template.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").value(template.getId().intValue()))
        .andExpect(jsonPath("$.data.packageMode").value("standard"));

    mockMvc
        .perform(
            post("/api/package/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"packageId\":-1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").value(-1))
        .andExpect(jsonPath("$.data.packageMode").value("custom"));
  }

  private Coach createCoach(String name, int status, BigDecimal referencePrice) throws Exception {
    Coach coach = new Coach();
    coach.setOpenid(name + System.nanoTime());
    coach.setPhone(phoneEncryptor.encrypt("1380000" + String.format("%04d", (int) (Math.random() * 10000))));
    coach.setPhoneHash(
        phoneEncryptor.hash("1380000" + String.format("%04d", (int) (Math.random() * 10000))));
    coach.setName(name);
    coach.setStatus(status);
    coach.setReferencePrice(referencePrice);
    coach.setRating(new BigDecimal("4.9"));
    coach.setRealtimeStatus("空闲中");
    coach.setTeachingStrokes("蛙泳,自由泳");
    coachMapper.insert(coach);
    assertThat(coach.getId()).isNotNull();
    return coach;
  }

  private PackageTemplate createActiveTemplate(String name, String packageMode, int totalHours) {
    PackageTemplate template = new PackageTemplate();
    template.setName(name);
    template.setPackageMode(packageMode);
    template.setTeachingType("one_on_one");
    template.setStatus("active");
    template.setTotalHours(totalHours);
    template.setDurationMinutes(60);
    template.setValidDays(30);
    template.setOriginalPrice(new BigDecimal("1200.00"));
    template.setPrice(new BigDecimal("1080.00"));
    template.setRefundEnabled(false);
    template.setTags(List.of("popular"));
    template.setStrokeIds(List.of(1, 2));
    template.setImages(List.of("https://example.com/img.png"));
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

  private CustomPackageConfig createCustomConfig(int minHours, int maxHours) {
    CustomPackageConfig config = new CustomPackageConfig();
    config.setConfigKey("g" + System.nanoTime());
    config.setMinHours(minHours);
    config.setMaxHours(maxHours);
    config.setDefaultValidDays(30);
    customPackageConfigMapper.insert(config);
    assertThat(config.getId()).isNotNull();
    return config;
  }
}
