package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.RecordConsentRequest;
import com.leyoswimming.dto.response.ConsentStatusResponse;
import com.leyoswimming.enums.ActorType;
import com.leyoswimming.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/consent")
@RequiredArgsConstructor
public class UserConsentController {

  private final PolicyService policyService;

  @PostMapping("/status")
  public ApiResponse<ConsentStatusResponse> status(@AuthenticationPrincipal Long userId) {
    return ApiResponse.ok(policyService.getConsentStatus(ActorType.user, userId));
  }

  @PostMapping("/record")
  public ApiResponse<Void> record(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody RecordConsentRequest request) {
    policyService.recordConsent(
        ActorType.user, userId, request.termsVersion(), request.privacyVersion());
    return ApiResponse.ok(null);
  }
}
