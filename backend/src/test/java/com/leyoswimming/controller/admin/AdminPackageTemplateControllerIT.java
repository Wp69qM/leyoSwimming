package com.leyoswimming.controller.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.repository.CoachMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPackageTemplateControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CoachMapper coachMapper;

  private String token;
  private Long coachId1;
  private Long coachId2;

  @BeforeEach
  void setUp() throws Exception {
    token = obtainAdminToken();
    coachId1 = createCoach("教练 A", new BigDecimal("300.00"));
    coachId2 = createCoach("教练 B", new BigDecimal("350.00"));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/list 无 token 返回 401")
  void list_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/package-template/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/list 带 token 返回分页列表")
  void list_withAdminToken_returnsPage() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/package-template/list")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":1,\"pageSize\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.items").isArray());
  }

  @Test
  @DisplayName("POST /api/admin/package-template/add 参数校验失败返回 100005")
  void add_invalidRequest_returnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/package-template/add")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(100005));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/add 创建标准套餐成功")
  void add_validStandardTemplate_returnsCreatedTemplate() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/package-template/add")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.packageTemplateId").isNumber())
        .andExpect(jsonPath("$.data.status").value("inactive"))
        .andExpect(jsonPath("$.data.coachIds[0]").value(coachId1.intValue()));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/detail 查询详情成功")
  void detail_existingTemplate_returnsDetails() throws Exception {
    Long templateId = createTemplate();

    mockMvc
        .perform(
            post("/api/admin/package-template/detail")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"packageTemplateId\":" + templateId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.packageTemplateId").value(templateId.intValue()))
        .andExpect(jsonPath("$.data.name").value("暑期 10 节课"));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/update 更新未上架套餐成功")
  void update_inactiveTemplate_returnsUpdatedTemplate() throws Exception {
    Long templateId = createTemplate();

    mockMvc
        .perform(
            post("/api/admin/package-template/update")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(templateId, 0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.name").value("暑期 12 节课"));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/toggle-status 上下架切换成功")
  void toggleStatus_inactiveTemplate_returnsActive() throws Exception {
    Long templateId = createTemplate();

    mockMvc
        .perform(
            post("/api/admin/package-template/toggle-status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"packageTemplateId\":" + templateId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("active"));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/custom-config 保存自定义配置成功")
  void customConfig_validRequest_returnsConfig() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/package-template/custom-config")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"minHours\":5,\"maxHours\":50,\"defaultValidDays\":180}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.minHours").value(5))
        .andExpect(jsonPath("$.data.maxHours").value(50));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/add 图片 URL 非法协议返回 100005")
  void add_invalidImageProtocol_returnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/package-template/add")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addRequestWithImage("ftp://example.com/a.jpg")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(100005));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/add description 中 script 标签被过滤")
  void add_descriptionWithScript_sanitizesDescription() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/package-template/add")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addRequestWithDescription("<p>暑期特惠</p><script>alert(1)</script>")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.description").value("<p>暑期特惠</p>"));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/update 图片 URL 非法协议返回 100005")
  void update_invalidImageProtocol_returnsValidationError() throws Exception {
    Long templateId = createTemplate();

    mockMvc
        .perform(
            post("/api/admin/package-template/update")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequestWithImage(templateId, 0, "ftp://example.com/a.jpg")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(100005));
  }

  @Test
  @DisplayName("POST /api/admin/package-template/update description 中 script 标签被过滤")
  void update_descriptionWithScript_sanitizesDescription() throws Exception {
    Long templateId = createTemplate();

    mockMvc
        .perform(
            post("/api/admin/package-template/update")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    updateRequestWithDescription(
                        templateId, 0, "<p>暑期特惠</p><script>alert(1)</script>")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.description").value("<p>暑期特惠</p>"));
  }

  @Test
  @DisplayName("POST /api/admin/image/upload 上传合法图片成功")
  void uploadImage_validPng_returnsUrl() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "files",
            "test.png",
            "image/png",
            new byte[] {
              (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00
            });

    mockMvc
        .perform(
            multipart("/api/admin/image/upload")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.urls[0]").isString());
  }

  @Test
  @DisplayName("POST /api/admin/image/upload 无 token 返回 401")
  void uploadImage_withoutToken_returnsUnauthorized() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "files",
            "test.png",
            "image/png",
            new byte[] {
              (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00
            });

    mockMvc
        .perform(multipart("/api/admin/image/upload").file(file))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  private String obtainAdminToken() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                      {"username":"admin","password":"admin123"}
                      """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }

  private Long createCoach(String name, BigDecimal referencePrice) {
    Coach coach = new Coach();
    coach.setOpenid("openid-" + name);
    coach.setPhone("1380000" + (int) (Math.random() * 10000));
    coach.setName(name);
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coach.setReferencePrice(referencePrice);
    coachMapper.insert(coach);
    return coach.getId();
  }

  private Long createTemplate() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/admin/package-template/add")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(addRequest()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .path("data")
        .path("packageTemplateId")
        .asLong();
  }

  private String addRequest() {
    return """
        {
          "name": "暑期 10 节课",
          "packageMode": "standard",
          "coachIds": [%d, %d],
          "teachingType": "one_on_one",
          "strokeIds": [1, 2],
          "totalHours": 10,
          "durationMinutes": 60,
          "validDays": 180,
          "originalPrice": 3600.00,
          "price": 3000.00,
          "refundEnabled": true,
          "refundRatio": 80.00,
          "refundValidDays": 30,
          "tags": ["热销"],
          "description": "<p>暑期特惠</p>",
          "images": ["https://cdn.example.com/a.jpg"]
        }
        """
        .formatted(coachId1, coachId2);
  }

  private String updateRequest(Long templateId, int version) {
    return """
        {
          "packageTemplateId": %d,
          "name": "暑期 12 节课",
          "coachIds": [%d],
          "totalHours": 12,
          "version": %d
        }
        """
        .formatted(templateId, coachId1, version);
  }

  private String updateRequestWithImage(Long templateId, int version, String imageUrl) {
    return """
        {
          "packageTemplateId": %d,
          "coachIds": [%d],
          "images": ["%s"],
          "version": %d
        }
        """
        .formatted(templateId, coachId1, imageUrl, version);
  }

  private String updateRequestWithDescription(Long templateId, int version, String description) {
    return """
        {
          "packageTemplateId": %d,
          "coachIds": [%d],
          "description": "%s",
          "version": %d
        }
        """
        .formatted(templateId, coachId1, description, version);
  }

  private String addRequestWithImage(String imageUrl) {
    return """
        {
          "name": "暑期 10 节课",
          "packageMode": "standard",
          "coachIds": [%d],
          "teachingType": "one_on_one",
          "totalHours": 10,
          "durationMinutes": 60,
          "validDays": 180,
          "originalPrice": 3600.00,
          "price": 3000.00,
          "refundEnabled": false,
          "images": ["%s"]
        }
        """
        .formatted(coachId1, imageUrl);
  }

  private String addRequestWithDescription(String description) {
    return """
        {
          "name": "暑期 10 节课",
          "packageMode": "standard",
          "coachIds": [%d],
          "teachingType": "one_on_one",
          "totalHours": 10,
          "durationMinutes": 60,
          "validDays": 180,
          "originalPrice": 3600.00,
          "price": 3000.00,
          "refundEnabled": false,
          "description": "%s"
        }
        """
        .formatted(coachId1, description);
  }
}
