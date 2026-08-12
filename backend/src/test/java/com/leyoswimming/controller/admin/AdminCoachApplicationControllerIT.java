package com.leyoswimming.controller.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AdminCoachApplicationControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("POST /api/admin/coach/application/list 管理员查询入驻列表")
  void list_withAdminToken_returnsApplications() throws Exception {
    String token = obtainAdminToken();

    mockMvc
        .perform(
            post("/api/admin/coach/application/list")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"pending","page":1,"pageSize":10}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.list").isArray());
  }

  @Test
  @DisplayName("POST /api/admin/coach/application/list 无 token 返回 401")
  void list_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/coach/application/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  @Test
  @DisplayName("POST /api/admin/coach/application/approve 审批不存在的申请返回 404")
  void approve_notFound_returnsResourceNotFound() throws Exception {
    String token = obtainAdminToken();

    mockMvc
        .perform(
            post("/api/admin/coach/application/approve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"applicationId":999999,"remark":"通过"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(100004));
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
    JsonNode data =
        objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }
}
