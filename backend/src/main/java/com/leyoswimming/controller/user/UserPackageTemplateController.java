package com.leyoswimming.controller.user;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.PackageDetailRequest;
import com.leyoswimming.dto.request.PackageListRequest;
import com.leyoswimming.dto.response.PackageDetailResponse;
import com.leyoswimming.dto.response.PackageListResponse;
import com.leyoswimming.dto.response.UserPackageTemplateCustomConfigResponse;
import com.leyoswimming.service.UserPackageTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/package")
@RequiredArgsConstructor
public class UserPackageTemplateController {

  private final UserPackageTemplateService userPackageTemplateService;

  @PostMapping("/list")
  public ApiResponse<PackageListResponse> list(@Valid @RequestBody PackageListRequest request) {
    return ApiResponse.ok(userPackageTemplateService.list(request));
  }

  @PostMapping("/detail")
  public ApiResponse<PackageDetailResponse> detail(
      @Valid @RequestBody PackageDetailRequest request) {
    return ApiResponse.ok(userPackageTemplateService.detail(request));
  }

  @PostMapping("/custom-config")
  public ApiResponse<UserPackageTemplateCustomConfigResponse> customConfig() {
    return ApiResponse.ok(userPackageTemplateService.customConfig());
  }
}
