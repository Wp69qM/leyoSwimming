package com.leyoswimming.controller.internal;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.ai.internal.InternalCoachItemResponse;
import com.leyoswimming.dto.ai.internal.InternalCoachQueryRequest;
import com.leyoswimming.dto.ai.internal.InternalHotRecommendationsRequest;
import com.leyoswimming.dto.ai.internal.InternalListResponse;
import com.leyoswimming.dto.ai.internal.InternalPackageItemResponse;
import com.leyoswimming.dto.ai.internal.InternalPackageQueryRequest;
import com.leyoswimming.dto.ai.internal.InternalUserPackageItemResponse;
import com.leyoswimming.dto.ai.internal.InternalUserPackagesRequest;
import com.leyoswimming.dto.ai.internal.InternalUserProfileRequest;
import com.leyoswimming.dto.ai.internal.InternalUserProfileResponse;
import com.leyoswimming.service.InternalAiDataService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/ai")
@RequiredArgsConstructor
public class InternalAiController {

  private final InternalAiDataService internalAiDataService;

  @PostMapping("/coaches/query")
  public ApiResponse<InternalListResponse<InternalCoachItemResponse>> queryCoaches(
      @Valid @RequestBody InternalCoachQueryRequest request) {
    List<InternalCoachItemResponse> data =
        internalAiDataService.queryCoaches(
            request.stroke(),
            request.gender(),
            request.minPrice(),
            request.maxPrice(),
            request.classSize(),
            request.limit());
    return ApiResponse.ok(new InternalListResponse<>(data));
  }

  @PostMapping("/packages/query")
  public ApiResponse<InternalListResponse<InternalPackageItemResponse>> queryPackages(
      @Valid @RequestBody InternalPackageQueryRequest request) {
    List<InternalPackageItemResponse> data =
        internalAiDataService.queryPackages(
            request.stroke(),
            request.packageMode(),
            request.minPrice(),
            request.maxPrice(),
            request.hours(),
            request.limit());
    return ApiResponse.ok(new InternalListResponse<>(data));
  }

  @PostMapping("/user/profile")
  public ApiResponse<InternalUserProfileResponse> userProfile(
      @Valid @RequestBody InternalUserProfileRequest request) {
    InternalUserProfileResponse data = internalAiDataService.queryUserProfile(request);
    return ApiResponse.ok(data);
  }

  @PostMapping("/user/packages")
  public ApiResponse<InternalListResponse<InternalUserPackageItemResponse>> userPackages(
      @Valid @RequestBody InternalUserPackagesRequest request) {
    List<InternalUserPackageItemResponse> data =
        internalAiDataService.queryUserPackages(request);
    return ApiResponse.ok(new InternalListResponse<>(data));
  }

  @PostMapping("/recommendations/hot")
  public ApiResponse<InternalListResponse<Object>> hotRecommendations(
      @Valid @RequestBody InternalHotRecommendationsRequest request) {
    List<Object> data = internalAiDataService.queryHotRecommendations(request);
    return ApiResponse.ok(new InternalListResponse<>(data));
  }
}
