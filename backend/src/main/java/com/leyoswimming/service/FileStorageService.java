package com.leyoswimming.service;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileStorageService {

  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
  private static final Set<String> ALLOWED_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/jpg");

  @Value("${app.file.upload-dir:uploads}")
  private String uploadDir;

  @Value("${app.file.base-url:/uploads/}")
  private String baseUrl;

  public String store(MultipartFile file) {
    validate(file);
    try {
      Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
      Files.createDirectories(dir);
      String extension = getExtension(file.getOriginalFilename());
      String filename = UUID.randomUUID() + "." + extension;
      Path target = dir.resolve(filename);
      file.transferTo(target);
      return baseUrl + filename;
    } catch (IOException e) {
      log.error("Failed to store file", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
    }
    if (!ALLOWED_TYPES.contains(file.getContentType())) {
      throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
    }
  }

  private String getExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return "png";
    }
    return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
  }
}
