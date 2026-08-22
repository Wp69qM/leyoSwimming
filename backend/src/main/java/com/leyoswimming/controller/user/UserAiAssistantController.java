package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.ai.gateway.AiChatRequest;
import com.leyoswimming.dto.ai.gateway.AiChatResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionCreateResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionDetailRequest;
import com.leyoswimming.dto.ai.gateway.AiSessionDetailResponse;
import com.leyoswimming.dto.ai.gateway.AiSessionListRequest;
import com.leyoswimming.dto.ai.gateway.AiSessionListResponse;
import com.leyoswimming.service.AiAssistantGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-assistant")
@RequiredArgsConstructor
public class UserAiAssistantController {

  private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

  private final AiAssistantGatewayService gatewayService;

  @PostMapping("/chat")
  public ApiResponse<AiChatResponse> chat(
      @AuthenticationPrincipal Long userId,
      @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
      @Valid @RequestBody AiChatRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        gatewayService.chat(userId, resolveClientIp(httpRequest), request, idempotencyKey));
  }

  @PostMapping("/session/create")
  public ApiResponse<AiSessionCreateResponse> createSession(
      @AuthenticationPrincipal Long userId) {
    return ApiResponse.ok(gatewayService.createSession(userId));
  }

  @PostMapping("/session/list")
  public ApiResponse<AiSessionListResponse> listSessions(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody(required = false) AiSessionListRequest request) {
    AiSessionListRequest effectiveRequest = request != null ? request : new AiSessionListRequest(1, 20);
    return ApiResponse.ok(gatewayService.listSessions(userId, effectiveRequest));
  }

  @PostMapping("/session/detail")
  public ApiResponse<AiSessionDetailResponse> sessionDetail(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody AiSessionDetailRequest request) {
    return ApiResponse.ok(gatewayService.getSessionDetail(userId, request));
  }

  private String resolveClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isBlank()) {
      return ip.split(",")[0].trim();
    }
    ip = request.getHeader("X-Real-IP");
    if (ip != null && !ip.isBlank()) {
      return ip.trim();
    }
    return Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
  }
}
