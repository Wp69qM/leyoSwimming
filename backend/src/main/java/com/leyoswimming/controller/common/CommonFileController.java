package com.leyoswimming.controller.common;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/common/file")
@RequiredArgsConstructor
public class CommonFileController {

  private final FileStorageService fileStorageService;

  @PostMapping("/upload")
  public ApiResponse<String> upload(
      @RequestParam("file") MultipartFile file, HttpServletRequest request) {
    String path = fileStorageService.store(file);
    String url = resolveAbsoluteUrl(path, request);
    return ApiResponse.ok(url);
  }

  private String resolveAbsoluteUrl(String path, HttpServletRequest request) {
    if (path == null || path.isBlank() || path.startsWith("http://") || path.startsWith("https://")) {
      return path;
    }
    String normalizedPath = path.startsWith("/") ? path : "/" + path;
    return ServletUriComponentsBuilder.fromRequestUri(request)
        .replacePath(normalizedPath)
        .toUriString();
  }
}
