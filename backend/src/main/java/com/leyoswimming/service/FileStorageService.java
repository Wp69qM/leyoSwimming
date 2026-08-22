package com.leyoswimming.service;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
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
  private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION =
      Map.of(
          "image/jpeg", "jpg",
          "image/jpg", "jpg",
          "image/png", "png",
          "image/webp", "webp");
  private static final int MAGIC_HEADER_LEN = 12;
  private static final Map<String, byte[][]> MAGIC_NUMBERS =
      Map.of(
          "image/jpeg", new byte[][] {{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},
          "image/png",
              new byte[][] {
                {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
              },
          "image/webp",
              new byte[][] {
                {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50}
              });

  @Value("${app.file.upload-dir:uploads}")
  private String uploadDir;

  @Value("${app.file.base-url:/uploads/}")
  private String baseUrl;

  public String store(MultipartFile file) {
    String contentType = validate(file);
    try {
      Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
      Files.createDirectories(dir);
      String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType);
      String filename = UUID.randomUUID() + "." + extension;
      Path target = dir.resolve(filename);
      file.transferTo(target);
      return baseUrl + filename;
    } catch (IOException e) {
      log.error("Failed to store file", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }
  }

  public String validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
    }
    String contentType = file.getContentType();
    if (!ALLOWED_TYPES.contains(contentType) || !CONTENT_TYPE_TO_EXTENSION.containsKey(contentType)) {
      throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
    }
    String magicType = "image/jpg".equals(contentType) ? "image/jpeg" : contentType;
    if (!hasValidMagicNumber(file, magicType)) {
      log.warn(
          "File magic number mismatch: contentType={}, filename={}",
          contentType,
          file.getOriginalFilename());
      throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
    }
    return contentType;
  }

  private boolean hasValidMagicNumber(MultipartFile file, String contentType) {
    byte[][] signatures = MAGIC_NUMBERS.get(contentType);
    if (signatures == null) {
      return false;
    }
    byte[] header;
    try (InputStream in = file.getInputStream()) {
      header = in.readNBytes(MAGIC_HEADER_LEN);
    } catch (IOException e) {
      log.warn("Failed to read file header", e);
      return false;
    }
    for (byte[] signature : signatures) {
      if (header.length < signature.length) {
        continue;
      }
      if (matchesMagic(header, signature)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesMagic(byte[] header, byte[] signature) {
    for (int i = 0; i < signature.length; i++) {
      if (signature[i] != 0x00 && signature[i] != header[i]) {
        return false;
      }
    }
    return true;
  }

}
