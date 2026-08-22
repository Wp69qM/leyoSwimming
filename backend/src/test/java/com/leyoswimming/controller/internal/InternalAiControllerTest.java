package com.leyoswimming.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.enums.PackageMode;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InternalAiControllerTest {

  private static final String INTERNAL_TOKEN = "test-internal-token";
  private static final String TOKEN_HEADER = "X-Internal-Token";

  @Autowired private MockMvc mockMvc;
  @Autowired private CoachMapper coachMapper;
  @Autowired private PackageTemplateMapper packageTemplateMapper;

  @Test
  @DisplayName("POST /api/internal/ai/coaches/query 无 Token 返回 401")
  void coachesQuery_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/internal/ai/coaches/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401))
        .andExpect(jsonPath("$.message").value("未授权的内部访问"));
  }

  @Test
  @DisplayName("POST /api/internal/ai/coaches/query 错误 Token 返回 401")
  void coachesQuery_withWrongToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/internal/ai/coaches/query")
                .header(TOKEN_HEADER, "wrong-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  @DisplayName("POST /api/internal/ai/coaches/query 正确 Token 返回教练业务数据")
  void coachesQuery_withValidToken_returnsBusinessData() throws Exception {
    Coach coach = createCoach("王教练", CoachStatus.APPROVED.getValue(), new BigDecimal("4.8"));

    mockMvc
        .perform(
            post("/api/internal/ai/coaches/query")
                .header(TOKEN_HEADER, INTERNAL_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.data").isArray())
        .andExpect(jsonPath("$.data.data.length()").value(1))
        .andExpect(jsonPath("$.data.data[0].coachId").value(coach.getId()))
        .andExpect(jsonPath("$.data.data[0].name").value("王教练"))
        .andExpect(jsonPath("$.data.data[0].gender").doesNotExist());
  }

  @Test
  @DisplayName("POST /api/internal/ai/packages/query 无 Token 返回 401")
  void packagesQuery_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/internal/ai/packages/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401))
        .andExpect(jsonPath("$.message").value("未授权的内部访问"));
  }

  @Test
  @DisplayName("POST /api/internal/ai/packages/query 错误 Token 返回 401")
  void packagesQuery_withWrongToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/internal/ai/packages/query")
                .header(TOKEN_HEADER, "wrong-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  @DisplayName("POST /api/internal/ai/packages/query 正确 Token 返回套餐业务数据")
  void packagesQuery_withValidToken_returnsBusinessData() throws Exception {
    PackageTemplate template = createActiveTemplate("标准 6 节", PackageMode.STANDARD.getValue(), 6);

    mockMvc
        .perform(
            post("/api/internal/ai/packages/query")
                .header(TOKEN_HEADER, INTERNAL_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.data").isArray())
        .andExpect(jsonPath("$.data.data.length()").value(1))
        .andExpect(jsonPath("$.data.data[0].packageId").value(template.getId()))
        .andExpect(jsonPath("$.data.data[0].name").value("标准 6 节"))
        .andExpect(jsonPath("$.data.data[0].packageMode").value(PackageMode.STANDARD.getValue()));
  }

  private Coach createCoach(String name, int status, BigDecimal rating) {
    Coach coach = new Coach();
    coach.setOpenid("openid-" + name + System.nanoTime());
    coach.setPhone("1380000" + String.format("%04d", (int) (Math.random() * 10000)));
    coach.setName(name);
    coach.setStatus(status);
    coach.setRating(rating);
    coach.setReferencePrice(new BigDecimal("200.00"));
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
    template.setTotalHours(totalHours);
    template.setDurationMinutes(60);
    template.setValidDays(30);
    template.setOriginalPrice(new BigDecimal("1200.00"));
    template.setPrice(new BigDecimal("1080.00"));
    template.setRefundEnabled(false);
    template.setStatus(PackageTemplateStatus.ACTIVE.getValue());
    packageTemplateMapper.insert(template);
    assertThat(template.getId()).isNotNull();
    return template;
  }
}
