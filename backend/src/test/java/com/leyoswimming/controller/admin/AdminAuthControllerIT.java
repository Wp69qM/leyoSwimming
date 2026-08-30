package com.leyoswimming.controller.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AdminAuthControllerIT {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("POST /api/admin/auth/login 正确的用户名密码登录成功")
  void login_validCredentials_returnsToken() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"username":"admin","password":"admin123"}
                  """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.token").isString())
        .andExpect(jsonPath("$.data.expiresIn").value(86400))
        .andExpect(jsonPath("$.data.admin.username").value("admin"));
  }

  @Test
  @DisplayName("POST /api/admin/auth/login 错误的密码返回业务错误 200001")
  void login_invalidPassword_returnsBusinessError() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"username":"admin","password":"wrong_password"}
                  """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200001))
        .andExpect(jsonPath("$.message").value("用户名或密码错误"));
  }

  @Test
  @DisplayName("POST /api/admin/auth/login 被禁用的账号返回业务错误 300002")
  void login_disabledAdmin_returnsBusinessError() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"username":"disabled_admin","password":"admin123"}
                  """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(300002))
        .andExpect(jsonPath("$.message").value("账号已被禁用，请联系超级管理员"));
  }
}
