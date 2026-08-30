package com.leyoswimming.controller.admin;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.AdminImageUploadResponse;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.service.AdminPermissionHelper;
import com.leyoswimming.service.FileStorageService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/image")
@RequiredArgsConstructor
public class AdminImageController {

  private static final int MAX_UPLOAD_COUNT = 10;

  private final FileStorageService fileStorageService;
  private final AdminPermissionHelper permissionHelper;

  @PostMapping("/upload")
  public ApiResponse<AdminImageUploadResponse> upload(
      @AuthenticationPrincipal Long adminId, @RequestParam("files") MultipartFile[] files) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_WRITE);

    if (files == null || files.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传图片");
    }
    if (files.length > MAX_UPLOAD_COUNT) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "单次最多上传 10 张图片");
    }

    List<String> urls = new ArrayList<>(files.length);
    for (MultipartFile file : files) {
      urls.add(fileStorageService.store(file));
    }
    return ApiResponse.ok(new AdminImageUploadResponse(urls));
  }
}
