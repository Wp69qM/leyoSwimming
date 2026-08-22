package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.ai.gateway.AiChatRequest;
import com.leyoswimming.dto.ai.gateway.AiChatResponse;
import com.leyoswimming.dto.ai.gateway.AiMessageResponse;
import com.leyoswimming.dto.ai.gateway.AiReplyResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionCreateResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionDetailRequest;
import com.leyoswimming.dto.ai.gateway.AiSessionDetailResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionListItemResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionListRequest;
import com.leyoswimming.dto.ai.gateway.AiSessionListResponse;
import com.leyoswimming.entity.AiChatMessage;
import com.leyoswimming.entity.AiChatSession;
import com.leyoswimming.entity.AiRecommendationLog;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AiChatMessageMapper;
import com.leyoswimming.repository.AiChatSessionMapper;
import com.leyoswimming.repository.AiRecommendationLogMapper;
import com.leyoswimming.util.AiUserHashUtil;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantGatewayService {

  private static final int RATE_LIMIT_USER_PER_MINUTE = 30;
  private static final int RATE_LIMIT_IP_PER_MINUTE = 60;
  private static final int RATE_LIMIT_WINDOW_SECONDS = 60;
  private static final String RATE_PREFIX = "ai:rate:";
  private static final int TITLE_MAX_LENGTH = 20;
  private static final int MESSAGE_MAX_LENGTH = 500;

  private final AiChatSessionMapper sessionMapper;
  private final AiChatMessageMapper messageMapper;
  private final AiRecommendationLogMapper recommendationLogMapper;
  private final StringRedisTemplate redisTemplate;
  private final IdempotencyHelper idempotencyHelper;

  @Value("${leyo.ai-service.base-url}")
  private String aiServiceBaseUrl;

  @Value("${leyo.ai-service.internal-api-token}")
  private String internalApiToken;

  private RestClient restClient;

  @PostConstruct
  void init() {
    log.info(
        "AI service client init: baseUrl={}, tokenLength={}",
        aiServiceBaseUrl,
        internalApiToken != null ? internalApiToken.length() : 0);
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(30));

    this.restClient =
        RestClient.builder()
            .baseUrl(aiServiceBaseUrl)
            .requestFactory(requestFactory)
            .defaultHeader("X-Internal-Token", internalApiToken)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Connection", "close")
            .build();
  }

  public AiChatResponse chat(Long userId, String clientIp, AiChatRequest request, String idempotencyKey) {
    checkRateLimit(userId, clientIp);

    boolean idempotencyEnabled = StringUtils.isNotBlank(idempotencyKey);
    if (idempotencyEnabled) {
      idempotencyHelper.checkAndLock("ai:user", userId != null ? userId : 0L, idempotencyKey);
    }

    try {
      String userHash = AiUserHashUtil.hash(userId);
      String sessionId = request.sessionId();
      ensureSessionExists(userId, sessionId);
      saveUserMessage(sessionId, request.message());

      Map<String, Object> body = new java.util.HashMap<>();
      body.put("session_id", sessionId);
      body.put("message", request.message());
      if (userHash != null) {
        body.put("user_hash", userHash);
      }

      long start = System.currentTimeMillis();
      AiChatResponse data = postToAiService("/api/ai-assistant/chat", body, AiChatResponse.class);
      long latency = System.currentTimeMillis() - start;

      if (data == null) {
        throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
      }
      saveAssistantMessage(data);
      saveRecommendationLog(userId, data, request.message(), latency);
      updateSessionTitle(userId, sessionId, request.message());

      return data;
    } catch (Exception e) {
      if (idempotencyEnabled) {
        idempotencyHelper.unlock("ai:user", userId != null ? userId : 0L, idempotencyKey);
      }
      if (e instanceof BusinessException) {
        throw e;
      }
      log.error("AI chat failed", e);
      throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
    }
  }

  public AiSessionCreateResponse createSession(Long userId) {
    AiSessionCreateResponse data =
        postToAiService("/api/ai-assistant/session/create", Map.of(), AiSessionCreateResponse.class);
    if (data == null) {
      throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
    }

    AiChatSession session = new AiChatSession();
    session.setSessionId(data.sessionId());
    session.setUserId(userId);
    session.setStatus(0);
    sessionMapper.insert(session);

    AiChatMessage welcome = new AiChatMessage();
    welcome.setMessageId("msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    welcome.setSessionId(data.sessionId());
    welcome.setRole("assistant");
    welcome.setContent(data.welcomeMessage());
    messageMapper.insert(welcome);

    return data;
  }

  public AiSessionListResponse listSessions(Long userId, AiSessionListRequest request) {
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    int page = request.page() == null || request.page() < 1 ? 1 : request.page();
    int size = request.size() == null || request.size() < 1 ? 20 : request.size();

    LambdaQueryWrapper<AiChatSession> wrapper =
        new LambdaQueryWrapper<AiChatSession>()
            .eq(AiChatSession::getUserId, userId)
            .eq(AiChatSession::getStatus, 0)
            .orderByDesc(AiChatSession::getUpdatedAt);

    Page<AiChatSession> pageParam = new Page<>(page, size);
    Page<AiChatSession> result = sessionMapper.selectPage(pageParam, wrapper);

    List<AiSessionListItemResponse> items =
        result.getRecords().stream()
            .map(
                session -> {
                  Long count =
                      messageMapper.selectCount(
                          new LambdaQueryWrapper<AiChatMessage>()
                              .eq(AiChatMessage::getSessionId, session.getSessionId()));
                  LocalDateTime lastMessageAt =
                      messageMapper
                          .selectList(
                              new LambdaQueryWrapper<AiChatMessage>()
                                  .eq(AiChatMessage::getSessionId, session.getSessionId())
                                  .orderByDesc(AiChatMessage::getCreatedAt)
                                  .last("LIMIT 1"))
                          .stream()
                          .findFirst()
                          .map(AiChatMessage::getCreatedAt)
                          .orElse(session.getUpdatedAt());
                  return new AiSessionListItemResponse(
                      session.getSessionId(), session.getTitle(), lastMessageAt, count);
                })
            .toList();

    return new AiSessionListResponse(
        items, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
  }

  public AiSessionDetailResponse getSessionDetail(Long userId, AiSessionDetailRequest request) {
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    AiChatSession session = findSession(request.sessionId());
    if (!Objects.equals(session.getUserId(), userId)) {
      throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND);
    }

    List<AiChatMessage> messages =
        messageMapper.selectList(
            new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, session.getSessionId())
                .orderByAsc(AiChatMessage::getCreatedAt));

    List<AiMessageResponse> messageResponses =
        messages.stream()
            .map(m -> new AiMessageResponse(m.getRole(), m.getContent(), m.getCreatedAt()))
            .toList();

    return new AiSessionDetailResponse(session.getSessionId(), messageResponses);
  }

  private void ensureSessionExists(Long userId, String sessionId) {
    AiChatSession session = findSession(sessionId);
    if (session == null) {
      session = new AiChatSession();
      session.setSessionId(sessionId);
      session.setUserId(userId);
      session.setStatus(0);
      sessionMapper.insert(session);
      return;
    }
    if (userId != null && !Objects.equals(session.getUserId(), userId)) {
      throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND);
    }
  }

  private AiChatSession findSession(String sessionId) {
    return sessionMapper.selectOne(
        new LambdaQueryWrapper<AiChatSession>().eq(AiChatSession::getSessionId, sessionId));
  }

  private void saveUserMessage(String sessionId, String message) {
    AiChatMessage msg = new AiChatMessage();
    msg.setMessageId("msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    msg.setSessionId(sessionId);
    msg.setRole("user");
    msg.setContent(StringUtils.abbreviate(message, MESSAGE_MAX_LENGTH));
    messageMapper.insert(msg);
  }

  private void saveAssistantMessage(AiChatResponse response) {
    AiChatMessage msg = new AiChatMessage();
    msg.setMessageId(response.messageId());
    msg.setSessionId(response.sessionId());
    msg.setRole("assistant");
    AiReplyResponse reply = response.reply();
    if (reply != null) {
      msg.setContent(reply.text());
      msg.setRecommendations(reply.recommendations());
    }
    messageMapper.insert(msg);
  }

  private void saveRecommendationLog(Long userId, AiChatResponse response, String input, long latencyMs) {
    AiRecommendationLog logEntity = new AiRecommendationLog();
    logEntity.setSessionId(response.sessionId());
    logEntity.setMessageId(response.messageId());
    logEntity.setUserId(userId);
    logEntity.setInput(input);
    logEntity.setFinalResponse(response.reply());
    logEntity.setLatencyMs((int) latencyMs);
    recommendationLogMapper.insert(logEntity);
  }

  private void updateSessionTitle(Long userId, String sessionId, String message) {
    AiChatSession session = findSession(sessionId);
    if (session == null || StringUtils.isNotBlank(session.getTitle())) {
      return;
    }
    String title = StringUtils.abbreviate(message.replaceAll("\\s+", " "), TITLE_MAX_LENGTH);
    session.setTitle(title);
    sessionMapper.updateById(session);
  }

  private void checkRateLimit(Long userId, String clientIp) {
    String userKey = buildRateLimitKey("user", userId != null ? String.valueOf(userId) : clientIp);
    if (exceedsLimit(userKey, RATE_LIMIT_USER_PER_MINUTE)) {
      throw new BusinessException(ErrorCode.AI_RATE_LIMITED);
    }

    String ipKey = buildRateLimitKey("ip", clientIp);
    if (exceedsLimit(ipKey, RATE_LIMIT_IP_PER_MINUTE)) {
      throw new BusinessException(ErrorCode.AI_RATE_LIMITED);
    }
  }

  private String buildRateLimitKey(String type, String value) {
    return RATE_PREFIX + type + ":" + value + ":" + (System.currentTimeMillis() / 1000 / RATE_LIMIT_WINDOW_SECONDS);
  }

  private boolean exceedsLimit(String key, int limit) {
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1) {
      redisTemplate.opsForValue().getOperations().expire(key, Duration.ofSeconds(RATE_LIMIT_WINDOW_SECONDS));
    }
    return count != null && count > limit;
  }

  private <T> T postToAiService(String path, Object body, Class<T> responseType) {
    try {
      return restClient
          .post()
          .uri(path)
          .body(body)
          .retrieve()
          .onStatus(
              HttpStatusCode::isError,
              (clientRequest, clientResponse) -> {
                String responseBody = "";
                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(
                            clientResponse.getBody(), StandardCharsets.UTF_8))) {
                  responseBody = reader.lines().collect(Collectors.joining("\n"));
                } catch (Exception ignored) {
                }
                log.error(
                    "AI service returned non-2xx status: {} url={} response={}",
                    clientResponse.getStatusCode(),
                    aiServiceBaseUrl + path,
                    responseBody);
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
              })
          .body(responseType);
    } catch (RestClientResponseException e) {
      log.error("AI service request failed: {}", e.getMessage());
      throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
    }
  }
}
