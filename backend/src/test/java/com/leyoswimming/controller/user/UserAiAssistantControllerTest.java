package com.leyoswimming.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leyoswimming.dto.ai.gateway.AiChatResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionCreateResponse;
import com.leyoswimming.service.AiAssistantGatewayService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAiAssistantControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private AiAssistantGatewayService gatewayService;

  @Test
  @DisplayName("POST /api/ai-assistant/session/create 游客可访问")
  void createSession_anonymous_returnsOk() throws Exception {
    when(gatewayService.createSession(null))
        .thenReturn(new AiSessionCreateResponse("session-123", "欢迎咨询", List.of("推荐教练", "选套餐")));

    mockMvc
        .perform(
            post("/api/ai-assistant/session/create")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.sessionId").value("session-123"))
        .andExpect(jsonPath("$.data.welcomeMessage").value("欢迎咨询"));
  }

  @Test
  @DisplayName("POST /api/ai-assistant/session/list 无 JWT 返回 401")
  void listSessions_withoutJwt_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/ai-assistant/session/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(200001));
  }

  @Test
  @DisplayName("POST /api/ai-assistant/chat 正常返回")
  void chat_anonymous_returnsOk() throws Exception {
    AiChatResponse response = new AiChatResponse("session-123", "msg-456", null);
    when(gatewayService.chat(eq(null), any(), any(), eq(null))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/ai-assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":\"session-123\",\"message\":\"你好\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.sessionId").value("session-123"))
        .andExpect(jsonPath("$.data.messageId").value("msg-456"));
  }
}
