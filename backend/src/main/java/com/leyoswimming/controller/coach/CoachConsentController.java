package com.leyoswimming.controller.coach;

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
@RequestMapping("/api/coach/consent")
@RequiredArgsConstructor
public class CoachConsentController {

  private final PolicyService policyService;

  @PostMapping("/status")
  public ApiResponse<ConsentStatusResponse> status(@AuthenticationPrincipal Long coachId) {
    return ApiResponse.ok(policyService.getConsentStatus(ActorType.coach, coachId));
  }

  @PostMapping("/record")
  public ApiResponse<Void> record(
      @AuthenticationPrincipal Long coachId, @Valid @RequestBody RecordConsentRequest request) {
    policyService.recordConsent(
        ActorType.coach, coachId, request.termsVersion(), request.privacyVersion());
    return ApiResponse.ok(null);
  }
}
