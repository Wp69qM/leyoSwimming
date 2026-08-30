package com.leyoswimming.controller.common;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.response.CurrentPolicyResponse;
import com.leyoswimming.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common/policy")
@RequiredArgsConstructor
public class CommonPolicyController {

  private final PolicyService policyService;

  @PostMapping("/current/terms")
  public ApiResponse<CurrentPolicyResponse> currentTerms() {
    return ApiResponse.ok(policyService.getCurrentTerms());
  }

  @PostMapping("/current/privacy")
  public ApiResponse<CurrentPolicyResponse> currentPrivacy() {
    return ApiResponse.ok(policyService.getCurrentPrivacy());
  }
}
