package com.leyoswimming.controller.common;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/common/file")
@RequiredArgsConstructor
public class CommonFileController {

  private final FileStorageService fileStorageService;

  @PostMapping("/upload")
  public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(fileStorageService.store(file));
  }
}
