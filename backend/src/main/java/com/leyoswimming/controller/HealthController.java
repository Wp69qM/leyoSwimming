package com.leyoswimming.controller;

import com.leyoswimming.common.ApiResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/health")
  public ApiResponse<Map<String, String>> health() {
    return ApiResponse.ok(Map.of("status", "UP", "time", Instant.now().toString()));
  }
}
