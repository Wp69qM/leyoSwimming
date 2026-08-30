package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.ai.gateway.AiSessionCreateResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionDetailRequest;
import com.leyoswimming.dto.ai.gateway.AiSessionDetailResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionListRequest;
import com.leyoswimming.entity.AiChatMessage;
import com.leyoswimming.entity.AiChatSession;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AiChatMessageMapper;
import com.leyoswimming.repository.AiChatSessionMapper;
import com.leyoswimming.repository.AiRecommendationLogMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class AiAssistantGatewayServiceTest {

  @Mock private AiChatSessionMapper sessionMapper;
  @Mock private AiChatMessageMapper messageMapper;
  @Mock private AiRecommendationLogMapper recommendationLogMapper;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private IdempotencyHelper idempotencyHelper;
  @InjectMocks private AiAssistantGatewayService service;

  @BeforeEach
  void setUp() {
    // @Value 字段与 RestClient 在单元测试中通过反射注入
  }

  @Test
  @DisplayName("createSession: 成功时保存会话与欢迎消息")
  void createSession_success_savesSessionAndWelcomeMessage() {
    AiSessionCreateResponse aiResponse = new AiSessionCreateResponse("session-123", "欢迎咨询", List.of("推荐教练", "选套餐"));
    givenAiServiceReturns(aiResponse);

    Long userId = 42L;
    service.createSession(userId);

    ArgumentCaptor<AiChatSession> sessionCaptor = ArgumentCaptor.forClass(AiChatSession.class);
    verify(sessionMapper).insert(sessionCaptor.capture());
    AiChatSession savedSession = sessionCaptor.getValue();
    assertThat(savedSession.getSessionId()).isEqualTo("session-123");
    assertThat(savedSession.getUserId()).isEqualTo(userId);
    assertThat(savedSession.getStatus()).isZero();

    ArgumentCaptor<AiChatMessage> messageCaptor = ArgumentCaptor.forClass(AiChatMessage.class);
    verify(messageMapper).insert(messageCaptor.capture());
    AiChatMessage savedMessage = messageCaptor.getValue();
    assertThat(savedMessage.getSessionId()).isEqualTo("session-123");
    assertThat(savedMessage.getRole()).isEqualTo("assistant");
    assertThat(savedMessage.getContent()).isEqualTo("欢迎咨询");
    assertThat(savedMessage.getMessageId()).startsWith("msg_");
  }

  @Test
  @DisplayName("listSessions: 无 userId 抛 UNAUTHORIZED")
  void listSessions_withoutUserId_throwsUnauthorized() {
    AiSessionListRequest request = new AiSessionListRequest(1, 20);

    assertThatThrownBy(() -> service.listSessions(null, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHORIZED));
  }

  @Test
  @DisplayName("getSessionDetail: 校验会话归属，非本人抛 AI_SESSION_NOT_FOUND")
  void getSessionDetail_sessionNotOwned_throwsSessionNotFound() {
    AiChatSession session = new AiChatSession();
    session.setSessionId("session-123");
    session.setUserId(1L);
    when(sessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);

    AiSessionDetailRequest request = new AiSessionDetailRequest("session-123");

    assertThatThrownBy(() -> service.getSessionDetail(2L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.AI_SESSION_NOT_FOUND));
  }

  @Test
  @DisplayName("getSessionDetail: 本人会话返回消息列表")
  void getSessionDetail_sessionOwned_returnsMessages() {
    AiChatSession session = new AiChatSession();
    session.setSessionId("session-123");
    session.setUserId(1L);
    when(sessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);

    AiChatMessage message = new AiChatMessage();
    message.setSessionId("session-123");
    message.setRole("user");
    message.setContent("你好");
    when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(message));

    AiSessionDetailResponse response = service.getSessionDetail(1L, new AiSessionDetailRequest("session-123"));

    assertThat(response.sessionId()).isEqualTo("session-123");
    assertThat(response.messages()).hasSize(1);
    assertThat(response.messages().get(0).role()).isEqualTo("user");
    assertThat(response.messages().get(0).content()).isEqualTo("你好");
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void givenAiServiceReturns(Object response) {
    RestClient restClient = mock(RestClient.class);
    RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

    lenient().when(restClient.post()).thenReturn(uriSpec);
    lenient().when(uriSpec.uri(anyString())).thenReturn(bodySpec);
    lenient().when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
    lenient().when(bodySpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    lenient().when(responseSpec.body(any(Class.class))).thenAnswer(inv -> response);

    ReflectionTestUtils.setField(service, "restClient", restClient);
  }
}
