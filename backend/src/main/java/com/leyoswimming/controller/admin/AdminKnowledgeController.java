package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.KnowledgeAddRequest;
import com.leyoswimming.dto.request.KnowledgeDeleteRequest;
import com.leyoswimming.dto.request.KnowledgeDetailRequest;
import com.leyoswimming.dto.request.KnowledgeListRequest;
import com.leyoswimming.dto.request.KnowledgeToggleRequest;
import com.leyoswimming.dto.request.KnowledgeUploadRequest;
import com.leyoswimming.dto.response.DownloadFile;
import com.leyoswimming.dto.response.KnowledgeDetailResponse;
import com.leyoswimming.dto.response.KnowledgeListItemResponse;
import com.leyoswimming.dto.response.KnowledgeListResponse;
import com.leyoswimming.service.AdminKnowledgeService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {

  private final AdminKnowledgeService adminKnowledgeService;

  @PostMapping("/list")
  public ApiResponse<KnowledgeListResponse> list(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid KnowledgeListRequest request) {
    return ApiResponse.ok(adminKnowledgeService.list(adminId, request));
  }

  @PostMapping("/add")
  public ApiResponse<KnowledgeListItemResponse> add(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid KnowledgeAddRequest request) {
    return ApiResponse.ok(adminKnowledgeService.add(adminId, request));
  }

  @PostMapping("/upload")
  public ApiResponse<KnowledgeListItemResponse> upload(
      @AuthenticationPrincipal Long adminId,
      @Valid @ModelAttribute KnowledgeUploadRequest request) {
    return ApiResponse.ok(adminKnowledgeService.upload(adminId, request));
  }

  @PostMapping("/toggle")
  public ApiResponse<KnowledgeListItemResponse> toggle(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid KnowledgeToggleRequest request) {
    return ApiResponse.ok(adminKnowledgeService.toggle(adminId, request));
  }

  @PostMapping("/delete")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid KnowledgeDeleteRequest request) {
    adminKnowledgeService.delete(adminId, request);
    return ApiResponse.ok();
  }

  @PostMapping("/detail")
  public ApiResponse<KnowledgeDetailResponse> detail(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid KnowledgeDetailRequest request) {
    return ApiResponse.ok(adminKnowledgeService.detail(adminId, request.documentId()));
  }

  @PostMapping("/download")
  public ResponseEntity<InputStreamResource> download(
      @AuthenticationPrincipal Long adminId,
      @RequestBody @Valid KnowledgeDetailRequest request) {
    DownloadFile downloadFile = adminKnowledgeService.download(adminId, request.documentId());
    String filename = downloadFile.filename();
    String encodedFilename =
        URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    String asciiFilename = filename.replaceAll("[^\\x00-\\x7F]", "_");
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + asciiFilename + "\"; filename*=UTF-8''" + encodedFilename);
    headers.setContentType(MediaType.parseMediaType(downloadFile.contentType()));
    headers.setContentLength(downloadFile.contentLength());
    InputStreamResource resource = new InputStreamResource(downloadFile.inputStream());
    return ResponseEntity.ok().headers(headers).body(resource);
  }
}
