package com.leyoswimming.controller.common;

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
class SmsControllerIT {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("POST /api/common/sms/send 发送验证码成功")
  void send_validRequest_returnsOk() throws Exception {
    mockMvc
        .perform(
            post("/api/common/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"phone":"13800138300","scene":"login","appType":"user"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  @DisplayName("POST /api/common/sms/send 60 秒内重复发送触发限流")
  void send_twiceQuickly_returnsRateLimitError() throws Exception {
    String phone = "13800138301";

    mockMvc
        .perform(
            post("/api/common/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        """
                        {"phone":"%s","scene":"login","appType":"user"}
                        """,
                        phone)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc
        .perform(
            post("/api/common/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        """
                        {"phone":"%s","scene":"login","appType":"user"}
                        """,
                        phone)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(420002))
        .andExpect(jsonPath("$.message").value("请 60 秒后再试"));
  }

  @Test
  @DisplayName("POST /api/common/sms/send 非法手机号返回校验错误")
  void send_invalidPhone_returnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/common/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"phone":"12345678901","scene":"login","appType":"user"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(100005));
  }
}
